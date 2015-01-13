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
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.align.TrainOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Steve Ash
 */
public abstract class AbstractEncoderTrainer {

  private static final Logger log = LoggerFactory.getLogger(SimpleEncoderTrainer.class);

  private List<InputRecord> train;
  private List<InputRecord> test;

  public PhoneticEncoder trainAndEval(List<InputRecord> train, List<InputRecord> test, TrainOptions opts) {
    this.train = train;
    this.test = test;

    PhoneticEncoder encoder = train(train, opts);
    eval(encoder, "FINAL", EncoderEval.PrintOpts.ALL);
    return encoder;
  }

  protected void eval(PhoneticEncoder encoder, String phaseLabel, EncoderEval.PrintOpts opts) {
    EncoderEval eval = new EncoderEval(encoder);
    log.info("--------------------- " + phaseLabel + " Eval on training data ------------------------");
    eval.evalAndPrint(train, opts);
    if (test != null) {
      log.info("--------------------- " + phaseLabel + " Eval on testing data ------------------------");
      EncoderEval eval2 = new EncoderEval(encoder);
      eval2.evalAndPrint(test, opts);
    }
  }

  protected abstract PhoneticEncoder train(List<InputRecord> inputs, TrainOptions opts);

  public static List<Alignment> makeCrfExamples(List<InputRecord> inputs, AlignModel model) {
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
}
