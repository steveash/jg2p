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

package com.github.steveash.jg2p.seqvow;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;

/**
 * The master pipe that expects to get a PartialTagging for the data element and it runs through all of the seqVowPipes
 * and then constructs the featureVectorSequence from them
 *
 * @author Steve Ash
 */
public class RetaggerMasterPipe extends Pipe implements Serializable {

  private static final long serialVersionUID = -1808521855323308605L;
  private final List<? extends RetaggerPipe> pipes;

  public RetaggerMasterPipe(Alphabet dataDict, Alphabet targetDict, List<? extends RetaggerPipe> pipes) {
    super(dataDict, targetDict);
    this.pipes = pipes;
  }

  @Override
  public Instance pipe(Instance inst) {
    PartialTagging data = (PartialTagging) inst.getData();

    List<FeatureVector> vectors = Lists.newArrayList();
    for (Integer index : data.getPredictionIndexes()) {
      processPipes(data, index);
      vectors.add(makeVector(data, index));
    }
    if (vectors.isEmpty()) {
      throw new IllegalArgumentException("Cant seqvow a word with no vowels");
    }
    updateTarget(data, inst);
    inst.setData(new FeatureVectorSequence(vectors.toArray(new FeatureVector[vectors.size()])));
    return inst;
  }

  private void updateTarget(PartialTagging data, Instance inst) {
    if (!data.hasExpectedPhones()) {
      return;
    }
    LabelAlphabet labelAlpha = (LabelAlphabet) this.getTargetAlphabet();
    List<String> expectedTags = data.getExpectedPredictedTags();
    List<Label> predictedPhones = Lists.newArrayList();
    for (String tag : expectedTags) {
      Label lbl = labelAlpha.lookupLabel(tag, true);
      predictedPhones.add(lbl);
    }

    Label[] labels = predictedPhones.toArray(new Label[predictedPhones.size()]);
    inst.setTarget(new LabelSequence(labels));
  }

  private FeatureVector makeVector(PartialTagging data, int phoneIndex) {
    Set<String> features = data.getFeatures().get(phoneIndex);
    int[] tags = new int[features.size()];
    int i = 0;
    for (String feature : features) {
      tags[i] = getDataAlphabet().lookupIndex(feature, true);
      i += 1;
    }
    return new FeatureVector(this.getDataAlphabet(), tags);
  }

  private void processPipes(PartialTagging data, int phoneIndex) {
    for (RetaggerPipe pipe : pipes) {
      pipe.pipe(phoneIndex, data);
    }
  }
}
