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

package com.github.steveash.jg2p.seqbin;

import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.seq.LeadingTrailingFeature;
import com.github.steveash.jg2p.seq.NeighborShapeFeature;
import com.github.steveash.jg2p.seq.NeighborTokenFeature;
import com.github.steveash.jg2p.seq.StringListToTokenSequence;
import com.github.steveash.jg2p.seq.SurroundingTokenFeature;
import com.github.steveash.jg2p.seq.TokenSequenceToFeature;
import com.github.steveash.jg2p.seq.TokenWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.ConfusionMatrix;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Trains a max ent model to predict which cluster to assign inputs to
 * @author Steve Ash
 */
public class SeqBinTrainer {

  private static final Logger log = LoggerFactory.getLogger(SeqBinTrainer.class);
  private Pipe pipe;

  public SeqBinTrainer() {
    this.pipe = makePipe();
  }

  public MaxEnt trainFor(Collection<InputRecord> recs) {
    InstanceList instances = convertToInstances(recs);
    MaxEntTrainer trainer = new MaxEntTrainer();

    MaxEnt model = trainer.train(instances);
    Trial trial = new Trial(model, instances);
    log.info("Trained seq bin. Final accuracy on itself: " + trial.getAccuracy());
    log.info(new ConfusionMatrix(trial).toString());
    return model;
  }

  private InstanceList convertToInstances(Collection<InputRecord> recs) {
    InstanceList instances = new InstanceList(pipe);
    int count = 0;
    for (InputRecord rec : recs) {
      if (isBlank(rec.memo)) {
        throw new IllegalArgumentException("Cannot bin an input record with no Good/Bad tag: " + rec.getLeft());
      }
      Instance instance = new Instance(rec.getLeft().getValue(), rec.memo, null, null);
      instances.addThruPipe(instance);
      count += 1;
    }

    log.info("Read {} instances", count);
    return instances;
  }

  private static Pipe makePipe() {
    Alphabet alpha = new Alphabet();
    Target2Label labelPipe = new Target2Label();
    LabelAlphabet labelAlpha = (LabelAlphabet) labelPipe.getTargetAlphabet();

    return new SerialPipes(ImmutableList.of(
        new StringListToTokenSequence(alpha, labelAlpha, false),   // convert to token sequence
        new TokenSequenceLowercase(),                       // make all lowercase
        new NeighborTokenFeature(true, makeNeighbors()),         // grab neighboring graphemes
        new NeighborShapeFeature(true, makeShapeNeighs()),
        new SurroundingTokenFeature(false),
        new SurroundingTokenFeature(true),
        new LeadingTrailingFeature(),
        new TokenSequenceToFeature(),                       // convert the strings in the text to features
        new TokenSequence2FeatureVector(alpha),
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
