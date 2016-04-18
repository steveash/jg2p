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

package com.github.steveash.jg2p.abb;

import com.google.common.collect.ImmutableMap;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.phoseq.Graphemes;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple rules file for whether or not this is an abbreviation and how to transcribe it
 *
 * @author Steve Ash
 */
public class Abbrev {

  public static String transcribeAcronym(Word input) {
    AbbrevBuilder sb = new AbbrevBuilder(false);
    for (int i = 0; i < input.unigramCount(); i++) {
      sb.append(phonesForLetter(input.gramAt(i)));
    }
    return sb.build();
  }

  public static String phonesForLetter(String gram) {
    return checkNotNull(gramToPhone.get(gram.toUpperCase()), "no phones found for abbrev letter ", gram);
  }

  public static boolean isAcronym(Word input) {
    int size = input.unigramCount();
    if (size == 1) {
      return true;
    }
    if (size <= 4) {
      if (Graphemes.isAllConsonants(input)) {
        return true;
      }
      if (size == 3 && Graphemes.isAllVowels(input)) {
        return true;
      }
    }
    return false;
  }

  private static final ImmutableMap<String, String> gramToPhone = ImmutableMap.<String, String>builder()
      .put("A", "EY")
      .put("B", "B IY")
      .put("C", "S IY")
      .put("D", "D IY")
      .put("E", "IY")
      .put("F", "EH F")
      .put("G", "JH IY")
      .put("H", "EY CH")
      .put("I", "AY")
      .put("J", "JH EY")
      .put("K", "K EY")
      .put("L", "EH L")
      .put("M", "EH M")
      .put("N", "EH N")
      .put("O", "OW")
      .put("P", "P IY")
      .put("Q", "K Y UW")
      .put("R", "AA R")
      .put("S", "EH S")
      .put("T", "T IY")
      .put("U", "Y UW")
      .put("V", "V IY")
      .put("W", "D AH B AH Y UW")
      .put("X", "EH K S")
      .put("Y", "W AY")
      .put("Z", "Z IY")
      .build();
}
