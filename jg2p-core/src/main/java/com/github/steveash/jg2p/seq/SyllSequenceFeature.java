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

package com.github.steveash.jg2p.seq;

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.syll.SyllStructure;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * emits a feature for SYLLSEQ_X where X is the current syllable sequence for this grapheme in the current syll
 * @author Steve Ash
 */
public class SyllSequenceFeature extends Pipe {

  private static final long serialVersionUID = 4853774894628951368L;

  @Override
  public Instance pipe(Instance inst) {
    TokenSequence ts = (TokenSequence) inst.getData();
    SyllStructure struct = (SyllStructure) ts.getProperty(PhonemeCrfTrainer.PROP_STRUCTURE);
    checkNotNull(struct, "no sylls", inst);
    int xx = 0;
    for (int i = 0; i < ts.size(); i++) {
      Token tok = ts.get(i);
      for (String graph : Grams.iterateSymbols(tok.getText())) {
        tok.setFeatureValue("SYLLSEQ_" + struct.getSyllSequenceForGraphemeIndex(xx), 1.0);
        xx += 1;
      }
    }
    return inst;
  }
}
