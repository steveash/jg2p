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

package com.github.steveash.jg2p.rerank;

import com.google.common.collect.Iterables;

import com.github.steveash.jg2p.phoseq.Phonemes;

import java.util.Iterator;
import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;

/**
 * Emits features for n-grams of the vowels in the word.  I.e. like skip features
 * @author Steve Ash
 */
public class VowelPatternPipe extends Pipe {

  private static final int DIST_BASE = 5;

  public VowelPatternPipe(Alphabet dataDict, Alphabet targetDict) {
    super(dataDict, targetDict);
  }

  @Override
  public Instance pipe(Instance inst) {
    RerankFeature data = (RerankFeature) inst.getData();
    addBigrams(data, "A_", data.getExample().getEncodingA().phones);
    addBigrams(data, "B_", data.getExample().getEncodingB().phones);
    return inst;
  }

  private void addBigrams(RerankFeature data, String prefix, List<String> phones) {
    Iterator<String> vowels = Iterables.filter(phones, Phonemes.whereOnlyVowels).iterator();
    if (!vowels.hasNext()) return;
    String last = vowels.next();
    while (vowels.hasNext()) {
      String next = vowels.next();
      data.setBinary(prefix + "_bgmPatt_" + last + "_" + next);
      last = next;
    }
  }
}
