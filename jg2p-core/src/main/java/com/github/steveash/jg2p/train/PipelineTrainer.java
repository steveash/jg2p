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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import com.github.steveash.jg2p.PipelineModel;
import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.abb.Abbrev;
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
import com.github.steveash.jg2p.phoseq.Graphemes;
import com.github.steveash.jg2p.rerank.Rerank3Model;
import com.github.steveash.jg2p.rerank.Rerank3Trainer;
import com.github.steveash.jg2p.rerank.RerankExample;
import com.github.steveash.jg2p.rerank.RerankExampleCollector;
import com.github.steveash.jg2p.rerank.RerankExampleCsvReader;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer;
import com.github.steveash.jg2p.syll.PhoneSyllTagModel;
import com.github.steveash.jg2p.syllchain.SyllChainModel;
import com.github.steveash.jg2p.syllchain.SyllChainTrainer;
import com.github.steveash.jg2p.syllchain.SyllTagAlignerAdapter;
import com.github.steveash.jg2p.util.ModelReadWrite;
import com.github.steveash.jg2p.util.ReadWrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Trains the entire pipeline and uses TrainOptions to control overall what stages are loaded from previous runs vs what
 * is re-trained from scratch
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
  private SyllChainModel loadedSyllTag;
  private PhonemeCrfModel loadedPronouncer;
  private LangModel loadedGraphone;
  private List<List<RerankExample>> loadedRerankerCsv;
  private Rerank3Model loadedReranker;
  private PhoneSyllTagModel phoneSyllTagModel;

  public static Predicate<? super InputRecord> keepTrainable = new Predicate<InputRecord>() {
    private final SkipTrainings skips = SkipTrainings.defaultSkips();

    @Override
    public boolean apply(InputRecord input) {
      if (PatternFacade.canTranscode(input.xWord)) {
        return false;
      }
      if (skips.skip(input.xWord.getAsNoSpaceString())) {
        return false;
      }
      if (Graphemes.isAllVowelsOrConsonants(input.xWord)) {
        if (input.yWord.getAsSpaceString().equalsIgnoreCase(Abbrev.transcribeAcronym(input.xWord))) {
          // even things that we can't detect but are in fact abbrev should be excluded from
          // training as it mucks with alignments
          return false;
        }
      }
      return true;
    }
  };
  public static Function<InputRecord, InputRecord> trainingXforms = new Function<InputRecord, InputRecord>() {
    @Override
    public InputRecord apply(InputRecord input) {
      Word maybeNew = Graphemes.xformForEval(input.xWord);
      if (maybeNew != input.xWord) { // ref equals
        return new InputRecord(maybeNew, input.yWord);
      }
      return input;
    }
  };

  public void train(List<InputRecord> inputs, TrainOptions opts, PipelineModel model) {
    inputs = FluentIterable.from(inputs)
        .filter(keepTrainable)
        .transform(trainingXforms)
        .toSortedList(InputRecord.OrderByX);
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
      if (opts.trainSyllTag) checkState(opts.useSyllableTagger, "cant train syll tag without using syll tagger");
      if (opts.useSyllableTagger) checkState(isNotBlank(opts.initSyllTagFromFile) || opts.trainSyllTag,
                                             "if using syll tagger, must have a syll tag model or train one");
      if (!opts.trainTrainingAligner) {
        loadedTrainingAligner = ModelReadWrite.readTrainAlignerFrom(opts.initTrainingAlignerFromFile);
      }
      if (!opts.trainTestingAligner) {
        loadedTestAligner = ModelReadWrite.readTestAlignerFrom(opts.initTestingAlignerFromFile);
      }
      if (!opts.trainPronouncer || isNotBlank(opts.initCrfFromModelFile)) {
        loadedPronouncer = ModelReadWrite.readPronouncerFrom(opts.initCrfFromModelFile);
        loadedPronouncer.getCrf().makeParametersHashSparse();
      }
      if ((opts.useSyllableTagger && !opts.trainSyllTag) || (opts.useSyllableTagger && isNotBlank(opts.initSyllTagFromFile))) {
        loadedSyllTag = ModelReadWrite.readSyllTagFrom(opts.initSyllTagFromFile);
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
      throw new IllegalStateException(
          "Failed validating that all inputs can be read and parsed before wasting a lot of time"
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
      PhonemeCrfModel phonemeCrfModel = crfTrainer.buildModel();
      phonemeCrfModel.getCrf().makeParametersHashSparse();
      return phonemeCrfModel;
    }
    return checkNotNull(loadedPronouncer, "shouldve already been loaded in init()");
  }

  private List<Alignment> alignInputs(AlignModel alignModel) {
    return AlignTagTrainer.makeAlignmentInputFromRaw(this.inputs, alignModel, this.opts);
  }

  // this is the aligner -> syll split into two separate models
  private Aligner makeTestAligner() {
    if (opts.trainTestingAligner) {
      AlignTagTrainer alignTagTrainer = new AlignTagTrainer();
      Aligner aligner = alignTagTrainer.train(this.alignedInputs);
      if (opts.useSyllableTagger) {
        SyllChainModel syllTagModel = makeSyllTag();
        aligner = new SyllTagAlignerAdapter(aligner, syllTagModel);
      }
      return aligner;
    }
    return checkNotNull(loadedTestAligner, "shouldve already been loaded in init()");
  }

  // this is the aligner + syll marker in the same model
//  private Aligner makeTestAligner() {
//    if (opts.trainTestingAligner) {
//      if (!opts.useSyllableTagger) {
//        AlignTagTrainer alignTagTrainer = new AlignTagTrainer();
//        return alignTagTrainer.train(this.alignedInputs);
//      }
//      SyllTagTrainer syllTagTrainer = new SyllTagTrainer();
//      if (loadedTestAligner != null) {
//        if (loadedTestAligner instanceof SyllTagModel) {
//          syllTagTrainer.setInitFrom((SyllTagModel) loadedTestAligner);
//        } else {
//          log.warn("Cant init the syll tag from a model that isn't a syll mode, training from scratch");
//        }
//      }
//      return syllTagTrainer.train(this.alignedInputs, null, true);
//    }
//    return checkNotNull(loadedTestAligner, "shouldve already been loaded in init()");
//  }

  private AlignModel makeTrainingAligner() {
    if (opts.trainTrainingAligner) {
      AlignerTrainer alignTrainer = new AlignerTrainer(opts);
      return alignTrainer.train(inputs);
    }
    return checkNotNull(loadedTrainingAligner, "shouldve already been loaded in init()");
  }

  private SyllChainModel makeSyllTag() {
    if (!opts.useSyllableTagger) {
      return null;
    }
    if (opts.trainSyllTag) {
      SyllChainTrainer trainer = new SyllChainTrainer();
      return trainer.train(this.alignedInputs);
    }
    return checkNotNull(loadedSyllTag, "shoulve already loaded syll tag model");
  }

}
