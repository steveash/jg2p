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

import com.github.steveash.jg2p.PipelineModel;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.AlignerTrainer;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.aligntag.AlignTagModel;
import com.github.steveash.jg2p.aligntag.AlignTagTrainer;
import com.github.steveash.jg2p.lm.LangModelTrainer;
import com.github.steveash.jg2p.lm.LangModel;
import com.github.steveash.jg2p.rerank.Rerank2Model;
import com.github.steveash.jg2p.rerank.Rerank2Trainer;
import com.github.steveash.jg2p.rerank.RerankExample;
import com.github.steveash.jg2p.rerank.RerankExampleCollector;
import com.github.steveash.jg2p.rerank.RerankExampleCsvReader;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer;
import com.github.steveash.jg2p.util.ModelReadWrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Encoder trainer that only does a forward pass from alignment stage to phoneme classification stage and doesnt feed
 * anything back to the alignment model
 *
 * @author Steve Ash
 */
public class PipelineTrainer {
  private static final Logger log = LoggerFactory.getLogger(PipelineTrainer.class);

  private List<InputRecord> inputs;
  private List<Alignment> alignedInputs;
  private TrainOptions opts;

  // if we load any then they show up here
  private AlignModel loadedTrainingAligner;
  private AlignTagModel loadedTestAligner;
  private PhonemeCrfModel loadedPronouncer;
  private LangModel loadedGraphone;
  private List<RerankExample> loadedRerankerCsv;
  private Rerank2Model loadedReranker;

  public void train(List<InputRecord> inputs, TrainOptions opts, PipelineModel model) {
    this.inputs = inputs;
    this.opts = opts;
    validateInputs();

    model.setTrainingAlignerModel(makeTrainingAligner());
    this.alignedInputs = alignInputs(model.getTrainingAlignerModel());
    model.setTestingAlignerModel(makeTestAligner());
    model.setPronouncerModel(makePronouncer());
    model.setGraphoneModel(makeGraphoneModel());
    model.setRerankerModel(makeRerankerModel(model));
  }

  private void validateInputs() {
    log.info("Validating that all inputs are good before starting...");
    try {
      if (!opts.trainTrainingAligner) {
        loadedTrainingAligner = ModelReadWrite.readTrainAlignerFrom(opts.initTrainingAlignerFromFile);
      }
      if (!opts.trainTestingAligner) {
        loadedTestAligner = ModelReadWrite.readTestAlignerFrom(opts.initTestingAlignerFromFile);
      }
      if (!opts.trainPronouncer || isNotBlank(opts.initCrfFromModelFile)) {
        loadedPronouncer = ModelReadWrite.readPronouncerFrom(opts.initCrfFromModelFile);
      }
      if (!opts.trainGraphoneModel) {
        loadedGraphone = ModelReadWrite.readGraphoneFrom(opts.initGraphoneModelFromFile);
      }
      if (opts.trainReranker && isNotBlank(opts.useInputRerankExampleCsv)) {
        loadedRerankerCsv = new RerankExampleCsvReader().readFrom(this.opts.useInputRerankExampleCsv);
      }
      if (!opts.trainReranker) {
        loadedReranker = ModelReadWrite.readRerankerFrom(opts.initRerankerFromFile);
      }

      log.info("All model files are loadable");
    } catch (Exception e) {
      throw new IllegalStateException("Failed validating that all inputs can be read and parsed before wasting a lot of time"
                                      + "trying to do training; please correct init model files", e);
    }
  }

  private Rerank2Model makeRerankerModel(PipelineModel modelSoFar) {
    if (opts.trainReranker) {
      List<RerankExample> rrExamples = collectExamples(modelSoFar);
      return new Rerank2Trainer().trainFor(rrExamples);
    }
    return checkNotNull(loadedReranker, "shouldve already been loaded in init()");
  }

  private List<RerankExample> collectExamples(PipelineModel modelSoFar) {
    if (isNotBlank(this.opts.useInputRerankExampleCsv)) {
      log.info("Using the reranker examples csv " + this.opts.useInputRerankExampleCsv);
      return checkNotNull(loadedRerankerCsv, "shouldve already been loaded in init()");
    }
    // we need to collect some
    RerankExampleCollector collector = new RerankExampleCollector(modelSoFar.getRerankEncoder(), this.opts);
    return collector.makeExamples(this.inputs);
  }

  private LangModel makeGraphoneModel() {
    if (opts.trainGraphoneModel) {
      return new LangModelTrainer(this.opts).trainFor(alignedInputs);
    }
    return checkNotNull(loadedGraphone, "shouldve already been loaded in init()");
  }

  private PhonemeCrfModel makePronouncer() {
    if (opts.trainPronouncer) {
      PhonemeCrfTrainer crfTrainer = PhonemeCrfTrainer.open(opts);
      crfTrainer.trainFor(this.alignedInputs);
      return crfTrainer.buildModel();
    }
    return checkNotNull(loadedPronouncer, "shouldve already been loaded in init()");
  }

  private List<Alignment> alignInputs(AlignModel alignModel) {
    return AlignTagTrainer.makeAlignmentInputFromRaw(this.inputs, alignModel, this.opts);
  }

  private AlignTagModel makeTestAligner() {
    if (opts.trainTestingAligner) {
      AlignTagTrainer alignTagTrainer = new AlignTagTrainer();
      return alignTagTrainer.train(this.alignedInputs);
    }
    return checkNotNull(loadedTestAligner, "shouldve already been loaded in init()");
  }

  private AlignModel makeTrainingAligner() {
    if (opts.trainTrainingAligner) {
      AlignerTrainer alignTrainer = new AlignerTrainer(opts);
      return alignTrainer.train(inputs);
    }
    return checkNotNull(loadedTrainingAligner, "shouldve already been loaded in init()");
  }

}
