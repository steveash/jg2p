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

package com.github.steveash.jg2p.seq;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.util.ReadWrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.HMM;
import cc.mallet.fst.HMMTrainerByLikelihood;
import cc.mallet.fst.TokenAccuracyEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2FeatureSequence;
import cc.mallet.pipe.Target2LabelSequence;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Trains a CRF to use the alignment model
 *
 * @author Steve Ash
 */
public class PhonemeHmmTrainer {

  private static final Logger log = LoggerFactory.getLogger(PhonemeHmmTrainer.class);

  public static PhonemeHmmTrainer open(TrainOptions opts) {
    Pipe pipe = makePipe();
    PhonemeHmmTrainer pct = new PhonemeHmmTrainer(pipe, opts);
    return pct;
  }

  private static enum State {Initializing, Training}

  private final Pipe pipe;
  private final TrainOptions opts;
  private State state = State.Initializing;

  private HMM hmm = null;
  private TransducerTrainer lastTrainer = null;

  private boolean printEval = false;

  private PhonemeHmmTrainer(Pipe pipe, TrainOptions opts) {
    this.pipe = pipe;
    this.opts = opts;
  }

  private void initializeFor(InstanceList examples) {
    Preconditions.checkState(state == State.Initializing, "can only initialize once");
    this.hmm = new HMM(pipe, null);
//    hmm.addOrderNStates(examples, new int[]{1}, null, null, null, null, false);
    hmm.addStatesForLabelsConnectedAsIn(examples);
    hmm.addStartState();
//    hmm.addSelfTransitioningStateForAllLabels();
    Random rand = new Random(0xCAFEBABE);
    hmm.initEmissions(rand, 0.0);
    hmm.initTransitions(rand, 0.0);

  }


  public void trainFor(Collection<Alignment> inputs) {
    InstanceList examples = makeExamplesFromAligns(inputs, pipe);
    trainForInstances(examples);
  }

  public void trainForInstances(InstanceList examples) {

    if (state == State.Initializing) {
      initializeFor(examples);
    }
    state = State.Training;
    Stopwatch watch = Stopwatch.createStarted();
    HMMTrainerByLikelihood trainer = makeNewTrainer(hmm);
    this.lastTrainer = trainer;

    trainer.train(examples, opts.maxIterations);

    watch.stop();
    log.info("Training took " + watch);
    if (printEval) {
      log.info("Accuracy on training data: " + accuracyFor(examples));
    }
  }

  public double accuracyFor(Collection<Alignment> inputs) {
    InstanceList examples = makeExamplesFromAligns(inputs, pipe);
    return accuracyFor(examples);
  }

  public double accuracyFor(InstanceList examples) {
    Preconditions.checkState(state == State.Training, "cant call before training");
    TokenAccuracyEvaluator teval = new TokenAccuracyEvaluator(examples, "train");
    teval.evaluate(lastTrainer);
    return teval.getAccuracy("train");
  }

  public PhonemeCrfModel buildModel() {
    return new PhonemeCrfModel(hmm);
  }

  public void setPrintEval(boolean printEval) {
    this.printEval = printEval;
  }

  private static HMMTrainerByLikelihood makeNewTrainer(HMM hmm) {
    HMMTrainerByLikelihood trainer = new HMMTrainerByLikelihood(hmm);
    trainer.setThreshold(1.0);
//    trainer.setGaussianPriorVariance(2);
//    trainer.setAddNoFactors(true);
//    trainer.setUseSomeUnsupportedTrick(false);
    return trainer;
  }

  private static int getCpuCount() {
    return Runtime.getRuntime().availableProcessors();
  }

  public void writeModel(File target) throws IOException {
    HMM hmm = (HMM) lastTrainer.getTransducer();
    ReadWrite.writeTo(new PhonemeCrfModel(hmm), target);
    log.info("Wrote for whole data");
  }

  private static InstanceList makeExamplesFromAligns(Iterable<Alignment> alignsToTrain, Pipe pipe) {
    int count = 0;
    InstanceList instances = new InstanceList(pipe);
    for (Alignment align : alignsToTrain) {
      List<String> phones = align.getAllYTokensAsList();
      updateEpsilons(phones);
//      if (!align.getWordAsSpaceString().equals("X A V I E R A")) continue;
      Instance ii = new Instance(align.getAllXTokensAsList(), phones, align.getWordAsSpaceString(), null);
      instances.addThruPipe(ii);
      count += 1;

//      if (count > 1000) {
//        break;
//      }
    }
    log.info("Read {} instances of training data", count);
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
    String last = "<EPS>";
    int blankCount = 0;
    for (int i = 0; i < phones.size(); i++) {
      String p = phones.get(i);
      if (isBlank(p)) {
//        phones.set(i, last + "_" + blankCount);
        phones.set(i, "<EPS>");
        blankCount += 1;
      } else {
        last = p;
        blankCount = 0;
      }
    }
  }

  private static Pipe makePipe() {
    Alphabet alpha = new Alphabet();
    Target2FeatureSequence labelPipe = new Target2FeatureSequence();
    Alphabet labelAlpha = labelPipe.getTargetAlphabet();

    return new SerialPipes(ImmutableList.of(
        new StringListToTokenSequence(alpha, labelAlpha),   // convert to token sequence
        new TokenSequenceLowercase(),                       // make all lowercase
//        new NeighborTokenFeature(true, makeNeighbors()),         // grab neighboring graphemes
//        new NeighborShapeFeature(true, makeShapeNeighs()),
//        new TokenSequenceToFeature(),                       // convert the strings in the text to features
        new TokenSequence2FeatureSequence(alpha),
        labelPipe
    ));
  }

  private static List<TokenWindow> makeShapeNeighs() {
    return ImmutableList.of(
        new TokenWindow(-5, 5),
        new TokenWindow(-4, 4),
        new TokenWindow(-3, 3),
        new TokenWindow(-2, 2),
        new TokenWindow(-1, 1),
        new TokenWindow(1, 1),
        new TokenWindow(1, 2),
        new TokenWindow(1, 3),
        new TokenWindow(1, 4),
        new TokenWindow(1, 5)
    );
  }

  private static List<TokenWindow> makeNeighbors() {
    return ImmutableList.of(
        new TokenWindow(1, 1),
        new TokenWindow(2, 1),
        new TokenWindow(3, 1),
        new TokenWindow(1, 2),
              new TokenWindow(1, 3),
        new TokenWindow(-1, 1),
        new TokenWindow(-2, 1),
        new TokenWindow(-3, 1),
        new TokenWindow(-2, 2)
//        new TokenWindow(-3, 3)
//        new TokenWindow(-4, 4),
    );
  }
}
