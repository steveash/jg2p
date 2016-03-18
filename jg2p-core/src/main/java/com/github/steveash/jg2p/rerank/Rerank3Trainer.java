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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEntL1Trainer;
import cc.mallet.classify.RankMaxEnt;
import cc.mallet.classify.RankMaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.ConfusionMatrix;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

/**
 * Trains a maxent classifier to do A-B reranking
 *
 * @author Steve Ash
 */
public class Rerank3Trainer {

  private static final Logger log = LoggerFactory.getLogger(Rerank3Trainer.class);

  private final Pipe pipe;

  public Rerank3Trainer() {
    pipe = makePipe();
  }

  /**
   * Takes "one-sided" rerank examples and will create the "flip" side and then train on both (so we dont learn to just
   * prefer one side over the other
   */
  public Rerank3Model trainFor(Collection<List<RerankExample>> trainingData) {
    InstanceList instances = convert(trainingData);
    RankMaxEntTrainer trainer = new RankMaxEntTrainer(10.0);
//    AdaBoostTrainer trainer = new AdaBoostTrainer(new MaxEntL1Trainer(), 10);
    RankMaxEnt model = (RankMaxEnt) trainer.train(instances);
//    Trial trial = new Trial(model, instances);
//    log.info("Trained reranker. Final accuracy on itself: " + trial.getAccuracy());
//    log.info(new ConfusionMatrix(trial).toString());
    return new Rerank3Model(model);
  }

  public InstanceList convert(Collection<List<RerankExample>> trainingData) {
    InstanceList instances = new InstanceList(pipe, trainingData.size());
    int count = 0;
    for (List<RerankExample> data : trainingData) {
      instances.addThruPipe(new Instance(data, 1 /*just putting something here triggers pipe*/, null, data.get(0).getWordGraphs()));
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

    return new SerialPipes(ImmutableList.of(
        new LoadTargetPipe(alpha, labelAlpha),
        new RerankFeaturePipe(alpha, labelAlpha, Arrays.asList(
            new DupsPipe(),
            new ModePipe(),
            new PrefixPipe(),
            new RanksPipe(),
            new ScoresPipe(),
            new ShapePipe(),
            new ShapePrefixPipe()
//        new VowelBigramPipe(alpha, labelAlpha),
//        new VowelPatternPipe(alpha, labelAlpha),
        ))
    ));

  }
}
