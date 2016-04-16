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

package com.github.steveash.jg2p.seq;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.util.FeatureSelections;
import com.github.steveash.jg2p.util.GramBuilder;
import com.github.steveash.jg2p.util.ModelReadWrite;
import com.github.steveash.jg2p.util.ReadWrite;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.TokenAccuracyEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2LabelSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSelection;
import cc.mallet.types.FeatureSelector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.RankedFeatureVector;

import static com.github.steveash.jg2p.util.FeatureSelections.featureCountsFrom;
import static com.github.steveash.jg2p.util.FeatureSelections.featureSumFrom;
import static com.github.steveash.jg2p.util.FeatureSelections.writeRankedToFile;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Trains a CRF to use the alignment model
 *
 * @author Steve Ash
 */
public class PhonemeCrfTrainer {

  private static final Logger log = LoggerFactory.getLogger(PhonemeCrfTrainer.class);

  public static PhonemeCrfTrainer open(TrainOptions opts) {
    PhonemeCrfTrainer pct = new PhonemeCrfTrainer(opts);
    return pct;
  }


  private final TrainOptions opts;

  private CRF crf = null;
  private CRF crfFrom = null;
  private TransducerTrainer lastTrainer = null;

  private PhonemeCrfTrainer(TrainOptions opts) {
    this.opts = opts;
    if (opts.initCrfFromModelFile != null) {
      try {
        log.info("Loading initial weights from " + opts.initCrfFromModelFile);
        this.crfFrom = readCrfFrom();
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
  }

  private void initializeFor(InstanceList examples) {
    this.crf = new CRF(examples.getPipe(), null);
    crf.addOrderNStates(examples, new int[]{1}, null, null, null, null, false);
    crf.addStartState();
    crf.setWeightsDimensionAsIn(examples, false);

    if (crfFrom != null) {
      crf.initializeApplicableParametersFrom(crfFrom);
    }
  }

  private CRF readCrfFrom() throws IOException, ClassNotFoundException {
    return ModelReadWrite.readPronouncerFrom(opts.initCrfFromModelFile).getCrf();
  }

  public void trainFor(Collection<Alignment> inputs) {
    // this pipe is the default pipe with new alphabet
    Stopwatch watch = Stopwatch.createStarted();
    trainRound(inputs, new Alphabet(), 0);

    crf.getInputAlphabet().stopGrowth();
    crf.getOutputAlphabet().stopGrowth();
    watch.stop();
    log.info("Training took " + watch);
  }

  private void trainRound(Collection<Alignment> inputs, Alphabet alpha, int trainRound) {
    SerialPipes initialPipe = makePipe(alpha);
    InstanceList examples = makeExamplesFromAligns(initialPipe, inputs);
    initializeFor(examples);

    CRFTrainerByThreadedLabelLikelihood trainer = makeNewTrainer(crf);
    this.lastTrainer = trainer;

    trainer.train(examples, opts.maxPronouncerTrainingIterations);
    trainer.shutdown(); // just closes the pool; next call to train will create a new one

    if (trainRound == 0 && opts.trimFeaturesUnderPercentile > 0) {
      trainer.getCRF().pruneFeaturesBelowPercentile(opts.trimFeaturesUnderPercentile);
      trainer.train(examples);
      trainer.shutdown();
    }
    if (trainRound == 0 && opts.trimFeaturesByGradientGain > 0) {

      // calc the gradients, report some stats on them, then move on for now
      log.info("Trimming based on gradiant gain ratio...");
//      String dateString = DateFormatUtils.format(new Date(), "yyMMddmmss");
      RankedFeatureVector rfv = FeatureSelections.gradientGainRatioFrom(examples, crf);

//      writeRankedToFile(pair.getLeft(), new File("grads" + dateString + ".txt"));
//      writeRankedToFile(pair.getRight(), new File("gradratio" + dateString + ".txt"));
//      writeRankedToFile(featureCountsFrom(examples), new File("featcounts" + dateString + ".txt"));

      Alphabet newDict = new Alphabet();
      for (int i = 0; i < rfv.singleSize(); i++) {
        double ratio = rfv.value(i);
        if (ratio > opts.trimFeaturesByGradientGain) {
          newDict.lookupIndex(alpha.lookupObject(i), true);
        }
      }
      log.info("Feature selection before count " + alpha.size() + " after " + newDict.size());
      newDict.stopGrowth();
      this.crfFrom = this.crf;

      trainRound(inputs, newDict, trainRound + 1);
    }
  }

  private double accuracyFor(InstanceList examples) {
    TokenAccuracyEvaluator teval = new TokenAccuracyEvaluator(examples, "train");
    teval.evaluate(lastTrainer);
    return teval.getAccuracy("train");
  }

  public PhonemeCrfModel buildModel() {

    return new PhonemeCrfModel(crf);
  }

  private static CRFTrainerByThreadedLabelLikelihood makeNewTrainer(CRF crf) {
    CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(crf, getCpuCount());
    trainer.setGaussianPriorVariance(2);
    trainer.setAddNoFactors(true);
    trainer.setUseSomeUnsupportedTrick(false);
    return trainer;
  }

  private static CRFTrainerByLabelLikelihood makeNewTrainerSingleThreaded(CRF crf) {
    CRFTrainerByLabelLikelihood trainer = new CRFTrainerByLabelLikelihood(crf);
    trainer.setGaussianPriorVariance(2);
//    trainer.setUseHyperbolicPrior(true);
    trainer.setAddNoFactors(true);
    trainer.setUseSomeUnsupportedTrick(false);
    return trainer;
  }

//  private static CRF makeNewCrf(InstanceList examples, Pipe pipe) {
//    CRF crf = new CRF(pipe, null);
//    crf.addOrderNStates(examples, new int[]{1}, null, null, null, null, false);
//    crf.addStartState();
//    crf.setWeightsDimensionDensely();
//    crf.setWeightsDimensionAsIn(examples, false);
//    crf.setWeightsDimensionWithFilterAsIn(examples, 2);
//    crf.addFullyConnectedStatesForBiLabels();
//    crf.addStartState();
//    return crf;
//  }

  private static int getCpuCount() {
    return Runtime.getRuntime().availableProcessors();
  }

  public void writeModel(File target) throws IOException {
    CRF crf = (CRF) lastTrainer.getTransducer();
    ReadWrite.writeTo(new PhonemeCrfModel(crf), target);
    log.info("Wrote for whole data");
  }

  private InstanceList makeExamplesFromAligns(Pipe pipe, Iterable<Alignment> alignsToTrain) {
    int count = 0;
    InstanceList instances = new InstanceList(pipe);
    for (Alignment align : alignsToTrain) {
      List<String> phones = align.getAllYTokensAsList();
      updateEpsilons(phones);
      Instance ii = new Instance(align, phones, null, null);
      instances.addThruPipe(ii);
      count += 1;
    }
    log.info("Read {} instances of training data for pronouncer training", count);
    return instances;
  }

  private Iterable<Alignment> getAlignsFromGroup(List<SeqInputReader.AlignGroup> inputs) {
    return FluentIterable.from(inputs).transformAndConcat(
        new Function<SeqInputReader.AlignGroup, Iterable<Alignment>>() {
          @Override
          public Iterable<Alignment> apply(SeqInputReader.AlignGroup input) {
            return input.alignments;
          }
        });
  }

  private static void updateEpsilons(List<String> phones) {
    String last = GramBuilder.EPS;
    int blankCount = 0;
    for (int i = 0; i < phones.size(); i++) {
      String p = phones.get(i);
      if (isBlank(p)) {
//        phones.set(i, last + "_" + blankCount);
        phones.set(i, GramBuilder.EPS);
        blankCount += 1;
      } else {
        last = p;
        blankCount = 0;
      }
    }
  }

  private SerialPipes makePipe(Alphabet alpha) {
    Target2LabelSequence labelPipe = new Target2LabelSequence();
    LabelAlphabet labelAlpha = (LabelAlphabet) labelPipe.getTargetAlphabet();

    return new SerialPipes(ImmutableList.of(
        new AlignmentToTokenSequence(alpha, labelAlpha),   // convert to token sequence
        new TokenSequenceLowercase(),                       // make all lowercase
        new NeighborTokenFeature(true, makeNeighbors()),         // grab neighboring graphemes
        new NeighborShapeFeature(true, makeShapeNeighs()),
//        new WindowFeature(false, 4),
//        new WindowFeature(true, 6),
        new NeighborSyllableFeature(-2, -1, 1, 2),
        new SyllCountingFeature(),
        new SyllMarkingFeature(),
        new EndingVowelFeature(),
        new SonorityFeature2(true),
        new SonorityFeature2(false),
        new SurroundingTokenFeature2(false, 1, 1),
//        new SurroundingTokenFeature2(true, 1, 1),
        new SurroundingTokenFeature2(false, 2, 2),
        new SurroundingTokenFeature2(false, 3, 1),
        new SurroundingTokenFeature2(true, 3, 3),
//        new SurroundingTokenFeature2(true, 4, 4),
//        new LeadingTrailingFeature(),
        new TokenSequenceToFeature(),                       // convert the strings in the text to features
        new TokenSequence2FeatureVectorSequence(alpha, true, false),
        labelPipe
    ));
  }

  private static List<TokenWindow> makeShapeNeighs() {
    return ImmutableList.of(
        new TokenWindow(-6, 6),
        new TokenWindow(-5, 5),
        new TokenWindow(-4, 4),
        new TokenWindow(-3, 3),
//        new TokenWindow(-2, 2),
//        new TokenWindow(-1, 1),
//        new TokenWindow(1, 1),
//        new TokenWindow(1, 2),
        new TokenWindow(1, 3),
        new TokenWindow(1, 4),
        new TokenWindow(1, 5),
        new TokenWindow(1, 6)
    );
  }

  private static List<TokenWindow> makeNeighbors() {
    return ImmutableList.of(
        new TokenWindow(1, 1),
        new TokenWindow(1, 2),
        new TokenWindow(2, 1),
        new TokenWindow(1, 3),
        new TokenWindow(3, 1),
        new TokenWindow(1, 4),
//        new TokenWindow(4, 1),
        new TokenWindow(-1, 1),
        new TokenWindow(-2, 2),
        new TokenWindow(-2, 1),
        new TokenWindow(-3, 3),
        new TokenWindow(-4, 4),
        new TokenWindow(-5, 5)

//        new TokenWindow(-2, 2),
//        new TokenWindow(-3, 3)
//        new TokenWindow(-4, 4),
    );
  }
}
