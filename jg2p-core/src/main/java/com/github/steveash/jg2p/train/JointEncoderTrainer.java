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

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.PhoneticEncoderFactory;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.AlignerTrainer;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.align.ProbTable;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.aligntag.AlignTagModel;
import com.github.steveash.jg2p.aligntag.AlignTagTrainer;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Trainer that uses a multi-stage iterative training procedure where alignments are used to train a CRF that then
 * provides positive examples back to re-train the alignment model.  The alignment model treats the feedback that it
 * gets from the later stage as semi-supervised input and updates the M step of its EM algorithm to take it in to
 * account
 *
 * @author Steve Ash
 */
public class JointEncoderTrainer extends AbstractEncoderTrainer {

  private static final Logger log = LoggerFactory.getLogger(JointEncoderTrainer.class);

  @Override
  protected PhoneticEncoder train(List<InputRecord> inputs, TrainOptions opts) {

    AlignerTrainer alignTrainer = new AlignerTrainer(opts);

    // first model
    AlignModel model = alignTrainer.train(inputs);
    List<Alignment> crfExamples = makeCrfExamples(inputs, model);
    PhonemeCrfModel crfModel;

    try (PhonemeCrfTrainer crfTrainer = PhonemeCrfTrainer.openAndTrain(crfExamples)) {

      int iterCount = 0;
      int previousGoodAligns;
      int goodAlignCount = 0;
      do {
        previousGoodAligns = goodAlignCount;
        crfModel = crfTrainer.buildModel();

        ProbTable goodAligns = new ProbTable();
        goodAlignCount = collectGoodAligns(crfExamples, crfModel, goodAligns);
        log.info("Trained CRF had " + goodAlignCount + " good aligns this time (last time " + previousGoodAligns + ")");

        model = alignTrainer.train(inputs, goodAligns);
        crfExamples = makeCrfExamples(inputs, model);
        crfTrainer.trainFor(crfExamples);

      } while (goodAlignCount > previousGoodAligns && iterCount++ < 5);
    }

    AlignTagTrainer alignTagTrainer = new AlignTagTrainer();
    AlignTagModel alignTagModel = alignTagTrainer.train(crfExamples);
    PhoneticEncoder encoder = PhoneticEncoderFactory.make(alignTagModel, crfModel);

    return encoder;
  }

  private int collectGoodAligns(List<Alignment> crfExamples, PhonemeCrfModel crfModel, ProbTable goodAligns) {
    int goodAlignCount = 0;
    for (Alignment crfExample : crfExamples) {
      List<PhonemeCrfModel.TagResult> predicts = crfModel.tag(crfExample.getAllXTokensAsList(), 1);
      if (predicts.size() > 0) {
        if (predicts.get(0).isEqualTo(crfExample.getYTokens())) {
          // good example, let's increment all of its transitions
          for (Pair<String, String> graphone : crfExample) {
            goodAligns.addProb(graphone.getLeft(), graphone.getRight(), 1.0);
          }
          goodAlignCount += 1;
        }
      }
    }
    return goodAlignCount;
  }
}
