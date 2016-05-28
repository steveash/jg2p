/*
 * Copyright 2016 Steve Ash
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

package com.github.steveash.jg2p.syllchain;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.seq.LeadingTrailingFeature;
import com.github.steveash.jg2p.seq.NeighborShapeFeature;
import com.github.steveash.jg2p.seq.NeighborTokenFeature;
import com.github.steveash.jg2p.seq.StringListToTokenSequence;
import com.github.steveash.jg2p.seq.SurroundingTokenFeature;
import com.github.steveash.jg2p.seq.TokenSequenceToFeature;
import com.github.steveash.jg2p.seq.TokenWindow;
import com.github.steveash.jg2p.syll.SyllTagTrainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2LabelSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

/**
 * @author Steve Ash
 */
public class SyllChainTrainer {

  private static final Logger log = LoggerFactory.getLogger(SyllChainTrainer.class);

  public SyllChainModel train(List<Alignment> aligns) {
    log.info("About to train the syll chain...");
    InstanceList examples = makeExamplesFromAligns(aligns);
    Pipe pipe = examples.getPipe();

    log.info("Training test-time syll chain tagger on whole data...");
    TransducerTrainer trainer = trainOnce(pipe, examples);
    return new SyllChainModel((CRF) trainer.getTransducer());
  }

  private TransducerTrainer trainOnce(Pipe pipe, InstanceList examples) {
    Stopwatch watch = Stopwatch.createStarted();

    CRF crf = new CRF(pipe, null);
    crf.addOrderNStates(examples, new int[]{1}, null, null, null, null, false);
    crf.addStartState();
//    crf.setWeightsDimensionAsIn(examples, false);

    log.info("Starting syllchain training...");
    CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(crf, 8);
    trainer.setGaussianPriorVariance(2);
//    trainer.setUseSomeUnsupportedTrick(false);
//    trainer.setAddNoFactors(true);
    trainer.train(examples);
    trainer.shutdown();
    watch.stop();

    log.info("SyllChain CRF Training took " + watch.toString());
    crf.getInputAlphabet().stopGrowth();
    crf.getOutputAlphabet().stopGrowth();
    return trainer;
  }

  private InstanceList makeExamplesFromAligns(List<Alignment> aligns) {
    Pipe pipe = makePipe();
    int count = 0;
    InstanceList instances = new InstanceList(pipe);
    for (Alignment align : aligns) {

      Word orig = Word.fromSpaceSeparated(align.getWordAsSpaceString());
//      Word marks = Word.fromGrams(SyllTagTrainer.makeSyllableGraphEndMarksFor(align));
      Word marks = Word.fromGrams(SyllTagTrainer.makeOncForGraphemes(align));
      Preconditions.checkState(orig.unigramCount() == marks.unigramCount());

      Instance ii = new Instance(orig.getValue(), marks.getValue(), null, null);
      instances.addThruPipe(ii);
      count += 1;

    }
    log.info("Read {} instances of training data for align tag", count);
    return instances;
  }

  private Pipe makePipe() {
    Alphabet alpha = new Alphabet();
    Target2LabelSequence labelPipe = new Target2LabelSequence();
    LabelAlphabet labelAlpha = (LabelAlphabet) labelPipe.getTargetAlphabet();

    return new SerialPipes(ImmutableList.of(
        new StringListToTokenSequence(alpha, labelAlpha),   // convert to token sequence
        new TokenSequenceLowercase(),                       // make all lowercase
        new NeighborTokenFeature(true, makeNeighbors()),         // grab neighboring graphemes
        new SurroundingTokenFeature(false),
        new SurroundingTokenFeature(true),
        new NeighborShapeFeature(true, makeShapeNeighs()),
        new LeadingTrailingFeature(),
        new TokenSequenceToFeature(),                       // convert the strings in the text to features
        new TokenSequence2FeatureVectorSequence(alpha, true, false),
        labelPipe
    ));
  }

  private static List<TokenWindow> makeShapeNeighs() {
    return ImmutableList.of(
        //        new TokenWindow(-5, 5),
        new TokenWindow(-4, 4),
        new TokenWindow(-3, 3),
        new TokenWindow(-2, 2),
        new TokenWindow(-1, 1),
        new TokenWindow(1, 1),
        new TokenWindow(1, 2),
        new TokenWindow(1, 3),
        new TokenWindow(1, 4)
        //        new TokenWindow(1, 5)
    );
  }

  private List<TokenWindow> makeNeighbors() {
    return ImmutableList.of(
        new TokenWindow(1, 1),
        new TokenWindow(1, 2),
        new TokenWindow(2, 1),
        new TokenWindow(1, 3),
        new TokenWindow(4, 1),
        new TokenWindow(-1, 1),
        new TokenWindow(-2, 2),
        new TokenWindow(-3, 3),
        new TokenWindow(-4, 1)
    );
  }
}
