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

import sun.org.mozilla.javascript.internal.ast.ForInLoop;

import java.util.regex.Pattern;

/**
 * Originally got this from: https://github.com/ipeirotis/ReadabilityMetrics/blob/master/src/main/java/com/ipeirotis/readability/engine/Syllabify.java
 *
 * But it has a bug because java regexes dont work like perl regexes.  I fixed it and uplifted the rules to
 * the current Perl' version 0.30
 *
 * @author Steve Ash
 */
public class RuleSyllabifier {

  private static final Pattern VOWELS = Pattern.compile("[^aeiouy]+");

  private static final String[] staticSubMatches = {"cial", "tia", "cius", "cious", "giu", "ion", "iou"};
  private static final Pattern[] regexSubMatches = {
      Pattern.compile(".*sia$"),
      Pattern.compile(".*.ely$"),
      Pattern.compile(".*[^td]ed$")
  };

  private static final String[] staticAddMatches = {"ia", "riet", "dien", "iu", "io", "ii", "microor"};
  private static final Pattern[] regexAddMatches = {
      Pattern.compile(".*[aeiouym]bl$"),
      Pattern.compile(".*[aeiou]{3}.*"),
      Pattern.compile("^mc.*"),
      Pattern.compile(".*ism$"),
      Pattern.compile(".*isms$"),
      Pattern.compile(".*([^aeiouy])\\1l$"),
      Pattern.compile(".*[^l]lien.*"),
      Pattern.compile("^coa[dglx]..*"),
      Pattern.compile(".*[^gq]ua[^auieo].*"),
      Pattern.compile(".*dnt$")
  };

  public static int syllable(String word) {

    word = word.toLowerCase();
    if (word.equals("w")) {
      return 2;
    }
    if (word.length() == 1) {
      return 1;
    }
    word = word.replaceAll("'", " ");

    if (word.endsWith("e")) {
      word = word.substring(0, word.length() - 1);
    }

    String[] phonems = VOWELS.split(word);

    int syl = 0;
    for (int i = 0; i < staticSubMatches.length; i++) {
      if (word.contains(staticSubMatches[i])) {
        syl -= 1;
      }
    }
    for (int i = 0; i < regexSubMatches.length; i++) {
      if (regexSubMatches[i].matcher(word).matches()) {
        syl -= 1;
      }
    }
    for (int i = 0; i < staticAddMatches.length; i++) {
      if (word.contains(staticAddMatches[i])) {
        syl += 1;
      }
    }
    for (int i = 0; i < regexAddMatches.length; i++) {
      if (regexAddMatches[i].matcher(word).matches()) {
        syl += 1;
      }
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

