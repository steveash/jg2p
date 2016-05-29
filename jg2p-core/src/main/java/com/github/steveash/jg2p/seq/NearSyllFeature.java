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
 * emits a feature like NEARSYLL_NEXT_graph_countToNext or NEXT_graph_X for those graphs that are last
 * @author Steve Ash
 */
public class NearSyllFeature extends Pipe {

  private static final long serialVersionUID = 6132320827737141523L;

  private final boolean isNext; // false if doing prev, true if next

  public NearSyllFeature(boolean isNext) {
    this.isNext = isNext;
  }

  @Override
  public Instance pipe(Instance inst) {
    TokenSequence ts = (TokenSequence) inst.getData();
    SyllStructure struct = (SyllStructure) ts.getProperty(PhonemeCrfTrainer.PROP_STRUCTURE);
    checkNotNull(struct, "no sylls", inst);
    int xx = 0;
    for (int i = 0; i < ts.size(); i++) {
      Token tok = ts.get(i);
      for (String grapheme : Grams.iterateSymbols(tok.getText())) {
        int relative;
        int syllIndex = struct.getSyllIndexForGraphemeIndex(xx);
        int mySyllSeq = struct.getSyllSequenceForGraphemeIndex(xx);
        // 01201
        // YYZYZ
        if (isNext) {
          int graphsInSyllable = struct.getSyllGraphsForSyllIndex(syllIndex).length(); // no zeroes
          relative = (graphsInSyllable - mySyllSeq);
          if (syllIndex == struct.getLastSyllIndex()) {
            relative = -1;
          }
        } else {
          relative = mySyllSeq + 1;
          if (syllIndex == 0) {
            relative = -1;
          }
        }
        tok.setFeatureValue("NEARSYLL_" + (isNext ? "NEXT_" : "PREV_") + grapheme + "_" + relative, 1.0);
        xx += 1;
      }
    }
    return inst;
  }
}
