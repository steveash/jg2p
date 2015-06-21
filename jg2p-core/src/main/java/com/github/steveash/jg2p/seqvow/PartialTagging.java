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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;

import java.util.List;

import javax.annotation.Nullable;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Input to the seq vow process
 * @author Steve Ash
 */
public class PartialTagging {

  public static PartialTagging createFromGraphsAndFinalPhoneGrams(List<String> graphemeGrams, List<String> finalPhoneGrams) {
    Preconditions.checkArgument(!PartialPhones.doesAnyGramContainPartialPhone(finalPhoneGrams));
    List<String> partialPhoneGrams = PartialPhones.phoneGramsToPartialPhoneGrams(finalPhoneGrams);
    PartialTagging tagging = new PartialTagging(graphemeGrams, partialPhoneGrams);
    tagging.setExpectedPhonesGrams(finalPhoneGrams);
    return tagging;
  }

  /**
   * This creates the PartialTagging from the input to the retagging process i.e. the phoneGrams should already contain
   * partialPhones (that were output from the previous stage)
   * @param graphs
   * @param partialPhoneGrams
   * @return
   */
  public static PartialTagging createFromGraphsAndPartialPhoneGrams(List<String> graphs, List<String> partialPhoneGrams) {
    return new PartialTagging(graphs, partialPhoneGrams);
  }

  // invariant is that these two are equal length
  private final List<String> graphemeGrams;
  private final List<String> partialPhoneGrams;
  @Nullable private List<String> originalPredictedGrams;
  // which indexes in the phones list need prediction (i.e. have a vowel)
  private final List<Integer> predictionIndexes;
  // null unless this is a training example for supervised learning; if != null then invariant is expectedPhonesGrams.size() == phones.size()
  @Nullable private List<String> expectedPhonesGrams;
  // map of phone index -> set of binary features
  private final SetMultimap<Integer,String> features = HashMultimap.create();

  private PartialTagging(List<String> graphemeGrams, List<String> partialPhoneGrams) {
    this.graphemeGrams = graphemeGrams;
    this.partialPhoneGrams = partialPhoneGrams;
    Preconditions.checkArgument(graphemeGrams.size() == partialPhoneGrams.size());

    ImmutableList.Builder<Integer> indexBuilder = ImmutableList.builder();
    for (int i = 0; i < partialPhoneGrams.size(); i++) {
      if (PartialPhones.doesGramContainPartial(partialPhoneGrams.get(i))) {
        indexBuilder.add(i);
      }
    }
    this.predictionIndexes = indexBuilder.build();
    Preconditions.checkArgument(predictionIndexes.size() > 0, "no vowels for retagging in ", this);
  }

  public int count() {
    return graphemeGrams.size();
  }

  public List<String> getGraphemeGrams() {
    return graphemeGrams;
  }

  public List<String> getPartialPhoneGrams() {
    return partialPhoneGrams;
  }

  public boolean hasExpectedPhones() {
    return expectedPhonesGrams != null;
  }

  @Nullable
  public List<String> getExpectedPhonesGrams() {
    return expectedPhonesGrams;
  }

  public void setExpectedPhonesGrams(List<String> expectedPhonesGrams) {
    Preconditions.checkNotNull(expectedPhonesGrams, "only call this if you want this to be a training example");
    this.expectedPhonesGrams = expectedPhonesGrams;
    Preconditions.checkArgument(expectedPhonesGrams.size() == partialPhoneGrams.size());
  }

  @Nullable
  public List<String> getOriginalPredictedGrams() {
    return originalPredictedGrams;
  }

  public void setOriginalPredictedGrams(List<String> originalPredictedGrams) {
    this.originalPredictedGrams = originalPredictedGrams;
    Preconditions.checkArgument(originalPredictedGrams.size() == this.partialPhoneGrams.size());
    Preconditions.checkArgument(originalPredictedGrams.size() == this.graphemeGrams.size());
  }

  public void addFeature(int phoneGramIndex, String feature) {
    features.put(phoneGramIndex, feature);
  }

  public SetMultimap<Integer, String> getFeatures() {
    return features;
  }

  public List<Integer> getPredictionIndexes() {
    return predictionIndexes;
  }

  /**
   * @return the list of tags that should come out of the CRF.  This is different from getExpectedPhonesGrams() because
   * that is the entire phoneGram sequence -- this is only the tags that we want to come out (i.e. one for
   * each of the indexes in predictedIndexes)
   */
  public List<String> getExpectedPredictedTags() {
    Preconditions.checkNotNull(expectedPhonesGrams);
    List<String> expected = Lists.newArrayListWithCapacity(predictionIndexes.size());
    for (Integer predictionIndex : predictionIndexes) {
      String expectedTag = PartialPhones.extractEligibleGramFromPhoneGram(expectedPhonesGrams.get(predictionIndex));
      Preconditions.checkArgument(isNotBlank(expectedTag));
      expected.add(expectedTag);
    }
    return expected;
  }

  @Override
  public String toString() {
    return "PartialTagging{" +
           "graphemeGrams=" + graphemeGrams +
           ", partialPhoneGrams=" + partialPhoneGrams +
           '}';
  }
}
