/*
 * Copyright 2014 Steve Ash
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.util.TokenSeqUtil;

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import static com.github.steveash.jg2p.seq.TokenWindow.makeTokenWindowsForInts;
import static com.github.steveash.jg2p.util.TokenSeqUtil.tokenToString;

/**
 * Creates features using a window that varies by starting point (relative to current) and width
 * @author Steve Ash
 */
public class NeighborTokenFeature extends Pipe {
  private static final long serialVersionUID = -3645223081008993222L;

  private final boolean includeCurrent;
  private final ImmutableList<TokenWindow> windows;

  public NeighborTokenFeature(boolean includeCurrent, int... neighbors) {
    this.includeCurrent = includeCurrent;
    this.windows = makeTokenWindowsForInts(neighbors);
  }

  public NeighborTokenFeature(boolean includeCurrent, List<TokenWindow> windows) {
    this.includeCurrent = includeCurrent;
    this.windows = ImmutableList.copyOf(windows);
  }

  @Override
  public Instance pipe(Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    List<String> ss = xformInputSequence(ts);
    for (int i = 0; i < ts.size(); i++) {
      Token t = ts.get(i);
      for (int j = 0; j < windows.size(); j++) {
        TokenWindow window = windows.get(j);
        String windStr = getWindow(ss, i, window);
        if (windStr == null) continue;
          String feature = prefix() + windStr + "@" + window.offset;
          if (includeCurrent) {
            feature += "^" + t.getText();
          }
          t.setFeatureValue(feature, 1.0);
        }
      }
    return carrier;
  }

  protected List<String> xformInputSequence(TokenSequence ts) {
    return Lists.transform(ts, tokenToString);
  }

  protected String prefix() {
    return "";
  }

  protected String getWindow(List<String> ts, int i, TokenWindow window) {
    return TokenSeqUtil.getWindowFromStrings(ts, i, window.offset, window.width);
  }
}
