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

import com.github.steveash.jg2p.Word;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple rules for known patterns (that we don't want to train with because they hurt training/alignment
 *
 * @author Steve Ash
 */
public class KnownPattern {

  public static boolean matches(Word word) {
    return maybeMake(word) != null;
  }

  public static String transcribePattern(Word word) {
    return checkNotNull(maybeMake(word), "cant trancribe pattern for ", word);
  }

  @Nullable
  private static String maybeMake(Word word) {
    if (word.unigramCount() == 3 && word.gramAt(1).equalsIgnoreCase("'")) {
      if (word.gramAt(2).equalsIgnoreCase("S")) {
        return new AbbrevBuilder(false)
            .append(Abbrev.phonesForLetter(word.gramAt(0)))
            .append("Z")
            .build();
      }
      if (word.gramAt(2).equalsIgnoreCase("D")) {
        return new AbbrevBuilder(false)
            .append(Abbrev.phonesForLetter(word.gramAt(0)))
            .append("D")
            .build();
      }
    }
    if (word.unigramCount() == 2 && word.gramAt(0).equalsIgnoreCase("'")) {
      if (word.gramAt(1).equalsIgnoreCase("M")) return "AH M";
      if (word.gramAt(1).equalsIgnoreCase("N")) return "AH N";
      if (word.gramAt(1).equalsIgnoreCase("S")) return "EH S";
    }
    return null;
  }
}
