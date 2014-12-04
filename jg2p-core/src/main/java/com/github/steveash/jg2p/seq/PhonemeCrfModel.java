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

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;

/**
 * @author Steve Ash
 */
public class PhonemeCrfModel implements Serializable {

  public static class TagResult {
    public final List<String> phones;
    public final double logScore;

    public TagResult(List<String> phones, double logScore) {
      this.phones = phones;
      this.logScore = logScore;
    }

    public double sequenceProbability() {
      return Math.exp(logScore);
    }
  }

  private final CRF crf;

  public PhonemeCrfModel(CRF crf) {
    this.crf = crf;
  }

  public List<TagResult> tag(List<String> xTokens, int nBest) {
    Instance instance = new Instance(xTokens, null, null, null);
    instance = crf.getInputPipe().instanceFrom(instance);

    Sequence inSeq = (Sequence) instance.getData();
    List<Sequence<Object>> outSeqs = crf.getMaxLatticeFactory().newMaxLattice(crf, inSeq).bestOutputSequences(nBest);

    ArrayList<TagResult> results = Lists.newArrayListWithCapacity(outSeqs.size());
    double z = crf.getSumLatticeFactory().newSumLattice(crf, inSeq).getTotalWeight();
    for (Sequence<Object> outSeq : outSeqs) {
      double score = crf.getSumLatticeFactory().newSumLattice(crf, inSeq, outSeq).getTotalWeight();
      results.add(new TagResult(makePhones(outSeq), score - z));
    }

    return results;
  }

  private List<String> makePhones(Sequence<?> labels) {
    ArrayList<String> phones = Lists.newArrayListWithCapacity(labels.size());
    for (int i = 0; i < labels.size(); i++) {
      phones.add(labels.get(i).toString());
    }
    return phones;
  }

  public CRF getCrf() {
    return crf;
  }
}
