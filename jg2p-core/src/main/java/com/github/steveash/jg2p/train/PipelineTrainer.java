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

import com.google.common.base.Predicate;

import com.github.steveash.jg2p.PipelineModel;
import com.github.steveash.jg2p.abb.PatternFacade;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Aligner;
import com.github.steveash.jg2p.align.AlignerTrainer;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.aligntag.AlignTagTrainer;
import com.github.steveash.jg2p.lm.LangModel;
import com.github.steveash.jg2p.lm.LangModelTrainer;
import com.github.steveash.jg2p.rerank.Rerank3Model;
import com.github.steveash.jg2p.rerank.Rerank3Trainer;
import com.github.steveash.jg2p.rerank.RerankExample;
import com.github.steveash.jg2p.rerank.RerankExampleCollector;
import com.github.steveash.jg2p.rerank.RerankExampleCsvReader;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer;
import com.github.steveash.jg2p.syll.PhoneSyllTagModel;
import com.github.steveash.jg2p.syll.SyllTagModel;
import com.github.steveash.jg2p.syll.SyllTagTrainer;
import com.github.steveash.jg2p.util.ModelReadWrite;
import com.github.steveash.jg2p.util.ReadWrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.filter;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Trains the entire pipeline and uses TrainOptions to control overall what stages are loaded from
 * previous runs vs what is re-trained from scratch
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
  private Aligner loadedTestAligner;
  private PhonemeCrfModel loadedPronouncer;
  private LangModel loadedGraphone;
  private List<List<RerankExample>> loadedRerankerCsv;
  private Rerank3Model loadedReranker;
  private PhoneSyllTagModel phoneSyllTagModel;

  private static Predicate<? super InputRecord> keepTrainable = new Predicate<InputRecord>() {
    @Override
    public boolean apply(InputRecord input) {
      if (PatternFacade.canTranscode(input.xWord)) {
        return false;
      }
      return true;
    }
  };

  public void train(List<InputRecord> inputs, TrainOptions opts, PipelineModel model) {
    inputs = InputRecord.OrderByX.sortedCopy(filter(inputs, keepTrainable));
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
      if (isNotBlank(opts.initPhoneSyllModelFromFile)) {
        phoneSyllTagModel = ReadWrite.readFromFile(PhoneSyllTagModel.class, new File(opts.initPhoneSyllModelFromFile));
      }

      log.info("All model files are loadable");
    } catch (Exception e) {
      throw new IllegalStateException("Failed validating that all inputs can be read and parsed before wasting a lot of time"
                                      + "trying to do training; please correct init model files", e);
    }
  }

  private Rerank3Model makeRerankerModel(PipelineModel modelSoFar) {
    if (opts.trainReranker) {
      LangModel existing = modelSoFar.getGraphoneModel();
      try {
        if (opts.graphoneLanguageModelOrder != opts.graphoneLanguageModelOrderForTraining) {
          // train a graphone model for the different order
          log.info("Need to train a separate graphone model for training...");
          LangModel graphoneModelForTraining = new LangModelTrainer(opts, false).trainFor(alignedInputs);
          log.info("Finished the training graphone model");
          modelSoFar.setGraphoneModel(graphoneModelForTraining);
        }
        Collection<List<RerankExample>> rrExamples = collectExamples(modelSoFar);
        Rerank3Trainer trainer = new Rerank3Trainer();
        if (phoneSyllTagModel != null) {
          trainer.setPhoneSyllModel(phoneSyllTagModel);
        }
        return trainer.trainFor(rrExamples);

      } finally {
        modelSoFar.setGraphoneModel(existing);
      }
    }
    return checkNotNull(loadedReranker, "shouldve already been loaded in init()");
  }

  private Collection<List<RerankExample>> collectExamples(PipelineModel modelSoFar) {
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
      return new LangModelTrainer(this.opts, true).trainFor(alignedInputs);
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

  private Aligner makeTestAligner() {
    if (opts.trainTestingAligner) {
      if (!opts.useSyllableTagger) {
        AlignTagTrainer alignTagTrainer = new AlignTagTrainer();
        return alignTagTrainer.train(this.alignedInputs);
      }
      SyllTagTrainer syllTagTrainer = new SyllTagTrainer();
      if (loadedTestAligner != null) {
        if (loadedTestAligner instanceof SyllTagModel) {
          syllTagTrainer.setInitFrom((SyllTagModel) loadedTestAligner);
        } else {
          log.warn("Cant init the syll tag from a model that isn't a syll mode, training from scratch");
        }
      }
      return syllTagTrainer.train(this.alignedInputs, null, true);
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
