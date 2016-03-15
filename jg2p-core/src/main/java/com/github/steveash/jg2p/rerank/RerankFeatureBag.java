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

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;

/**
 * Holder for feature values in the reranking pipe
 * @author Steve Ash
 */
public class RerankFeatureBag {

  private static final Double ONE = 1.0;

  private final Alphabet dataAlphabet;
  private final RerankExample example;
  private final SortedMap<Integer,Double> features = Maps.newTreeMap();

  public RerankFeatureBag(Alphabet dataAlphabet, RerankExample example) {
    this.dataAlphabet = dataAlphabet;
    this.example = example;
  }

  public RerankExample getExample() {
    return example;
  }

  public void setBinary(String key) {
    int idx = lookup(key);
    features.put(idx, ONE);
  }

  public void setFeature(String key, double value) {
    int idx = lookup(key);
    if (value != 0) {
      features.put(idx, value);
    }
  }

  private int lookup(String key) {
    int idx = dataAlphabet.lookupIndex(key, true);
    if (idx < 0) {
      throw new IllegalArgumentException("Couldnt add new index for " + key);
    }
    return idx;
  }

  public FeatureVector toVector() {
    int[] keys = new int[features.size()];
    double[] vals = new double[features.size()];

    int i = 0;
    for (Map.Entry<Integer, Double> entry : features.entrySet()) {
      keys[i] = entry.getKey();
      vals[i] = entry.getValue();
      i += 1;
    }
    return new FeatureVector(dataAlphabet, keys, vals);
  }
}
