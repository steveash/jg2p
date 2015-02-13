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

package com.github.steveash.jg2p.train;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.PhoneticEncoderFactory;
import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Aligner;
import com.github.steveash.jg2p.align.AlignerTrainer;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.align.ProbTable;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.aligntag.AlignTagTrainer;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer;
import com.github.steveash.jg2p.util.Zipper;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Trainer that uses a multi-stage iterative training procedure where alignments are used to train a CRF that then
 * provides positive examples back to re-train the alignment model.  The alignment model treats the feedback that it
 * gets from the later stage as semi-supervised input and updates the M step of its EM algorithm to take it in to
 * account
 *
 * @author Steve Ash
 */
public class JointEncoderTrainer extends AbstractEncoderTrainer {

  private static final Logger log = LoggerFactory.getLogger(JointEncoderTrainer.class);
  private static final int MAX_JOINT_ITER = 2;

  private File startingPointCrf = null;

  @Override
  protected PhoneticEncoder train(List<InputRecord> inputs, TrainOptions opts) {

    AlignerTrainer alignTrainer = new AlignerTrainer(opts);
    AlignTagTrainer alignTagTrainer = new AlignTagTrainer();

    // first model
    AlignModel model = alignTrainer.train(inputs);
    Collection<Alignment> crfExamples = makeCrfExamples(inputs, model);
    Set<Alignment> goodExamples = Sets.newHashSet();
    PhonemeCrfModel crfModel;
    Aligner alignTagModel = model;

    PhonemeCrfTrainer crfTrainer = PhonemeCrfTrainer.open(opts);
    crfTrainer.trainFor(crfExamples);

    int iterCount = 0;
    int previousGoodAligns = 0;
    int goodAlignCount = 0;

    while (true) {
      if (iterCount > 0) {
        previousGoodAligns = goodAlignCount;
      }
      crfModel = crfTrainer.buildModel();

      ProbTable goodAligns = new ProbTable();
      goodAlignCount = collectGoodAligns(crfExamples, crfModel, goodAligns, goodExamples);
      log.info("Trained CRF had " + goodAlignCount + " good aligns this time (last time " + previousGoodAligns + ")");

      eval(PhoneticEncoderFactory.make(alignTagModel, crfModel), "PHASE" + iterCount, EncoderEval.PrintOpts.SIMPLE);

      if (goodAlignCount == previousGoodAligns || iterCount++ >= MAX_JOINT_ITER) {
        log.info("Stopping iteration on finding good aligns, previous model will stand");
        break;
      }

      alignTagModel = alignTagTrainer.train(goodExamples, true);
      crfExamples = makeCrfExamplesFromAlignTag(inputs, goodExamples, alignTagModel, model);
      crfTrainer.trainFor(crfExamples);
    }

    PhoneticEncoder encoder = PhoneticEncoderFactory.make(alignTagModel, crfModel);

    return encoder;
  }

  private Collection<Alignment> makeCrfExamplesFromAlignTag(List<InputRecord> inputs, Set<Alignment> goodExamples,
                                                            Aligner crfAligner, AlignModel mlAligner) {
    // this is a little complicated; we want to use the goodExamples (supervised aligns) if we have it; next use the
    // crfAligner if it produces a good alignment that matches the Y count; else use the mlAligner
    Set<Alignment> result = Sets.newHashSetWithExpectedSize(inputs.size());
    List<InputRecord> failedInputs = Lists.newArrayList();
    Map<Pair<Word, Word>, Alignment> supervisedAligns = makeSupervisedAlignsMap(goodExamples);

    int superHit = 0;
    int xyEqualCount = 0;
    int crfAlignHit = 0;
    int mlAlignHit = 0;

    for (InputRecord input : inputs) {
      Alignment maybeSuper = supervisedAligns.get(input.xyWordPair());
      if (maybeSuper != null) {
        superHit += 1;
        result.add(maybeSuper);
        continue;
      }

      if (input.xWord.unigramCount() == input.yWord.unigramCount()) {
        // no alignment necessary its already aligned
        result.add(new Alignment(input.xWord, Zipper.up(input.xWord, input.yWord), 0.0));
        xyEqualCount += 1;
        continue;
      }

      Alignment maybeCrf = makeCrfAlign(crfAligner, input);
      if (maybeCrf != null) {
        crfAlignHit += 1;
        result.add(maybeCrf);
        continue;
      }

      mlAlignHit += 1;
      failedInputs.add(input);
    }

    List<Alignment> mlAligns = makeCrfExamples(failedInputs, mlAligner);
    result.addAll(mlAligns);

    log.info("Super/Eq/Crf/ML " + superHit + "/" + xyEqualCount + "/" + crfAlignHit + "/" + mlAlignHit + " ML added count " + mlAligns.size());
    return result;
  }

  private Alignment makeCrfAlign(Aligner crfAligner, InputRecord input) {
    List<Alignment> best = crfAligner.inferAlignments(input.xWord, 1);
    if (best.isEmpty()) {
      return null;
    }
    Alignment result = best.get(0);

    if (result.getScore() <= -50) {
      return null;
    }
    if (result.getGraphones().size() != input.yWord.unigramCount()) {
      return null;
    }
    return result.withReplacedYs(input.yWord.getValue());
  }

  private Map<Pair<Word, Word>, Alignment> makeSupervisedAlignsMap(Set<Alignment> goodExamples) {
    HashMap<Pair<Word, Word>, Alignment> map = Maps.newHashMap();
    for (Alignment example : goodExamples) {
      map.put(example.xyWordPair(), example);
    }
    return map;
  }

  private int collectGoodAligns(Collection<Alignment> crfExamples, PhonemeCrfModel crfModel, ProbTable goodAligns,
                                Set<Alignment> goodExamples) {
    int goodAlignCount = 0;
    for (Alignment crfExample : crfExamples) {
      List<PhonemeCrfModel.TagResult> predicts = crfModel.tag(crfExample.getAllXTokensAsList(), 1);
      if (predicts.size() > 0) {
        if (predicts.get(0).isEqualTo(crfExample.getYTokens())) {
          // good example, let's increment all of its transitions
          for (Pair<String, String> graphone : crfExample) {
            goodAligns.addProb(graphone.getLeft(), graphone.getRight(), 1.0);
          }
          goodAlignCount += 1;
          goodExamples.add(crfExample);
        }
      }
    }
    return goodAlignCount;
  }
}
