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

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.fst.Transducer;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;

/**
 * @author Steve Ash
 */
public class PhonemeCrfModel implements Serializable {
  private static final long serialVersionUID = 1888858574145460221L;

  private static final Splitter split =com.google.common.base.Splitter.on(' ');

  public static class TagResult {
    private final List<String> phones;
    private final double logScore;

    public TagResult(List<String> phones, double logScore) {
      this.phones = phones;
      this.logScore = logScore;
    }

    public double sequenceLogProbability() { return logScore; }
    public double sequenceProbability() {
      return Math.exp(logScore);
    }

    public List<String> phonesNoEps() {
      return Lists.newArrayList(Iterables.filter(phones, isNotEps));
    }

    public boolean isEqualTo(Iterable<String> expected) {
      Iterator<String> iter = expected.iterator();
      for (int i = 0; i < phones.size(); i++) {
        if (!iter.hasNext()) return false;
        String next = iter.next();
        if (!phones.get(i).equals(next)) return false;
      }
      // got through all of the phones, make sure that the iterator is empty too
      return !iter.hasNext();
    }
  }

  private final Transducer tduc;

  public PhonemeCrfModel(Transducer tduc) {
    this.tduc = tduc;
  }

  public List<TagResult> tag(List<String> xTokens, int nBest) {
    Instance instance = new Instance(xTokens, null, null, null);
    instance = tduc.getInputPipe().instanceFrom(instance);

    Sequence inSeq = (Sequence) instance.getData();
    List<Sequence<Object>> outSeqs = tduc.getMaxLatticeFactory().newMaxLattice(tduc, inSeq).bestOutputSequences(nBest);

    ArrayList<TagResult> results = Lists.newArrayListWithCapacity(outSeqs.size());
    double z = tduc.getSumLatticeFactory().newSumLattice(tduc, inSeq).getTotalWeight();
    for (Sequence<Object> outSeq : outSeqs) {
      double score = tduc.getSumLatticeFactory().newSumLattice(tduc, inSeq, outSeq).getTotalWeight();
      results.add(new TagResult(makePhones(outSeq), score - z));
    }

    return results;
  }

  private List<String> makePhones(Sequence<?> labels) {
    ArrayList<String> phones = Lists.newArrayListWithCapacity(labels.size());
    for (int i = 0; i < labels.size(); i++) {
      // if our CRF can predict two phonemes at once then we need to unpack them here
      String predicted = labels.get(i).toString();
      if (predicted.contains(" ")) {
        for (String singlePhone : split.split(predicted)) {
          phones.add(singlePhone);
        }
      } else {
        phones.add(predicted);
      }
    }
    return phones;
  }

  public CRF getCrf() {
    return (CRF) tduc;
  }

  private static final Predicate<String> isNotEps = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return !input.equalsIgnoreCase(PhonemeCrfTrainer.EPS);
    }
  };
}
