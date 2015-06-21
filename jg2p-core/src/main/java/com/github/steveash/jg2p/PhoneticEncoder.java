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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Aligner;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.TagResult;
import com.github.steveash.jg2p.seqvow.PartialPhones;
import com.github.steveash.jg2p.seqvow.RetaggingModel;
import com.github.steveash.jg2p.util.Zipper;

import net.sf.jsefa.csv.annotation.CsvDataType;
import net.sf.jsefa.csv.annotation.CsvField;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Steve Ash
 */
public class PhoneticEncoder implements Serializable {

  private static final long serialVersionUID = 5996956897894317622L;

  public static final Joiner pipeJoiner = Joiner.on('|');
  public static final Joiner spaceJoiner = Joiner.on(' ');

  private final Aligner aligner;
  private final PhonemeCrfModel phoneTagger;
  private int bestAlignments;
  private int bestTaggings;
  private double alignMinScore;
  private double tagMinScore;
  private Integer bestFinal;
  private boolean includeOneToOne = true;
  private RetaggingModel retagger = null;
  private AlignModel alignModel = null;

  @CsvDataType
  public static class Encoding {

    @CsvField(pos = 1)
    public List<String> alignment;
    @CsvField(pos = 2)
    public List<String> phones;
    @CsvField(pos = 3)
    public double alignScore;
    @CsvField(pos = 4)
    public double tagScore;
    @CsvField(pos = 5)
    public double retagScore;
    @CsvField(pos = 6)
    public boolean isPostProcessed;
    @CsvField(pos = 7)
    public int rank;      // what order overall was this coming out of the encoder
    @CsvField(pos = 8)
    public int alignRank; // what order did this come out for the particular align group
    @CsvField(pos = 9)
    public List<String> graphones;
        // the exact phones that came out of the CRF.  This is 1-1 with alignment i.e. alignment - split phones make
        // up the graphones of the word

    public Encoding() {
      // no arg constructor for the CSV serialization library
    }

    private Encoding(List<String> alignment, List<String> phones, List<String> graphones, double alignScore,
                     double tagScore, double retagScore) {

      this.alignment = alignment;
      this.phones = phones;
      this.graphones = graphones;
      this.alignScore = alignScore;
      this.tagScore = tagScore;
      this.retagScore = retagScore;
    }

    public static Encoding createEncoding(List<String> alignment, List<String> phones, List<String> graphones,
                                          double alignScore, double tagScore, double retagScore) {

      Encoding encoding = new Encoding(alignment, phones, graphones, alignScore, tagScore, retagScore);
      return encoding;
    }

    public double tagProbability() {
      return Math.exp(tagScore);
    }

    public double retagProbability() {
      if (retagScore == 0) return 0;
      return Math.exp(retagScore);
    }

    public Encoding withReplacedPhoneme(int index, String newPhoneme) {
      ArrayList<String> newPhones = Lists.newArrayList(this.phones);
      newPhones.set(index, newPhoneme);
      Encoding result = createEncoding(this.alignment, newPhones, this.graphones, alignScore, tagScore, retagScore);
      result.isPostProcessed = true;
      result.rank = this.rank;
      result.alignRank = this.alignRank;
      return result;
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (alignment != null) {
        sb.append(pipeJoiner.join(alignment));
      } else {
        sb.append("<null>");
      }
      sb.append(" -> ");
      if (phones != null) {
        sb.append(spaceJoiner.join(phones));
      } else {
        sb.append("<null>");
      }
      return sb.toString();
    }
  }

  public PhoneticEncoder(Aligner aligner, PhonemeCrfModel phoneTagger, int bestAlignments, double alignMinScore,
                         double tagMinScore) {
    this.aligner = aligner;
    this.phoneTagger = phoneTagger;
    this.bestAlignments = bestAlignments;
    this.bestTaggings = bestAlignments;
    this.alignMinScore = alignMinScore;
    this.tagMinScore = tagMinScore;
  }

  public List<Encoding> encode(String word) {
    Word input = Word.fromNormalString(word);
    return encode(input);
  }

  public Result complexEncode(Word input) {
    Result result = new Result();
    List<Alignment> alignments = aligner.inferAlignments(input, bestAlignments);
    if (includeOneToOne) {
      alignments.add(makeOneToOne(input));
    }
    Set<Alignment> deduped = Sets.newHashSet(alignments);
    List<Encoding> results = Lists.newArrayListWithCapacity(alignments.size() + 1);
    for (Alignment alignment : deduped) {
      AlignResult ar = new AlignResult(alignment);
      result.alignResults.add(ar);
      List<String> graphemes = alignment.getAllXTokensAsList();
      List<TagResult> tagResults = phoneTagger.tag(graphemes, bestTaggings);
      for (TagResult tagResult : tagResults) {
        if (!results.isEmpty() && tagResult.sequenceLogProbability() < tagMinScore) {
          continue;
        }
        if (retagger != null) {
          tagResult = retag(graphemes, tagResult);
        }
        Encoding e = Encoding.createEncoding(graphemes, tagResult.phones(), tagResult.phoneGrams(), alignment.getScore(),
                                             tagResult.sequenceLogProbability(), tagResult.getLogScore2());
        results.add(e);
        ar.encodings.add(e);
      }
      Collections.sort(ar.encodings, OrderByTagScore);
      // set the align ranks
      for (int i = 0; i < ar.encodings.size(); i++) {
        ar.encodings.get(i).alignRank = i;
      }
    }
    Collections.sort(results, OrderByTagScore);
    int finalCount = (bestFinal != null ? bestFinal : bestAlignments);
    if (results.size() > finalCount) {
      results = results.subList(0, finalCount);
    }
    // set the overall ranks
    for (int i = 0; i < results.size(); i++) {
      results.get(i).rank = i;
    }
    result.overallResults.addAll(results);
    return result;
  }

  private TagResult retag(List<String> graphemes, TagResult tagResult) {
    // in this version we only re-assign vowels that were predicted
    Preconditions.checkArgument(!PartialPhones.doesAnyGramContainPartialPhone(tagResult.phoneGrams()));
    if (!PartialPhones.doesAnyGramContainPhoneEligibleAsPartial(tagResult.phoneGrams())) {
      return tagResult;
    }
    List<String> partialPhoneGrams = PartialPhones.phoneGramsToPartialPhoneGrams(tagResult.phoneGrams());
    List<TagResult> retagged = retagger.tag(graphemes, partialPhoneGrams, 1);
    if (retagged.isEmpty()) {
      throw new IllegalArgumentException("cant retag " + graphemes + " -> " + tagResult);
    }
    TagResult retaggedResult = retagged.get(0);
    TagResult updatedResult = new TagResult(retaggedResult.phoneGrams(), retaggedResult.phones(), tagResult.sequenceLogProbability());
    updatedResult.setLogScore2(retaggedResult.sequenceLogProbability());
    return updatedResult;
  }

  public List<Encoding> encode(Word input) {
    Result result = complexEncode(input);
    return result.overallResults;
  }

  private Alignment makeOneToOne(Word input) {
    return new Alignment(input, Zipper.upTo(input.getValue(), ""), 0);
  }

  public PhoneticEncoder withAligner(Aligner aligner) {
    return new PhoneticEncoder(aligner, this.phoneTagger, this.bestAlignments, this.alignMinScore, this.tagMinScore);
  }

  public int getBestTaggings() {
    return bestTaggings;
  }

  public void setBestTaggings(int bestTaggings) {
    this.bestTaggings = bestTaggings;
  }

  public int getBestAlignments() {
    return bestAlignments;
  }

  public void setBestAlignments(int bestAlignments) {
    this.bestAlignments = bestAlignments;
  }

  public Integer getBestFinal() {
    return bestFinal;
  }

  public void setBestFinal(Integer bestFinal) {
    this.bestFinal = bestFinal;
  }

  public boolean isIncludeOneToOne() {
    return includeOneToOne;
  }

  public void setIncludeOneToOne(boolean includeOneToOne) {
    this.includeOneToOne = includeOneToOne;
  }

  public double getAlignMinScore() {
    return alignMinScore;
  }

  public void setAlignMinScore(double alignMinScore) {
    this.alignMinScore = alignMinScore;
  }

  public double getTagMinScore() {
    return tagMinScore;
  }

  public void setTagMinScore(double tagMinScore) {
    this.tagMinScore = tagMinScore;
  }



  public Aligner getAligner() {
    return aligner;
  }

  public PhonemeCrfModel getPhoneTagger() {
    return phoneTagger;
  }

  public RetaggingModel getRetagger() {
    return retagger;
  }

  public void setRetagger(RetaggingModel retagger) {
    this.retagger = retagger;
  }

  public AlignModel getAlignModel() {
    return alignModel;
  }

  public void setAlignModel(AlignModel alignModel) {
    this.alignModel = alignModel;
  }

  public static final Ordering<Encoding> OrderByTagScore = new Ordering<Encoding>() {
    @Override
    public int compare(Encoding left, Encoding right) {
      return ComparisonChain.start()
          .compare(left.tagScore, right.tagScore)
          .compare(left.alignScore, right.alignScore)
          .compare(left.isPostProcessed, right.isPostProcessed)
          .result();
    }
  }.reverse();

  private Object readResolve() throws ObjectStreamException {
    if (bestTaggings == 0) {
      bestTaggings = bestAlignments;
    }
    return this;
  }

  public static class Result {

    public final List<AlignResult> alignResults = Lists.newArrayList();
    public final List<Encoding> overallResults = Lists.newArrayList();
  }

  public static class AlignResult {

    public final Alignment alignment;
    public final List<Encoding> encodings = Lists.newArrayList();

    public AlignResult(Alignment alignment) {
      this.alignment = alignment;
    }

    public int rankOfMatchingPhones(List<String> phones) {
      for (int i = 0; i < encodings.size(); i++) {
        if (encodings.get(i).phones.equals(phones)) {
          return i;
        }
      }
      return -1;
    }
  }
}
