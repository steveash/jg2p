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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.align.Alignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import cc.mallet.grmm.examples.CrossTemplate1;
import cc.mallet.grmm.learning.ACRF;
import cc.mallet.grmm.learning.ACRFTrainer;
import cc.mallet.grmm.learning.DefaultAcrfTrainer;
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
 * General CRF version which uses indicator functions as latent variables to see if that increases accuracy
 * @author Steve Ash
 */
public class PhonemeACrfTrainer2 {

  private static final Logger log = LoggerFactory.getLogger(PhonemeACrfTrainer2.class);

  public void train(Collection<Alignment> examples) {
    Pipe pipe = makePipe();
    InstanceList instances = makeExamplesFromAligns(examples, pipe);

    ACRF.Template[] tmpls = new ACRF.Template[]{
        new ACRF.BigramTemplate(0),
                new ACRF.BigramTemplate (1),
                new ACRF.PairwiseFactorTemplate (0,1),
                new CrossTemplate1(0,1)
    };

    ACRF acrf = new ACRF(pipe, tmpls);

    ACRFTrainer trainer = new DefaultAcrfTrainer();
    acrf.setSupportedOnly(true);
    acrf.setGaussianPriorVariance(2.0);
    DefaultAcrfTrainer.LogEvaluator eval = new DefaultAcrfTrainer.LogEvaluator();
    eval.setNumIterToSkip(2);
    trainer.train(acrf, instances, null, null, eval, 9999);

  }

  private static InstanceList makeExamplesFromAligns(Iterable<Alignment> alignsToTrain, Pipe pipe) {
    int count = 0;
    InstanceList instances = new InstanceList(pipe);
    for (Alignment align : alignsToTrain) {
      List<String> xs = align.getWordUnigrams();
      List<Boolean> bs = align.getXBoundaryMarks();
      Iterator<String> ys = align.getAllYTokensAsList().iterator();

      Preconditions.checkState(xs.size() == bs.size());
      List<String[]> targets = Lists.newArrayListWithCapacity(xs.size());

      for (int i = 0; i < xs.size(); i++) {
        targets.add(new String[] {
            bs.get(i) ? "1" : "0",
            bs.get(i) ? ys.next() : "<>"
        });
      }
      Instance ii = new Instance(xs, targets, null, null);
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

  private static Pipe makePipe() {
    Alphabet alpha = new Alphabet();
    JointInputToTokenSequence inputPipe = new JointInputToTokenSequence(alpha, new LabelAlphabet(), new LabelAlphabet());

    return new SerialPipes(ImmutableList.of(
        inputPipe,
        new TokenSequenceLowercase(),                       // make all lowercase
        new NeighborTokenFeature(true, makeNeighbors()),         // grab neighboring graphemes
        new NeighborShapeFeature(true, makeShapeNeighs()),
        new TokenSequenceToFeature(),                       // convert the strings in the text to features
        new TokenSequence2FeatureVectorSequence(alpha, true, true)
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
        //        new TokenWindow(1, 2),
        //              new TokenWindow(1, 3),
        new TokenWindow(-1, 1),
        new TokenWindow(-2, 1),
        new TokenWindow(-3, 1),
        new TokenWindow(-2, 2)
        //        new TokenWindow(-3, 3)
        //        new TokenWindow(-4, 4),
    );
  }
}
