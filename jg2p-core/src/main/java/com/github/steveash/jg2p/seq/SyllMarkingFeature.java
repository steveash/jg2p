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

import com.github.steveash.jg2p.syll.SyllCounter;

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * emits a feature for SYLLCNT_X where X is the current syllable this grapheme is in
 * @author Steve Ash
 */
public class SyllMarkingFeature extends Pipe {

  private static final long serialVersionUID = -2121131855504897619L;

  @Override
  public Instance pipe(Instance inst) {
    TokenSequence ts = (TokenSequence) inst.getData();
    List<String> sg = Lists.transform(ts, NeighborSyllableFeature.TokenToSyllGram);
    SyllCounter counter = new SyllCounter();
    int prevSyll = 0;
    for (int i = 0; i < ts.size(); i++) {
      Token tok = ts.get(i);
      String s = sg.get(i);
      counter.onNextGram(s);
      if (prevSyll != counter.currentSyllable()) {
        tok.setFeatureValue("SYLLMARK_" + counter.currentSyllable(), 1.0);
        prevSyll = counter.currentSyllable();
      }
    }
    return inst;
  }
}
