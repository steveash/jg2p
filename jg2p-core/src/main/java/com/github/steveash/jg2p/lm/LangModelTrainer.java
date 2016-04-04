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

package com.github.steveash.jg2p.lm;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;

import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.kylm.model.immutable.ImmutableLM;
import com.github.steveash.kylm.model.immutable.ImmutableLMConverter;
import com.github.steveash.kylm.model.ngram.NgramLM;
import com.github.steveash.kylm.model.ngram.smoother.KNSmoother;
import com.github.steveash.kylm.model.ngram.smoother.MKNSmoother;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Trains a graphone model based on aligned exampled
 * @author Steve Ash
 */
public class LangModelTrainer {
  private static final Logger log = LoggerFactory.getLogger(LangModelTrainer.class);

  private final TrainOptions opts;
  private final int modelOrder;

  public LangModelTrainer(TrainOptions opts, boolean isForTesting) {
    this.opts = opts;
    if (isForTesting) {
      this.modelOrder = opts.graphoneLanguageModelOrder;
    } else {
      this.modelOrder = opts.graphoneLanguageModelOrderForTraining;
    }
  }

  public LangModel trainFor(Collection<Alignment> inputs) {
    MKNSmoother smoother = new MKNSmoother();
    smoother.setSmoothUnigrams(true);
    NgramLM lm = new NgramLM(modelOrder, smoother);
    Iterable<String[]> trainInput = FluentIterable.from(inputs).transform(new Function<Alignment, String[]>() {
      @Override
      public String[] apply(Alignment input) {
        return LangModel.makeSequenceFromAlignment(input, opts.graphoneLangModel).toArray(new String[0]);
      }
    });
    try {
      log.info("Starting to train language model on {} inputs", inputs.size());
      lm.trainModel(trainInput);
      log.info("Finished training language model");
      ImmutableLM newlm = new ImmutableLMConverter().convert(lm);
      return new LangModel(newlm, opts.graphoneLangModel);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
