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

package com.github.steveash.jg2p.rerank;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEnt;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelVector;
import cc.mallet.types.Labeling;

/**
 * @author Steve Ash
 */
public class Rerank2Model implements Reranker, Serializable {
  private static final long serialVersionUID = 2016299559324603971L;

  public static final ImmutableList<String> goodShapes =
      ImmutableList.of("CCvC", "CCv", "CC", "vCCv", "v", "vC", "vCC", "vCCC", "vCvC", "vv", "vCv", "CCC", "CCCv");
  public static final ImmutableList<String> scoreHeaders;
  public static final ImmutableList<String> csvHeaders;
  public static final ImmutableList<String> featureHeaders;
  public static final int minGoodShape;
  public static final int maxGoodShape;

  static {
    List<String> distinct = Lists.newArrayList("lmScore", "tagScore", "alignScore", "uniqueMode", "dups",
                                               "alignIndex", "overallIndex", "shapeEdit", "shapeLenDiff",
                                               "leadingConsMatch", "leadingConsMismatch");
    distinct.addAll(goodShapes);
    scoreHeaders = ImmutableList.copyOf(distinct);

    ImmutableList.Builder<String> csv = ImmutableList.builder();
    ImmutableList.Builder<String> feature = ImmutableList.builder();
    csv.add("seq","word","phones","label","A","B");
    for (String col : distinct) {
      String a = "A_" + col;
      String b = "B_" + col;
      csv.add(a, b);
      feature.add(a, b);
    }
    csvHeaders = csv.build();
    featureHeaders = feature.build();

    int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
    for (String goodShape : goodShapes) {
      min = Math.min(min, goodShape.length());
      max = Math.max(max, goodShape.length());
    }
    minGoodShape = min;
    maxGoodShape = max;
  }

  private final Classifier model;

  public Rerank2Model(Classifier model) {
    this.model = model;
  }

  public Map<String, Double> probabilities(RerankExample ex) {

    Classification classify = model.classify(ex);
    Labeling labeling = classify.getLabeling();
    HashMap<String, Double> result = Maps.newHashMapWithExpectedSize(labeling.numLocations());
    for (int i = 0; i < labeling.numLocations(); i++) {
      String label = (String) labeling.labelAtLocation(i).getEntry();
      double prob = labeling.valueAtLocation(i);
      Preconditions.checkArgument(prob >= 0 && prob <= 1, "didnt get a real prob", prob);
      result.put(label, prob);
    }
    return result;
  }

  @Override
  public Map<String, Double> probabilities(Map<String, Object> values) {
    throw new IllegalStateException("not implemented");
  }
}
