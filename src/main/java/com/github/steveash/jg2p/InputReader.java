/*
 * Copyright 2014 Steve Ash
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

package com.github.steveash.jg2p;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Steve Ash
 */
public class InputReader {

  private static final Splitter tabSplit = Splitter.on('\t');

  public static class InputRecord extends Pair<Word,Word> {
    public final Word xWord;
    public final Word yWord;

    public InputRecord(Word xWord, Word yWord) {
      this.xWord = xWord;
      this.yWord = yWord;
    }

    @Override
    public Word getLeft() {
      return xWord;
    }

    @Override
    public Word getRight() {
      return yWord;
    }

    @Override
    public Word setValue(Word value) {
      throw new IllegalStateException("Word pairs are immutable");
    }
  }

  public List<InputRecord> readFromClasspath(String resource) throws IOException {
    return read(Resources.asCharSource(Resources.getResource(resource), Charsets.UTF_8));
  }

  public List<InputRecord> read(CharSource source) throws IOException {
    return source.readLines(new LineProcessor<List<InputRecord>>() {
      private final List<InputRecord> recs = Lists.newArrayList();

      @Override
      public boolean processLine(String line) throws IOException {
        if (isBlank(line)) return true;
        Iterator<String> iter = tabSplit.split(line).iterator();
        Word x = Word.fromSpaceSeparated(iter.next());
        Word y = Word.fromSpaceSeparated(iter.next());
        recs.add(new InputRecord(x, y));
        return true;
      }

      @Override
      public List<InputRecord> getResult() {
        return recs;
      }
    });
  }
}
