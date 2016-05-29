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

import com.google.common.base.Preconditions;

import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.syll.SyllStructure;

import java.io.Serializable;
import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Converts an alignment in the data segment to a token sequence (incorporating syllable info if present)
 * @author Steve Ash
 */
public class AlignmentToTokenSequence extends Pipe implements Serializable {
  private static final long serialVersionUID = -7681162543291251873L;
  public static final String SYLL_GRAM = "syllGram";

  private final boolean updateTarget;
  private final boolean updateSyllable;
  private final boolean updateSyllTokenCount;

  public AlignmentToTokenSequence(Alphabet dataDict, Alphabet targetDict) {
    this(dataDict, targetDict, true, true, false);
  }

  public AlignmentToTokenSequence(Alphabet dataDict, Alphabet targetDict,
                                  boolean updateTarget, boolean updateSyllable, boolean syllTokenCount) {
    super(dataDict, targetDict);
    this.updateTarget = updateTarget;
    this.updateSyllable = updateSyllable;
    this.updateSyllTokenCount = syllTokenCount;
  }

  @Override
  public Instance pipe(Instance inst) {
    Alignment source = (Alignment) inst.getData();

    List<String> xList = source.getAllXTokensAsList();
    TokenSequence xTokens = makeTokenSeq(xList);
//    xTokens.setProperty(PhonemeCrfTrainer.PROP_ALIGNMENT, source);
    inst.setData(xTokens);
    if (inst.getTarget() != null && updateTarget) {
      List<String> target = (List<String>) inst.getTarget();
      Preconditions.checkState(target.size() == xList.size(), "target %s source %s", target, source);
      inst.setTarget(makeTokenSeq(target));
    }
    if (updateSyllable) {
      List<String> sylls = checkNotNull(source.getGraphoneSyllableGrams(), "no syllables", source);
      SyllStructure struct = new SyllStructure(source);
      xTokens.setProperty(PhonemeCrfTrainer.PROP_STRUCTURE, struct);

      Preconditions.checkState(sylls.size() == xList.size(), "graphemes and syll markers not equal");
      for (int i = 0; i < xTokens.size(); i++) {
        Token token = xTokens.get(i);
        String syll = sylls.get(i);
        String syllFeat = syll;
        if (updateSyllTokenCount) {
          syllFeat += "_" + token.getText().toLowerCase() + "_" + struct.getSyllIndexForGraphoneGramIndex(i);
        }
        token.setFeatureValue("SYL_" + syllFeat, 1.0);
        token.setProperty(SYLL_GRAM, syll);
      }
    }
    return inst;
  }

  private TokenSequence makeTokenSeq(List<String> vals) {
    TokenSequence ts = new TokenSequence(vals.size());
    for (String s : vals) {
      ts.add(s);
    }
    return ts;
  }
}
