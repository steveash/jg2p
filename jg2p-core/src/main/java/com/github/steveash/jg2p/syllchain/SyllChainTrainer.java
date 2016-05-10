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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.syll.SyllTagTrainer;
import com.github.steveash.kylm.model.immutable.ImmutableLM;
import com.github.steveash.kylm.model.immutable.ImmutableLMConverter;
import com.github.steveash.kylm.model.ngram.NgramLM;
import com.github.steveash.kylm.model.ngram.smoother.KNSmoother;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static javax.swing.UIManager.get;

/**
 * @author Steve Ash
 */
public class SyllChainTrainer {

  private static final Logger log = LoggerFactory.getLogger(SyllChainTrainer.class);
  public static final String SYLL_END = "$";

  private final AlignModel aligner;

  public SyllChainTrainer(AlignModel aligner) {
    this.aligner = aligner;
  }

  public SyllChainModel train(List<InputRecord> words) {
    log.info("About to train the syll chain...");
    KNSmoother smoother = new KNSmoother();
    smoother.setSmoothUnigrams(true);
    NgramLM lm = new NgramLM(8, smoother);
    final long[] box = new long[1];
    FluentIterable<String[]> sylls =
        FluentIterable.from(words).transformAndConcat(new Function<InputRecord, Iterable<String>>() {
          @Override
          public Iterable<String> apply(InputRecord input) {
            List<Alignment> aligned = aligner.align(input.getLeft(), input.getRight(), 1);
            if (aligned.isEmpty()) {
              return ImmutableList.of();
            }
            Alignment alignment = aligned.get(0);
            return SyllTagTrainer.makeSyllablesFor(alignment);
          }
        }).transform(new Function<String, String[]>() {
          @Override
          public String[] apply(String input) {
            Preconditions.checkState(input.length() > 0);
            box[0] = Math.max(box[0], input.length());
            String[] out = new String[input.length()];
            for (int i = 0; i < input.length(); i++) {
              out[i] = String.valueOf(input.charAt(i));
            }
            return out;
          }
        });
    for (int i = 0; i < 10; i++) {
      log.info("Syll sample " + i + " = " + Arrays.toString(sylls.get(i)));
    }
    lm.trainModel(sylls);
    log.info("Trained the syll chain model, the longest syllable that we observed is " + box[0]);
    ImmutableLM newlm = new ImmutableLMConverter().convert(lm);
    return new SyllChainModel(newlm);
  }
}
