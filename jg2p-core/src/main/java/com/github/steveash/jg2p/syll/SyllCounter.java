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

package com.github.steveash.jg2p.syll;

import com.google.common.base.Preconditions;

import java.util.List;

import cc.mallet.types.Sequence;

/**
 * @author Steve Ash
 */
public class SyllCounter {

  public static int countSyllablesInGrams(List<String> grams) {
    SyllCounter counter = new SyllCounter();
    for (String gram : grams) {
      counter.onNextGram(gram);
    }
    return counter.currentSyllable() + 1; // 0 based index
  }

  public static int countSyllablesInSequence(Sequence seq) {
    SyllCounter counter = new SyllCounter();
    for (int i = 0; i < seq.size(); i++) {
      String tag = (String) seq.get(i);
      Preconditions.checkState(tag.length() == 1, tag);
      counter.onNextCode(tag.charAt(0));
    }
    return counter.currentSyllable() + 1; // 0 based index
  }

  private final boolean splitNucleus;
  private int state = 0; // 0 = onset, 1 = nucleus, 2 = coda
  private int syllable = 0;

  // by default to consecutive nucleus phonemes cause a syllable break
  public SyllCounter() {
    splitNucleus = false;
  }

  public SyllCounter(boolean splitNucleus) {
    this.splitNucleus = splitNucleus;
  }

  public int currentSyllable() {
    return syllable;
  }

  public void onNextGram(String syllGram) {
    for (int i = 0; i < syllGram.length(); i++) {
      char syllChar = syllGram.charAt(i);
      if (Character.isWhitespace(syllChar)) {
        continue; // skip the whitespace
      }
      onNextCode(syllChar);
    }
  }

  public void onNextCode(char syllChar) {
    char c = syllChar;
    if (c == SyllTagTrainer.OnsetChar) {
      if (state != 0) {
        syllable += 1;
        state = 0;
      }
    } else if (c == SyllTagTrainer.NucleusChar) {
      if (state == 0) {
        state = 1;
      } else if (state == 1) {
        // if we're splitting nucleuses then increment syllable
        if (splitNucleus) {
          syllable += 1;
        }
      } else {
        syllable += 1;
        state = 1;
      }
    } else if (c == SyllTagTrainer.CodaChar) {
//      Preconditions.checkState(state != 0); should never happen but just in case
      state = 2;
    } else {
      throw new IllegalArgumentException("invalid syllable code " + syllChar);
    }
  }
}
