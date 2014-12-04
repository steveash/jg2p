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

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * @author Steve Ash
 */
public class NeighborTokenFeature extends Pipe {

  private final boolean includeCurrent;
  private final int[] neighbors;

  public NeighborTokenFeature(boolean includeCurrent, int... neighbors) {
    this.includeCurrent = includeCurrent;
    this.neighbors = neighbors;
  }

  @Override
  public Instance pipe(Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    for (int i = 0; i < ts.size(); i++) {
      Token t = ts.get(i);
      for (int j = 0; j < neighbors.length; j++) {
        int nidx = i + j;
        if (nidx >= 0 && nidx < ts.size()) {
          Token neighbor = ts.get(nidx);
          String feature = neighbor.getText() + "@" + j;
          if (includeCurrent) {
            feature += "@" + t.getText();
          }
          t.setFeatureValue(feature, 1.0);
        }
      }
    }
    return carrier;
  }
}
