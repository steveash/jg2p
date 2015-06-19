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
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.util.GramBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.fst.Transducer;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @author Steve Ash
 */
public class PhonemeCrfModel implements Serializable {
  //  private static final long serialVersionUID = -6696520265858560431L;
  private static final long serialVersionUID = 1888858574145460221L;

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
      results.add(makeTagResult(outSeq, score - z));
    }

    return results;
  }

  private TagResult makeTagResult(Sequence<?> labels, double logScore) {
    ArrayList<String> phones = Lists.newArrayListWithExpectedSize(labels.size());
    ArrayList<String> graphones = Lists.newArrayListWithCapacity(labels.size());
    for (int i = 0; i < labels.size(); i++) {
      // if our CRF can predict two phonemes at once then we need to unpack them here
      String predicted = labels.get(i).toString();
      if (predicted.contains(" ")) {
        for (String singlePhone : GramBuilder.SPLITTER.split(predicted)) {
          addIfPhone(phones, singlePhone);
        }
      } else {
        addIfPhone(phones, predicted);
      }
      graphones.add(predicted);
    }
    return new TagResult(graphones, phones, logScore);
  }

  private void addIfPhone(ArrayList<String> phones, String predicted) {
    if (isNotEps.apply(predicted) && isNotBlank(predicted)) {
      phones.add(predicted);
    }
  }

  public CRF getCrf() {
    return (CRF) tduc;
  }

  public static final Predicate<String> isNotEps = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return !input.equalsIgnoreCase(GramBuilder.EPS);
    }
  };
}
