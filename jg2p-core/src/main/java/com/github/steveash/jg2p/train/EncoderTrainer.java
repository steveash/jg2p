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

package com.github.steveash.jg2p.train;

import com.google.common.collect.Lists;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.PhoneticEncoderFactory;
import com.github.steveash.jg2p.align.AlignerTrainer;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.G2PModel;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Steve Ash
 */
public class EncoderTrainer {

  private static final Logger log = LoggerFactory.getLogger(EncoderTrainer.class);

  public void trainAndEval(List<InputRecord> train, List<InputRecord> test, TrainOptions opts) {
    PhoneticEncoder encoder = train(train, opts);
    eval(train, test, encoder);
  }

  private void eval(List<InputRecord> train, List<InputRecord> test, PhoneticEncoder encoder) {
    EncoderEval eval = new EncoderEval(encoder);
    log.info("--------------------- Eval on training data ------------------------");
    eval.evalAndPrint(train);
    log.info("--------------------- Eval on testing data ------------------------");
    eval.evalAndPrint(test);
  }

  private PhoneticEncoder train(List<InputRecord> inputs, TrainOptions opts) {
    AlignerTrainer alignTrainer = new AlignerTrainer(opts);
    PhonemeCrfTrainer crfTrainer = new PhonemeCrfTrainer();

    G2PModel model = alignTrainer.train(inputs);
    List<Alignment> crfExamples = makeCrfExamples(inputs, model);
    PhonemeCrfModel crfModel = crfTrainer.train(crfExamples);
    PhoneticEncoder encoder = PhoneticEncoderFactory.make(model, crfModel);
    return encoder;
  }

  private List<Alignment> makeCrfExamples(List<InputRecord> inputs, G2PModel model) {
    List<Alignment> examples = Lists.newArrayListWithCapacity(inputs.size());
    for (InputRecord input : inputs) {
      List<Alignment> best = model.align(input.xWord, input.yWord, 5);

      for (Alignment pairs : best) {
        if (pairs.getScore() > -150) {
          examples.add(pairs);
        }
      }

//      if (!best.isEmpty()) {
//        of any that are above the threshold find the one with the highest score (where score is calculated by gram size)
//
//        examples.add(best.get(0));
//      }
    }
    return examples;
  }

  private double score(Alignment alignment) {
    double score = 0;
    for (String token : alignment.getXTokens()) {
      score += token.length() * token.length();
    }
    return score;
  }
}
