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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.phoseq.Phonemes;

import java.util.List;
import java.util.Map;

/**
 * Generates new candidate encodings for particular entries just by replacing particular vowels with frequently confused
 * vowels
 *
 * @author Steve Ash
 */
public class VowelReplacer {

  private static final int REPLACE_TOP_K = 4;
  private static final ImmutableMultimap<String, String> replaces = ImmutableListMultimap.<String, String>builder()
      .put("AH", "AA")
      .put("AH", "AE")
      .put("AH", "IH")
      .put("AA", "AH")
      .put("AE", "AA")
      .put("AH", "EH")
      .put("IH", "AH")
      .put("AE", "AH")
      .put("AA", "AE")
      .put("EH", "AH")
      .put("OW", "AA")
      .put("AA", "OW")
      .put("IH", "IY")
      .put("AH", "IY")
      .put("AH", "OW")
      .put("IH", "AY")
      .put("OW", "AH")
      .build();

  public List<PhoneticEncoder.Encoding> updateResults(List<PhoneticEncoder.Encoding> original) {
    // updates the top k (defaults k = 4) entries by adding similar vowel versions
    Map<List<String>, PhoneticEncoder.Encoding> results = null;
    for (int i = 0; i < original.size() && i < REPLACE_TOP_K; i++) {
      List<PhoneticEncoder.Encoding> emitted = emit(original.get(i));
      if (emitted.isEmpty()) {
        continue;
      }
      if (results == null) {
        results = Maps.newHashMap();
        for (PhoneticEncoder.Encoding encoding : original) {
          results.put(encoding.phones, encoding);
        }
      }
      // add these if they aren't alreaday present
      for (PhoneticEncoder.Encoding emittedEncoding : emitted) {
        if (!results.containsKey(emittedEncoding.phones)) {
          results.put(emittedEncoding.phones, emittedEncoding);
        }
      }
    }
    if (results == null) {
      return original;
    }
    return PhoneticEncoder.OrderByTagScore.sortedCopy(results.values());
  }

  /**
   * This returns any replacements that can be made for the given input
   */
  public List<PhoneticEncoder.Encoding> emit(PhoneticEncoder.Encoding input) {
    // find the first vowel or second vowel -- that is in the replacement list and return that
    int vowelCount = 0;
    List<PhoneticEncoder.Encoding> results = null;
    for (int i = 0; i < input.phones.size(); i++) {
      String p = input.phones.get(i);
      if (Phonemes.isVowel(p)) {
        vowelCount += 1;
        if (vowelCount > 2) {
          break;
        }
        for (String newPhone : replaces.get(p)) {
          if (results == null) {
            results = Lists.newArrayList();
          }
          results.add(input.withReplacedPhoneme(i, newPhone));
        }
      }
    }
    if (results == null) return ImmutableList.of();
    return results;
  }
}
