/*
 * Copyright 2015 Steve Ash
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.steveash.jg2p.align;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import com.github.steveash.jg2p.Word;

import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Filters the allowed transitions to only those that match a particular filter
 *
 * @author Steve Ash
 */
public class FilterWalkerDecorator implements XyWalker {

  public static Set<Pair<String, String>> readFromFile(File file) throws IOException {
    Splitter splitter = Splitter.on('^').trimResults();
    List<String> lines = Files.readLines(file, Charsets.UTF_8);
    HashSet<Pair<String, String>> result = Sets.newHashSet();
    for (String line : lines) {
      if (isBlank(line)) {
        continue;
      }
      Iterator<String> fields = splitter.split(line).iterator();
      String x = fields.next();
      String y = fields.next();
      result.add(Pair.of(x, y));
    }
    return result;
  }

  private final XyWalker delegate;
  private final Set<Pair<String, String>> allowed;
  private final Set<Pair<String, String>> blocked = Sets.newConcurrentHashSet();

  public FilterWalkerDecorator(XyWalker delegate, Set<Pair<String, String>> allowed) {
    this.delegate = delegate;
    this.allowed = allowed;
  }

  public Set<Pair<String,String>> getBlocked() {
    return blocked;
  }

  @Override
  public void forward(Word x, Word y, final Visitor visitor) {
    delegate.forward(x, y, new Visitor() {
      @Override
      public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
        if (isAllowed(xGram, yGram)) {
          visitor.visit(xxBefore, xxAfter, xGram, yyBefore, yyAfter, yGram);
        } else {
          blocked.add(Pair.of(xGram, yGram));
        }
      }
    });
  }

  private boolean isAllowed(String xGram, String yGram) {
    return allowed.contains(Pair.of(xGram, yGram));
  }

  @Override
  public void backward(Word x, Word y, final Visitor visitor) {
    delegate.backward(x, y, new Visitor() {
      @Override
      public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
        if (isAllowed(xGram, yGram)) {
          visitor.visit(xxBefore, xxAfter, xGram, yyBefore, yyAfter, yGram);
        } else {
          blocked.add(Pair.of(xGram, yGram));
        }
      }
    });
  }
}
