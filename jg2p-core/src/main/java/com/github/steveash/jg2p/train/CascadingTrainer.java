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

import com.google.common.collect.Lists;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.PhoneticEncoderFactory;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.AlignerTrainer;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.aligntag.AlignTagModel;
import com.github.steveash.jg2p.aligntag.AlignTagTrainer;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer;
import com.github.steveash.jg2p.seqbin.SeqBinModel;
import com.github.steveash.jg2p.util.ReadWrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.steveash.jg2p.train.AbstractEncoderTrainer.makeCrfExamples;

/**
 * @author Steve Ash
 */
public class CascadingTrainer {
  private static final Logger log = LoggerFactory.getLogger(CascadingTrainer.class);

  public CascadeEncoder train(List<InputRecord> allInputs, TrainOptions opts, CascadeEncoder previous,
                              File seqBinFile) throws IOException, ClassNotFoundException {
    File prevG = File.createTempFile("prevG", ".dat");
    prevG.deleteOnExit();
    File prevB = File.createTempFile("prevB", ".dat");
    prevB.deleteOnExit();
    ReadWrite.writeTo(previous.getEncoderG(), prevG);
    ReadWrite.writeTo(previous.getEncoderB(), prevB);

    return train(allInputs, opts, prevG, prevB, seqBinFile);
  }

  public CascadeEncoder train(List<InputRecord> allInputs, TrainOptions opts, File crfModelG, File crfModelB,
                              File seqBinFile)
      throws IOException, ClassNotFoundException {

    SeqBinModel seqBinModel = ReadWrite.readFromFile(SeqBinModel.class, seqBinFile);

    // alignment model
    AlignerTrainer alignTrainer = new AlignerTrainer(opts);
    AlignTagTrainer alignTagTrainer = new AlignTagTrainer();
    AlignModel model = alignTrainer.train(allInputs);

    // align tag model
    List<Alignment> allExamples = makeCrfExamples(allInputs, model, opts);
    AlignTagModel alignTagModel = alignTagTrainer.train(allExamples);

    List<InputRecord> goodExamples = findExamples(allInputs, "G");
    PhonemeCrfModel modelG = trainModel(goodExamples, opts, crfModelG, model);

    // train the "B" model just for those bad examples
    List<InputRecord> badExamples = findExamples(allInputs, "B");
    PhonemeCrfModel modelB = trainModel(badExamples, opts, crfModelB, model);

    PhoneticEncoder encoderG = PhoneticEncoderFactory.make(alignTagModel, modelG);
    PhoneticEncoder encoderB = PhoneticEncoderFactory.make(alignTagModel, modelB);

    return new CascadeEncoder(model, encoderG, encoderB, seqBinModel);
  }

  protected PhonemeCrfModel trainModel(List<InputRecord> inputs, TrainOptions opts, File initFrom, AlignModel model) {
    List<Alignment> examples = makeCrfExamples(inputs, model, opts);

    if (initFrom != null) {
      opts.initCrfFromModelFile = initFrom.getAbsolutePath();
    }
    PhonemeCrfTrainer crfTrainer = PhonemeCrfTrainer.open(opts);

    log.info("Training the model with " + examples.size() + " aligned examples (from " + inputs.size() + " inputs)");
    crfTrainer.trainFor(examples);
    PhonemeCrfModel crf = crfTrainer.buildModel();
    return crf;
  }

  private List<InputRecord> findExamples(List<InputRecord> allInputs, String memo) {
    ArrayList<InputRecord> badInputs = Lists.newArrayList();
    for (InputRecord input : allInputs) {
      if (input.memo != null && input.memo.equalsIgnoreCase(memo)) {
        badInputs.add(input);
      }
    }
    return badInputs;
  }
}
