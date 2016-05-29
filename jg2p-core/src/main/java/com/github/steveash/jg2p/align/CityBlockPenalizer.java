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

package com.github.steveash.jg2p.align;

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.abb.Abbrev;
import com.github.steveash.jg2p.phoseq.Graphemes;
import com.github.steveash.jg2p.phoseq.Phonemes;

/**
 * @author Steve Ash
 */
public class CityBlockPenalizer implements Penalizer {

  public static final CityBlockPenalizer Instance = new CityBlockPenalizer();

  private static final int EPS_PENALTY = 2;
  private static final double PENALTY_SCALE = 1.0;

  @Override
  public double penalize(String xGram, String yGram, double prob) {
    int xCount = Grams.countInGram(xGram);
    int yCount = Grams.countInGram(yGram);
    if (xCount < 1) {
      xCount = EPS_PENALTY;
    }
    if (yCount < 1) {
      yCount = EPS_PENALTY;
    }
    double penalty = (xCount + yCount) * PENALTY_SCALE;
    if (isWhitelisted(xGram, yGram, xCount, yCount)) {
      penalty = 1.0;
//    }else if (xCount == 1 && yCount == 1 && weirdSinglePairing(xGram, yGram)) {
//      penalty *= 1.4;
    } else if (!isSpokenLetter(xGram, yGram) && weirdPhoneCombo(yGram)) {
      penalty *= 1.4;
    }
    return Math.pow(prob, penalty);
  }

  private boolean isWhitelisted(String xGram, String yGram, int xCount, int yCount) {
    if (xCount == 2 && yCount == 2) {
      if (xGram.equalsIgnoreCase("W H") && yGram.equalsIgnoreCase("HH W")) {
        return true;
      }
      if (xGram.equalsIgnoreCase("H U") && yGram.equalsIgnoreCase("Y UW")) {
        return true;
      }
    }
    return false;
  }

  private boolean weirdSinglePairing(String xGram, String yGram) {
    return false;
  }

  private boolean isSpokenLetter(String xGram, String yGram) {
    if (Grams.countInGram(xGram) == 1) {
      if (Graphemes.isVowelOrConsonant(xGram)) {
        String spokenPhones = Abbrev.phonesForLetter(xGram);
        if (spokenPhones.equalsIgnoreCase(yGram)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean weirdPhoneCombo(String yGram) {
    boolean sawSoft = false;
    boolean sawHard = false;
    for (String phone : Grams.iterateSymbols(yGram)) {
      // the HH is weird it says fricative but that just cannot be right
      // lets just skip it entirely for now
      if (phone.equalsIgnoreCase("HH")) continue;
      Phonemes.PhoneClass pc = Phonemes.getClassSymbolForPhone(phone);
      switch (pc) {
        case S:
        case A:
        case F:
        case N:
        case L:
          sawHard = true;
          break;
        default:
          sawSoft = true;
          break;
      }
    }
    return sawSoft && sawHard;
  }
}
