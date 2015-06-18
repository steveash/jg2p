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

package com.github.steveash.jg2p.train;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.AlignerTrainer;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.seqvow.PartialPhones;
import com.github.steveash.jg2p.seqvow.PartialTagging;
import com.github.steveash.jg2p.seqvow.RetaggerTrainer;
import com.github.steveash.jg2p.seqvow.RetaggingModel;
import com.github.steveash.jg2p.util.ReadWrite;

import java.io.File;
import java.util.List;

/**
 * Encoder that trains the retagger model by training both a CRF that outputs partials and
 *
 * @author Steve Ash
 */
public class RetaggingEncoderTrainer extends AbstractEncoderTrainer {

  @Override
  public PhoneticEncoder train(List<InputRecord> inputs, TrainOptions opts) {
    if (!opts.useRetagger) {
      throw new IllegalArgumentException("cant use retagging encoder trainer without the --useRetagger option");
    }
    AlignerTrainer alignTrainer = new AlignerTrainer(opts);
    AlignModel model = alignTrainer.train(inputs);

    PhoneticEncoder simpleEncoder;
    try {
      simpleEncoder = ReadWrite.readFromFile(PhoneticEncoder.class, new File(opts.initCrfFromModelFile));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
//    SimpleEncoderTrainer simple = new SimpleEncoderTrainer(true);
//    PhoneticEncoder simpleEncoder = simple.train(inputs, opts);

    TrainOptions clone = opts.clone();
    clone.topKAlignCandidates = 1;  // for retagging we only train with 1
    List<PartialTagging> trainingInput = makeRetaggerInputs(inputs, model, clone);
    RetaggerTrainer trainer = RetaggerTrainer.open(clone);
    trainer.trainFor(trainingInput);
    RetaggingModel retagger = trainer.buildModel();
    simpleEncoder.setRetagger(retagger);

    return simpleEncoder;
  }

  protected List<PartialTagging> makeRetaggerInputs(List<InputRecord> inputs, AlignModel model, TrainOptions clone) {
    List<Alignment> alignsForRetagTrain = AbstractEncoderTrainer.makeCrfExamples(inputs, model, clone);
    List<PartialTagging> trainingInput = Lists.newArrayListWithCapacity(alignsForRetagTrain.size());
    for (Alignment align : alignsForRetagTrain) {
      try {
        List<String> phoneGrams = align.getAllYTokensAsList();
        if (!PartialPhones.doesAnyGramContainPhoneEligibleAsPartial(phoneGrams)) {
          continue;
        }
        PartialTagging input =
            PartialTagging.createFromGraphsAndFinalPhoneGrams(align.getAllXTokensAsList(), phoneGrams);
        trainingInput.add(input);
      } catch (Exception e) {
        throw new IllegalArgumentException("Problem trying to make example from $align", e);
      }
    }
    return trainingInput;
  }

}
