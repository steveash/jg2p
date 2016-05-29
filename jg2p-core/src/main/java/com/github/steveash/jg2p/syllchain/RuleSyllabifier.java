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

import java.util.regex.Pattern;

/**
 * @author Steve Ash
 */
public class RuleSyllabifier {

  private static final Pattern VOWELS = Pattern.compile("[^aeiouy]+");

  private static String[] SubSyl = {"cial", "tia", "cius", "cious", "giu", "ion", "iou", "sia$", ".ely$"};

  private static String[] AddSyl = {"ia", "riet", "dien", "iu", "io", "ii", "[aeiouym]bl$", "[aeiou]{3}", "^mc", "ism$",
                                    "[^aeiouy][^aeiouy]l$", "[^l]lien", "^coa[dglx].", "[^gq]ua[^auieo]", "dnt$"};

  public static int syllable(String word) {

    word = word.toLowerCase();
    word = word.replaceAll("'", " ");

    if (word.equals("i")) {
      return 1;
    }
    if (word.equals("a")) {
      return 1;
    }

    if (word.endsWith("e")) {
      word = word.substring(0, word.length() - 1);
    }

    String[] phonems = VOWELS.split(word);

    int syl = 0;
    for (int i = 0; i < SubSyl.length; i++) {
      String syllabe = SubSyl[i];
      if (word.matches(syllabe)) {
        syl--;
      }
    }
    for (int i = 0; i < AddSyl.length; i++) {
      String syllabe = AddSyl[i];
      if (word.matches(syllabe)) {
        syl++;
      }
    }
    if (word.length() == 1) {
      syl++;
    }

    for (int i = 0; i < phonems.length; i++) {
      if (phonems[i].length() > 0) {
        syl++;
      }
    }

    if (syl == 0) {
      syl = 1;
    }

    return syl;
  }

}

