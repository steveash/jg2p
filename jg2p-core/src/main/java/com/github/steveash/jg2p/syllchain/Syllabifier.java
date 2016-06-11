/*
 * Copyright 2016 Steve Ash
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

package com.github.steveash.jg2p.syllchain;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Friendly interface for just calculating syllable related things
 * @author Steve Ash
 */
public abstract class Syllabifier {

  int syllableCount(String word) {
    return tagSyllStarts(word).size();
  }

  List<String> splitIntoSyllables(String word) {
    ArrayList<String> sylls = Lists.newArrayList();
    Set<Integer> breaks = tagSyllStarts(word);
    List<Integer> sortedBreaks = Ordering.natural().sortedCopy(breaks);
    if (Iterables.getFirst(sortedBreaks, -1) == 0) {
      sortedBreaks = sortedBreaks.subList(1, sortedBreaks.size());
    }
    int last = 0;
    for (Integer sortedBreak : sortedBreaks) {
      sylls.add(word.substring(last, sortedBreak));
      last = sortedBreak;
    }
    sylls.add(word.substring(last, word.length()));
    return sylls;
  }

  protected abstract Set<Integer> tagSyllStarts(String word);
}
