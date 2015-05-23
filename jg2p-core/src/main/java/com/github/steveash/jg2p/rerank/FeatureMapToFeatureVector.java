/*
 * Copyright 2015 Steve Ash
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

package com.github.steveash.jg2p.rerank;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Map;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;

/**
 * Transforms the rerank map of fetures into a feature vector
 *
 * @author Steve Ash
 */
public class FeatureMapToFeatureVector extends Pipe {

  private final List<String> featureNames;

  public FeatureMapToFeatureVector(Alphabet dataDict, Alphabet targetDict, List<String> featureNames) {
    super(dataDict, null);
    this.featureNames = featureNames;
  }

  @Override
  public Instance pipe(Instance inst) {
    Map<String, Object> map = (Map<String, Object>) inst.getData();
    int nonZeroCount = 0;
    for (String feature : featureNames) {
      String val = (String) map.get(feature);
      if (val == null) continue;

      double dblVal = Double.parseDouble(val);
      if (dblVal != 0) {
        nonZeroCount += 1;
      }
    }

    Object[] keys = new Object[nonZeroCount];
    double[] vals = new double[nonZeroCount];
    int i = 0;
    for (String feature : featureNames) {
      String val = (String) map.get(feature);
      if (val == null) continue;

      double dblVal = Double.parseDouble(val);
      if (dblVal != 0) {
        keys[i] = feature;
        vals[i] = dblVal;
        i += 1;
      }
    }
    Preconditions.checkState(keys.length == vals.length);
    Preconditions.checkState(i == keys.length, "didnt find same count?");
    Preconditions.checkState(keys.length > 0, "didnt have any keys in the feature vector map");

    inst.setData(new FeatureVector(this.getDataAlphabet(), keys, vals));
    return inst;
  }
}
