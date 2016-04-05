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

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.util.TokenSeqUtil;

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import static com.github.steveash.jg2p.util.TokenSeqUtil.getWindowFromStrings;
import static com.github.steveash.jg2p.util.TokenSeqUtil.tokenToString;

/**
 * Creates features for a sliding window around the current slot. Treats the current gram as 1 slot in the window
 * regardless of how many graphs are in the gram
 *
 * @author Steve Ash
 */
public class WindowFeature extends Pipe {

  private static final long serialVersionUID = -3645223081008993222L;

  private final boolean emitShape;
  private final int maxWidth;

  public WindowFeature(boolean emitShape, int maxWidth) {
    this.emitShape = emitShape;
    this.maxWidth = maxWidth;
  }

  @Override
  public Instance pipe(Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    List<String> ss = Lists.transform(ts, tokenToString);
    int gramsBefore = 0;
    int gramsAfter = TokenSeqUtil.countAfter(ss, 0);

    for (int i = 0; i < ts.size(); i++) {
      Token t = ts.get(i);
      String gram = ss.get(i);
      for (int j = 0; j < maxWidth; j++) {
        int before = maxWidth - j;
        int after = j;
        before = Math.min(before, gramsBefore);
        after = Math.min(after, gramsAfter);
        StringBuilder sb = new StringBuilder();
        sb.append(prefix());
        if (before > 0) {
          sb.append(xform(getWindowFromStrings(ss, i, -before, before)));
        }
        sb.append("$").append(gram).append("$");
        if (after > 0) {
          sb.append(xform(getWindowFromStrings(ss, i, 1, after)));
        }
        if (before > 0 || after > 0) {
          t.setFeatureValue(sb.toString(), 1.0);
        }
      }
      int thisGramCount = Grams.countInGram(gram);
      gramsBefore += thisGramCount;
      gramsAfter -= thisGramCount;
    }
    return carrier;
  }

  private String xform(String window) {
    if (emitShape) {
      return TokenSeqUtil.convertShape(window);
    }
    return window;
  }

  private String prefix() {
    if (emitShape) {
      return "WY_";
    }
    return "W_";
  }
}
