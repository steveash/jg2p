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

import com.github.steveash.jg2p.phoseq.Phonemes;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * adds feature for how many spots after me is the next vowel adds feature for how many spots before me is the prev
 * vowel
 *
 * @author Steve Ash
 */
public class VowelNeighborPipe extends Pipe {

  @Override
  public Instance pipe(Instance inst) {
    TokenSequence seq = (TokenSequence) inst.getData();
    int last = -1;
    for (int i = 0; i < seq.size(); i++) {
      Token token = seq.get(i);
      String phone = token.getText();
      token.setFeatureValue("VPREV" + (last >= 0 ? Integer.toString(i - last) : "X"), 1.0);
      if (Phonemes.isVowel(phone)) {
        last = i;
      }
    }
    int next = -1;
    for (int i = seq.size() - 1; i >= 0; i--) {
      Token token = seq.get(i);
      String phone = token.getText();
      token.setFeatureValue("VNEXT" + (next >= 0 ? Integer.toString(next - i) : "X"), 1.0);
      if (Phonemes.isVowel(phone)) {
        next = i;
      }
    }
    return inst;
  }
}
