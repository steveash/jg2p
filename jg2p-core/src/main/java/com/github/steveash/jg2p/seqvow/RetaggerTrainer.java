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

package com.github.steveash.jg2p.seqvow;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.util.ReadWrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.TokenAccuracyEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

/**
 * Trains a CRF that takes a graphone seqeunce and predicts the vowels sounds
 *
 * @author Steve Ash
 */
public class RetaggerTrainer {

  private static final Logger log = LoggerFactory.getLogger(RetaggerTrainer.class);

  public static RetaggerTrainer open(TrainOptions opts) {
    Pipe pipe = makePipe();
    RetaggerTrainer pct = new RetaggerTrainer(pipe, opts);
    return pct;
  }

  private enum State {Initializing, Training}

  private final Pipe pipe;
  private final TrainOptions opts;
  private State state = State.Initializing;

  private CRF crf = null;
  private TransducerTrainer lastTrainer = null;

  private boolean printEval = false;

  private RetaggerTrainer(Pipe pipe, TrainOptions opts) {
    this.pipe = pipe;
    this.opts = opts;
  }

  private void initializeFor(InstanceList examples) {
    Preconditions.checkState(state == State.Initializing, "can only initialize once");
    this.crf = new CRF(pipe, null);
    crf.addOrderNStates(examples, new int[]{1}, null, null, null, null, false);
    crf.addStartState();
    //    crf.setWeightsDimensionDensely();
    crf.setWeightsDimensionAsIn(examples, false);
    //    crf.setWeightsDimensionWithFilterAsIn(examples, 2);

    if (opts.initSeqVowFromFile != null) {
      try {
        log.info("Loading initial weights from " + opts.initSeqVowFromFile);
        CRF crfFrom = readCrfFrom();
        crf.initializeApplicableParametersFrom(crfFrom);

      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
  }

  private CRF readCrfFrom() throws IOException, ClassNotFoundException {
    Object model = ReadWrite.readFromFile(Object.class, new File(opts.initSeqVowFromFile));
    if (model instanceof RetaggingModel) {
      return ((RetaggingModel) model).getCrf();
    } else if (model instanceof PhoneticEncoder) {
      return ((PhoneticEncoder) model).getRetagger().getCrf();
    }
    throw new IllegalArgumentException("Dont know how to get a crf out of " + model);
  }

  public void useCrf(CRF crf) {
    Preconditions.checkState(state == State.Initializing);
    this.crf = crf;
    this.state = State.Training;
  }

  public void trainFor(Collection<PartialTagging> inputs) {
    InstanceList examples = makeExamplesFrom(inputs, pipe);
    trainForInstances(examples);
  }

  public void trainForInstances(InstanceList examples) {
    if (state == State.Initializing) {
      initializeFor(examples);
    }
    state = State.Training;
    Stopwatch watch = Stopwatch.createStarted();
    CRFTrainerByThreadedLabelLikelihood trainer = makeNewTrainer(crf);
    this.lastTrainer = trainer;

    trainer.train(examples, opts.maxCrfIterations);
    trainer.shutdown(); // just closes the pool; next call to train will create a new one

    if (opts.trimFeaturesUnderPercentile > 0) {
      trainer.getCRF().pruneFeaturesBelowPercentile(opts.trimFeaturesUnderPercentile);
      trainer.train(examples);
      trainer.shutdown();
    }

    watch.stop();
    log.info("Training took " + watch);
    if (printEval) {
      log.info("Accuracy on training data: " + accuracyFor(examples));
    }
  }

  public double accuracyFor(Collection<PartialTagging> inputs) {
    InstanceList examples = makeExamplesFrom(inputs, pipe);
    return accuracyFor(examples);
  }

  public double accuracyFor(InstanceList examples) {
    Preconditions.checkState(state == State.Training, "cant call before training");
    TokenAccuracyEvaluator teval = new TokenAccuracyEvaluator(examples, "train");
    teval.evaluate(lastTrainer);
    return teval.getAccuracy("train");
  }

  public RetaggingModel buildModel() {
    return new RetaggingModel(crf);
  }

  public void setPrintEval(boolean printEval) {
    this.printEval = printEval;
  }

  private static CRFTrainerByThreadedLabelLikelihood makeNewTrainer(CRF crf) {
    CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(crf, getCpuCount());
    trainer.setGaussianPriorVariance(2);
    trainer.setAddNoFactors(true);
    trainer.setUseSomeUnsupportedTrick(false);
    return trainer;
  }

  private static int getCpuCount() {
    return Runtime.getRuntime().availableProcessors();
  }

  public void writeModel(File target) throws IOException {
    CRF crf = (CRF) lastTrainer.getTransducer();
    ReadWrite.writeTo(buildModel(), target);
    log.info("Wrote for whole data");
  }

  private static InstanceList makeExamplesFrom(Iterable<PartialTagging> toTrain, Pipe pipe) {
    int count = 0;
    InstanceList instances = new InstanceList(pipe);
    for (PartialTagging example : toTrain) {
      Preconditions.checkArgument(example.getExpectedPhonesGrams() != null, "no training label for", example);
      Instance ii = new Instance(example, example.getExpectedPhonesGrams(), null, example);
      instances.addThruPipe(ii);
      count += 1;

//      if (count > 1000) {
//        break;
//      }
    }
    log.info("Read {} instances of training data", count);
    return instances;
  }

  private static Pipe makePipe() {
    Alphabet alpha = new Alphabet();
    LabelAlphabet labelAlpha = new LabelAlphabet();

    return new RetaggerMasterPipe(alpha, labelAlpha, ImmutableList.of(
        new Neighbor(true, -1, 1),
        new Neighbor(true, -2, 1),
        new Neighbor(true, -3, 1),
        new Neighbor(true, -4, 1),
        new Neighbor(false, -1, 1),
        new Neighbor(false, -2, 1),
        new Neighbor(false, -3, 1),
        new Neighbor(false, -4, 1),
        new Neighbor(true, 1, 1),
        new Neighbor(true, 2, 1),
        new Neighbor(true, 3, 1),
        new Neighbor(true, 4, 1),
        new Neighbor(false, 1, 1),
        new Neighbor(false, 2, 1),
        new Neighbor(false, 3, 1),
        new Neighbor(false, 4, 1),

        new Neighbor(false, -2, 2),
        new Neighbor(false, -3, 3),
        new Neighbor(false, -4, 4),
        new Neighbor(false, 1, 2),
        new Neighbor(false, 1, 3),
//        new Neighbor(false, 1, 4),
        new Neighbor(true, -2, 2),
        new Neighbor(true, -3, 3),
        new Neighbor(true, 1, 2),
//        new Neighbor(true, 1, 3),

        new Current(),
        new OriginPrediction(),
        new NearVowel(-1),
        new NearVowel(-2),
        new NearVowel(-3),
        new NearVowel(1),
        new NearVowel(2),
        new NearVowel(3)
    ));
  }
}
