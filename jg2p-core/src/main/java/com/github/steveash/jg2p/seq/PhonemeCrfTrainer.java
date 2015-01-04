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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.util.ReadWrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2LabelSequence;
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
public class PhonemeCrfTrainer implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(PhonemeCrfTrainer.class);

  public static PhonemeCrfTrainer openAndTrain(Collection<Alignment> examples) {
    Pipe pipe = makePipe();
    InstanceList instances = makeExamplesFromAligns(examples, pipe);

    CRF crf = makeNewCrf(instances, pipe);
    CRFTrainerByThreadedLabelLikelihood trainer = makeNewTrainer(crf);

    PhonemeCrfTrainer pct = new PhonemeCrfTrainer(pipe, trainer);
    pct.trainForInstances(instances);
    return pct;
  }

  private final Pipe pipe;
  private final CRFTrainerByThreadedLabelLikelihood trainer;

  private PhonemeCrfTrainer(Pipe pipe, CRFTrainerByThreadedLabelLikelihood trainer) {
    this.pipe = pipe;
    this.trainer = trainer;
  }

  public void trainFor(Collection<Alignment> inputs) {
    InstanceList examples = makeExamplesFromAligns(inputs, pipe);
    trainForInstances(examples);
  }

  public void trainForInstances(InstanceList examples) {
    Stopwatch watch = Stopwatch.createStarted();
    trainer.train(examples);
    trainer.shutdown(); // just closes the pool; next call to train will create a new one
    watch.stop();
    log.info("Training took " + watch);
  }


  public PhonemeCrfModel buildModel() {
    return new PhonemeCrfModel(trainer.getCRF());
  }

  private static CRFTrainerByThreadedLabelLikelihood makeNewTrainer(CRF crf) {
    CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(crf, getCpuCount());
    trainer.setGaussianPriorVariance(2);
    return trainer;
  }

  private static CRF makeNewCrf(InstanceList examples, Pipe pipe) {
    CRF crf = new CRF(pipe, null);
    crf.addOrderNStates(examples, new int[]{1}, null, null, null, null, false);
    crf.addStartState();
    crf.setWeightsDimensionAsIn(examples, false);
    return crf;
  }

  private static int getCpuCount() {
    return Runtime.getRuntime().availableProcessors();
  }


  public void writeModel() throws IOException {
    writeModel(new File("g2p_crf.dat"));
  }

  public void writeModel(File target) throws IOException {
    CRF crf = (CRF) trainer.getTransducer();
    ReadWrite.writeTo(new PhonemeCrfModel(crf), target);
    log.info("Wrote for whole data");
  }

  private static InstanceList makeExamplesFromAligns(Iterable<Alignment> alignsToTrain, Pipe pipe) {
    int count = 0;
    InstanceList instances = new InstanceList(pipe);
    for (Alignment align : alignsToTrain) {
      List<String> phones = align.getAllYTokensAsList();
      updateEpsilons(phones);
      Instance ii = new Instance(align.getAllXTokensAsList(), phones, null, null);
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
    Target2LabelSequence labelPipe = new Target2LabelSequence();
    LabelAlphabet labelAlpha = (LabelAlphabet) labelPipe.getTargetAlphabet();

    return new SerialPipes(ImmutableList.of(
        new StringListToTokenSequence(alpha, labelAlpha),   // convert to token sequence
        new TokenSequenceLowercase(),                       // make all lowercase
        new NeighborTokenFeature(true, makeNeighbors()),         // grab neighboring graphemes
        new TokenSequenceToFeature(),                       // convert the strings in the text to features
        new TokenSequence2FeatureVectorSequence(alpha, true, true),
        labelPipe
    ));
  }

  private static List<NeighborTokenFeature.NeighborWindow> makeNeighbors() {
    return ImmutableList.of(
        new NeighborTokenFeature.NeighborWindow(1, 1),
        new NeighborTokenFeature.NeighborWindow(2, 1),
        new NeighborTokenFeature.NeighborWindow(3, 1),
//        new NeighborTokenFeature.NeighborWindow(1, 2),
//              new NeighborTokenFeature.NeighborWindow(1, 3),
        new NeighborTokenFeature.NeighborWindow(-1, 1),
        new NeighborTokenFeature.NeighborWindow(-2, 1),
        new NeighborTokenFeature.NeighborWindow(-3, 1),
        new NeighborTokenFeature.NeighborWindow(-2, 2)
//        new NeighborTokenFeature.NeighborWindow(-3, 3)
    );
  }

  @Override
  public void close() {
    // we shut down the pool after training iterations so we don't leak pools (le sigh mallet)
  }
}
