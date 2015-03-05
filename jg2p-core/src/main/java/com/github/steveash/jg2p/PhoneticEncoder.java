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

package com.github.steveash.jg2p;

import com.github.steveash.jg2p.util.Zipper;
import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import com.github.steveash.jg2p.align.Aligner;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Steve Ash
 */
public class PhoneticEncoder implements Serializable {
  private static final long serialVersionUID = 5996956897894317622L;

  public static final Joiner pipeJoiner = Joiner.on('|');
  public static final Joiner spaceJoiner = Joiner.on(' ');

  private final Aligner aligner;
  private final PhonemeCrfModel phoneTagger;
  private final int bestAlignments;
  private final double alignMinScore;
  private final double tagMinScore;

  public static class Encoding {
    public final List<String> alignment;
    public final List<String> phones;
    public final double alignScore;
    public final double tagScore;

    public Encoding(List<String> alignment, List<String> phones, double alignScore, double tagScore) {
      this.alignment = alignment;
      this.phones = phones;
      this.alignScore = alignScore;
      this.tagScore = tagScore;
    }

    public double tagProbability() {
      return Math.exp(tagScore);
    }

    @Override
    public String toString() {
      return pipeJoiner.join(alignment) + " -> " + spaceJoiner.join(phones);
    }
  }

  public PhoneticEncoder(Aligner aligner, PhonemeCrfModel phoneTagger, int bestAlignments, double alignMinScore,
                         double tagMinScore) {
    this.aligner = aligner;
    this.phoneTagger = phoneTagger;
    this.bestAlignments = bestAlignments;
    this.alignMinScore = alignMinScore;
    this.tagMinScore = tagMinScore;
  }

  public List<Encoding> encode(String word) {
    Word input = Word.fromNormalString(word);
    return encode(input);
  }

  public List<Encoding> encode(Word input) {
    List<Alignment> alignments = aligner.inferAlignments(input, bestAlignments);
    alignments.add(makeOneToOne(input));
    ArrayList<Encoding> results = Lists.newArrayListWithCapacity(alignments.size() + 1);
    for (Alignment alignment : alignments) {
      if (!results.isEmpty() && alignment.getScore() < alignMinScore) {
        continue;
      }

      List<String> graphemes = alignment.getAllXTokensAsList();
      List<PhonemeCrfModel.TagResult> tagResults = phoneTagger.tag(graphemes, bestAlignments);
      for (PhonemeCrfModel.TagResult tagResult : tagResults) {
        if (!results.isEmpty() && tagResult.sequenceLogProbability() < tagMinScore) {
          continue;
        }
        results.add(new Encoding(graphemes, tagResult.phonesNoEps(), alignment.getScore(), tagResult.sequenceLogProbability()));
      }
    }
    Collections.sort(results, OrderByTagScore);
    if (results.size() > bestAlignments) {
      return results.subList(0, bestAlignments);
    }
    return results;
  }

  private Alignment makeOneToOne(Word input) {
    return new Alignment(input, Zipper.upTo(input.getValue(), ""), 0);
  }

  public PhoneticEncoder withAligner(Aligner aligner) {
    return new PhoneticEncoder(aligner, this.phoneTagger, this.bestAlignments, this.alignMinScore, this.tagMinScore);
  }

  public Aligner getAligner() {
    return aligner;
  }

  public PhonemeCrfModel getPhoneTagger() {
    return phoneTagger;
  }

  private static final Ordering<Encoding> OrderByTagScore = new Ordering<Encoding>() {
    @Override
    public int compare(Encoding left, Encoding right) {
      return ComparisonChain.start()
          .compare(left.tagScore, right.tagScore)
          .compare(left.alignScore, right.alignScore)
          .result();
    }
  }.reverse();
}
