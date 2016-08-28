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

/**
 * Emits features for n-grams of the vowels in the word.  I.e. like skip features
 * @author Steve Ash
 */
public class VowelPatternPipe implements RerankFeature {

  private static final int DIST_BASE = 5;

  @Override
  public void emitFeatures(RerankFeatureBag data) {
    Iterator<String> vowels = Iterables.filter(data.getExample().getEncoding().getPhones(), Phonemes.whereOnlyVowels).iterator();
    if (!vowels.hasNext()) return;
    String last = vowels.next();
    while (vowels.hasNext()) {
      String next = vowels.next();
      data.setBinary("bgmPatt_" + last + "_" + next);
      last = next;
    }
  }

}
