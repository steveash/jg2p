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

import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

import cc.mallet.classify.AdaBoostTrainer;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntL1Trainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.ConfusionMatrix;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Trains a maxent classifier to do A-B reranking
 *
 * @author Steve Ash
 */
public class Rerank2Trainer {

  private static final Logger log = LoggerFactory.getLogger(Rerank2Trainer.class);

  private final Pipe pipe;

  public Rerank2Trainer() {
    pipe = makePipe();
  }

  /**
   * Takes "one-sided" rerank examples and will create the "flip" side and then train on both (so we dont learn to
   * just prefer one side over the other
   * @param trainingData
   * @return
   */
  public Rerank2Model trainFor(Collection<RerankExample> trainingData) {
    InstanceList instances = convert(trainingData);
//    MaxEntTrainer trainer = new MaxEntTrainer(10.0);
    MaxEntL1Trainer trainer = new MaxEntL1Trainer();
//    AdaBoostTrainer trainer = new AdaBoostTrainer(new MaxEntL1Trainer(), 10);
    Classifier model = trainer.train(instances);
    Trial trial = new Trial(model, instances);
    log.info("Trained reranker. Final accuracy on itself: " + trial.getAccuracy());
    log.info(new ConfusionMatrix(trial).toString());
    return new Rerank2Model(model);
  }

  public InstanceList convert(Collection<RerankExample> trainingData) {
    InstanceList instances = new InstanceList(pipe, trainingData.size());
    int count = 0;
    for (RerankExample data : trainingData) {
      instances.addThruPipe(new Instance(data, checkNotNull(data.getLabel()), null, null));
      RerankExample flip = data.flip();
      instances.addThruPipe(new Instance(flip, checkNotNull(flip.getLabel()), null, null));
      count += 1;

      if (count % 10000 == 0) {
        log.info("Loaded " + count + " instances ...");
      }
    }
    log.info("Loaded all " + instances.size() + " instances");
    return instances;
  }

  private static Pipe makePipe() {
    Alphabet alpha = new Alphabet();
    LabelAlphabet labelAlpha = new LabelAlphabet();
    Target2Label labelPipe = new Target2Label(alpha, labelAlpha);

    return new SerialPipes(ImmutableList.of(
        new LoadExamplePipe(alpha, labelAlpha),
        new DupsPipe(alpha, labelAlpha),
        new ModePipe(alpha, labelAlpha),
        new PrefixPipe(alpha, labelAlpha),
        new RanksPipe(alpha, labelAlpha),
        new ScoresPipe(alpha, labelAlpha),
        new ShapePipe(alpha, labelAlpha),
        new ShapePrefixPipe(alpha, labelAlpha),
//        new VowelBigramPipe(alpha, labelAlpha),
//        new VowelPatternPipe(alpha, labelAlpha),
        new ExampleToFeatureVectorPipe(alpha, labelAlpha),
        labelPipe
    ));

  }
}
