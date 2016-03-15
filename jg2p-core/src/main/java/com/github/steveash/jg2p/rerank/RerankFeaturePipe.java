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

package com.github.steveash.jg2p.rerank;

import com.google.common.collect.ImmutableList;

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;

/**
 * This pipe takes the data as a List<rerankExampe> and runs through a list of RerankFeature, finally setting the
 * data element to the resulting featureVectorSequence
 * @author Steve Ash
 */
public class RerankFeaturePipe extends Pipe {

  private final ImmutableList<? extends RerankFeature> features;
  private final Alphabet dataDict;

  public RerankFeaturePipe(Alphabet dataDict, Alphabet targetDict, List<? extends RerankFeature> features) {
    super(dataDict, targetDict);
    this.dataDict = dataDict;
    this.features = ImmutableList.copyOf(features);
  }

  @Override
  public Instance pipe(Instance inst) {
    List<RerankExample> entries = (List<RerankExample>) inst.getData();
    FeatureVector[] output = new FeatureVector[entries.size()];
    for (int i = 0; i < entries.size(); i++) {
      RerankFeatureBag bag = new RerankFeatureBag(dataDict, entries.get(i));
      for (RerankFeature feature : features) {
        feature.emitFeatures(bag);
      }
      output[i] = bag.toVector();
    }
    inst.setData(new FeatureVectorSequence(output));
    return inst;
  }
}
