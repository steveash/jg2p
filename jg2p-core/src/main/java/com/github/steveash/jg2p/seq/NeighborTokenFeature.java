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

import com.github.steveash.jg2p.util.TokenSeqUtil;

import java.io.Serializable;
import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * Creates features using a window that varies by starting point (relative to current) and width
 * @author Steve Ash
 */
public class NeighborTokenFeature extends Pipe {

  public static class NeighborWindow implements Serializable {

    public final int offset;
    public final int width;

    public NeighborWindow(int offset, int width) {
      this.offset = offset;
      this.width = width;
    }
  }

  private final boolean includeCurrent;
  private final ImmutableList<NeighborWindow> windows;

  public NeighborTokenFeature(boolean includeCurrent, int... neighbors) {
    this.includeCurrent = includeCurrent;
    ImmutableList.Builder<NeighborWindow> builder = ImmutableList.builder();
    for (int i = 0; i < neighbors.length; i++) {
      builder.add(new NeighborWindow(neighbors[i], 1));
    }
    this.windows = builder.build();
  }

  public NeighborTokenFeature(boolean includeCurrent, List<NeighborWindow> windows) {
    this.includeCurrent = includeCurrent;
    this.windows = ImmutableList.copyOf(windows);
  }

  @Override
  public Instance pipe(Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    for (int i = 0; i < ts.size(); i++) {
      Token t = ts.get(i);
      for (int j = 0; j < windows.size(); j++) {
        NeighborWindow window = windows.get(j);
        String windStr = TokenSeqUtil.getWindow(ts, i, window.offset, window.width);
        if (windStr == null) continue;
          String feature = windStr + "@" + window.offset + "x" + window.width;
          if (includeCurrent) {
            feature += "^" + t.getText();
          }
          t.setFeatureValue(feature, 1.0);
        }
      }
    return carrier;
  }
}
