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

package com.github.steveash.jg2p.util;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.seq.PhonemeCrfModel;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

/**
 * @author Steve Ash
 */
public class GramWalker {

  @Nullable // if there aren't enough
  public static String window(List<String> grams, int startingGram, int startingSymbolInGram, int offset, int length) {
    if (offset < 0) {
      return getBackwardWindow(grams, startingGram, startingSymbolInGram, offset, length);
    }
    return getForwardWindow(grams, startingGram, startingSymbolInGram, offset, length);
  }

  private static String getBackwardWindow(List<String> grams, int startingGram, int startingSymbolInGram, int offset,
                                          int length) {
    Preconditions.checkArgument(startingSymbolInGram + offset <= 0, "cant has offset cross the starting");
    Iterator<String> symbols = FluentIterable
        .from(Lists.reverse(grams.subList(0, startingGram + 1)))
        .transformAndConcat(gramToReversedSymbols)
        .filter(PhonemeCrfModel.isNotEps)
        .filter(Funcs.onlyNonBlank())
        .iterator();
    int startGramSize = Iterables.size(GramBuilder.SPLITTER.split(grams.get(startingGram)));
    int startingGramSkip = startGramSize - startingSymbolInGram - 1;
    int windowDelta = 1 + (-1 * (offset + length));
    if (advance(windowDelta + startingGramSkip, symbols)) {
      return null;
    }

    List<String> collected = Lists.newArrayListWithCapacity(length);
    for (int i = 0; i < length; i++) {
      if (symbols.hasNext()) {
        collected.add(symbols.next());
      } else {
        return null;
      }
    }
    Collections.reverse(collected);
    GramBuilder sb = new GramBuilder();
    for (String s : collected) {
      sb.append(s);
    }
    return sb.make();
  }

  private static String getForwardWindow(List<String> grams, int startingGram, int startingSymbolInGram, int offset,
                                         int length) {
    Iterator<String> symbols = FluentIterable
        .from(grams.subList(startingGram, grams.size()))
        .transformAndConcat(gramToSymbols)
        .filter(PhonemeCrfModel.isNotEps)
        .filter(Funcs.onlyNonBlank())
        .iterator();
    // this starts with the first symbol in the starting gram, but we might not start on that
    if (advance((startingSymbolInGram + offset), symbols)) {
      return null;
    }
    // now build up the final window
    GramBuilder sb = new GramBuilder();
    for (int i = 0; i < length; i++) {
      if (symbols.hasNext()) {
        sb.append(symbols.next());
      } else {
        return null;
      }
    }
    return sb.make();
  }

  // return true if terminate early
  private static boolean advance(int count, Iterator<String> iter) {
    for (int i = 0; i < count; i++) {
      if (iter.hasNext()) {
        iter.next();
      } else {
        return true;
      }
    }
    return false;
  }

  private static final Function<String, Iterable<String>> gramToSymbols = new Function<String, Iterable<String>>() {
    @Override
    public Iterable<String> apply(String input) {
      return GramBuilder.SPLITTER.split(input);
    }
  };

  private static final Function<String, Iterable<String>>
      gramToReversedSymbols =
      new Function<String, Iterable<String>>() {
        @Override
        public Iterable<String> apply(String input) {
          return Lists.reverse(GramBuilder.SPLITTER.splitToList(input));
        }
      };
}
