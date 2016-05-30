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

import com.github.steveash.jg2p.syll.SyllStructure;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * emits a feature for SYLLREL_1, SYLLREL_2, SYLLREL_X, SYLLREL_-2 (second to last), SYLLREL_-1 (last)
 * @author Steve Ash
 */
public class SyllRelativeMarkFeature extends Pipe {

  private static final long serialVersionUID = -2121131855504897619L;

  @Override
  public Instance pipe(Instance inst) {
    TokenSequence ts = (TokenSequence) inst.getData();
    SyllStructure struct = (SyllStructure) ts.getProperty(PhonemeCrfTrainer.PROP_STRUCTURE);
    checkNotNull(struct, "no sylls", inst);
    if (struct.getSyllCount() >= 1) {
      markAll(ts, struct, 0, "SYLLREL_1");
    }
    if (struct.getSyllCount() >= 2) {
      markAll(ts, struct, struct.getLastSyllIndex(), "SYLLREL_-1");
    }
    if (struct.getSyllCount() >= 3) {
      markAll(ts, struct, 1, "SYLLREL_2");
    }
    if (struct.getSyllCount() >= 4) {
      markAll(ts, struct, struct.getLastSyllIndex() - 1, "SYLLREL_-2");
    }
    if (struct.getSyllCount() >= 5) {
      for (int i = 2; i < (struct.getLastSyllIndex() - 1); i++) {
        markAll(ts, struct, i, "SYLLRELL_X");
      }
    }

    return inst;
  }

  private void markAll(TokenSequence ts, SyllStructure struct, int syllIndex, String label) {
    for (int i = 0; i < ts.size(); i++) {
      if (struct.getSyllIndexForGraphoneGramIndex(i) == syllIndex) {
        ts.get(i).setFeatureValue(label, 1.0);
      }
    }
  }
}
