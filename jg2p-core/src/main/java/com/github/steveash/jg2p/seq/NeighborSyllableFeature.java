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
import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.syll.SyllStructure;
import com.github.steveash.jg2p.util.TokenSeqUtil;

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import static com.github.steveash.jg2p.seq.TokenWindow.makeTokenWindowsForInts;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Puts features for the neighboring syllable tokens
 *
 * @author Steve Ash
 */
public class NeighborSyllableFeature extends Pipe {

  private final ImmutableList<TokenWindow> windows;

  public NeighborSyllableFeature(int... neighbors) {
    this.windows = makeTokenWindowsForInts(neighbors);
  }

  @Override
  public Instance pipe(Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    SyllStructure struct = (SyllStructure) ts.getProperty(PhonemeCrfTrainer.PROP_STRUCTURE);
    checkNotNull(struct, "no sylls", carrier);

    List<String> sylls = struct.getOncGrams();
    Preconditions.checkState(ts.size() == sylls.size(), "sylls and grams dont equal size");
    for (int i = 0; i < ts.size(); i++) {
      Token t = ts.get(i);
      if (t.getText().length() != sylls.get(i).length()) {
        throw new IllegalStateException("grams doesnt match syll grams " + t + " - " + sylls);
      }
      for (int j = 0; j < windows.size(); j++) {
        TokenWindow window = windows.get(j);
        String windStr = TokenSeqUtil.getWindowFromStrings(sylls, i, window.offset, window.width);
        if (windStr == null) {
          continue;
        }
        String feature = "SYN_" + windStr + "@" + window.offset;
        t.setFeatureValue(feature, 1.0);
      }
    }
    return carrier;
  }
}
