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

import com.google.common.collect.Lists;

import com.github.steveash.jg2p.syll.SyllStructure;
import com.github.steveash.jg2p.util.TokenSeqUtil;

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * finds the next nucleus vowel and for the next nearest vowel -- emits a window of (up to two chars
 * of the onset and the nucleus vowels)
 *
 * @author Steve Ash
 */
public class VowelWindowFeature extends Pipe {
  private static final long serialVersionUID = 2615297725037896989L;

  public static VowelWindowFeature makeOnlyLastVowel() {
    return new VowelWindowFeature(2, 2, "VWF_LAST_", 0, true);
  }
  public static VowelWindowFeature makeRelative(int relativePos) {
    return new VowelWindowFeature(2, 2, "VWF_REL" + relativePos + "_", relativePos, false);
  }

  private final int maxOnset;
  private final int maxCoda;
  private final String prefix;
  private final int relativeIndex;
  private final boolean onlyLast;

  public VowelWindowFeature(int maxOnset, int maxCoda, String prefix, int relativeIndex, boolean onlyLast) {
    this.maxOnset = maxOnset;
    this.maxCoda = maxCoda;
    this.prefix = prefix;
    this.relativeIndex = relativeIndex;
    this.onlyLast = onlyLast;
  }

  @Override
  public Instance pipe(Instance inst) {

    TokenSequence ts = (TokenSequence) inst.getData();
    List<String> graphoneText = Lists.transform(ts, TokenSeqUtil.tokenToString);
    List<String> graphoneCodes = Lists.transform(ts, NeighborSyllableFeature.TokenToSyllGram);
    SyllStructure struct = new SyllStructure(graphoneText, graphoneCodes);
    for (int i = 0; i < ts.size(); i++) {
      Token tt = ts.get(i);
      if (!struct.graphoneGramIndexContainsNucleus(i)) {
        continue;
      }
      int syllIndex = struct.getSyllIndexForGraphoneGramIndex(i);
      String feat = null;
      if (onlyLast) {
        if (syllIndex < struct.getLastSyllIndex()) {
          feat = prefix + struct.getSyllPart(struct.getLastSyllIndex(), maxOnset, -1, maxCoda);
        }
      } else if (relativeIndex != 0) {
        int targetSyll = syllIndex + relativeIndex;
        if (targetSyll >= 0 && targetSyll <= struct.getLastSyllIndex()) {
          feat = prefix + struct.getSyllPart(targetSyll, maxOnset, -1, maxCoda);
        }
      }
      if (feat != null) {
        tt.setFeatureValue(feat, 1.0);
      }
    }
    return inst;
  }
}
