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

package com.github.steveash.jg2p.wfst;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * Keeps a bunch of multi-symbol seqeunces and takes an input sequence to see if the prefix of ths input matches
 * any of the given test sequences; will return all of the matches (i.e. might be more than one if the candiates
 * are not prefix-free distinct
 *
 * (Right now this is just a first level of a trie but if the accept list were large a full trie implementation would
 * be better)
 * @author Steve Ash
 */
public class ListAcceptor<T> {

  private final ImmutableListMultimap<T, Map.Entry<? extends List<T>, T>> byFirstSymbol;

  public ListAcceptor(Map<? extends List<T>, T> accepts) {
    ImmutableListMultimap.Builder<T, Map.Entry<? extends List<T>, T>> builder = ImmutableListMultimap.builder();
    for (Map.Entry<? extends List<T>, T> entry : accepts.entrySet()) {
      builder.put(entry.getKey().get(0), entry);
    }
    this.byFirstSymbol = builder.build();
  }

  public List<Map.Entry<? extends List<T>, T>> accept(List<T> sequenceToMatchAgainst) {
    if (sequenceToMatchAgainst.isEmpty()) {
      return ImmutableList.of();
    }
    ImmutableList<Map.Entry<? extends List<T>, T>> candidates = byFirstSymbol.get(sequenceToMatchAgainst.get(0));
    if (candidates.isEmpty()) {
      return ImmutableList.of();
    }
    List<Map.Entry<? extends List<T>, T>> results = Lists.newArrayList();
    for (Map.Entry<? extends List<T>, T> candidate : candidates) {
      if (matchesPrefix(candidate.getKey(), sequenceToMatchAgainst, 1)) {
        results.add(candidate);
      }
    }
    if (results.isEmpty()) {
      return ImmutableList.of();
    }
    return results;
  }

  private boolean matchesPrefix(List<T> testCandidate, List<T> testTarget, int startIndex) {
    if (testCandidate.size() > testTarget.size()) {
      return false;
    }
    for (int i = startIndex; i < testCandidate.size(); i++) {
      if (!testCandidate.get(i).equals(testTarget.get(i))) {
        return false;
      }
    }
    return true;
  }
}
