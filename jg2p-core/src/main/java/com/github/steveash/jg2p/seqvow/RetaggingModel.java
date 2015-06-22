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
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.TagResult;
import com.github.steveash.jg2p.util.GramBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Model that takes graphones that represent a partial tagging of the graphones and returns a re-tagged version
 * with the final phones
 * @author Steve Ash
 */
public class RetaggingModel implements Serializable {

  private static final long serialVersionUID = -5475412828742332865L;

  private final CRF tduc;

  public RetaggingModel(CRF transducer) {
    this.tduc = transducer;
  }

  public List<TagResult> tag(List<String> graphemeGrams, List<String> phoneGrams, int nBest) {
    PartialTagging partial = PartialTagging.createFromGraphsAndOriginalPredictedPhoneGrams(graphemeGrams, phoneGrams);
    Instance instance = new Instance(partial, null, null, null);
    instance = tduc.getInputPipe().instanceFrom(instance);

    Sequence inSeq = (Sequence) instance.getData();
    List<Sequence<Object>> outSeqs = tduc.getMaxLatticeFactory().newMaxLattice(tduc, inSeq).bestOutputSequences(nBest);

    ArrayList<TagResult> results = Lists.newArrayListWithCapacity(outSeqs.size());
    double z = tduc.getSumLatticeFactory().newSumLattice(tduc, inSeq).getTotalWeight();
    for (Sequence<Object> outSeq : outSeqs) {
      double score = tduc.getSumLatticeFactory().newSumLattice(tduc, inSeq, outSeq).getTotalWeight();
      results.add(makeResult(outSeq, partial, score - z));
    }

    return results;
  }

  public CRF getCrf() {
    return this.tduc;
  }

  // needs the final phones, no eps given the vowseq output and the original partial phone output
  private TagResult makeResult(Sequence<Object> outSeq, PartialTagging input, double score) {

    List<String> phoneGrams = Lists.newArrayListWithCapacity(outSeq.size());
    List<String> phones = Lists.newArrayList();
    Preconditions.checkArgument(outSeq.size() == input.getPredictionIndexes().size());

    int outIndex = 0;
    for (int i = 0; i < input.getPartialPhoneGrams().size(); i++) {
      String graphone = input.getPartialPhoneGrams().get(i);
      if (input.getPredictionIndexes().contains(i)) {
        String tag = outSeq.get(outIndex).toString();
        outIndex += 1;
        graphone = PartialPhones.partialGramUpdatedWithPredictedPhoneGram(graphone, tag);
      }
      phoneGrams.add(graphone);
      // we have the graphone (maybe updated by the model) let's split it out to get the final phones
      if (!GramBuilder.isUnaryGram(graphone)) {
        for (String singlePhone : GramBuilder.SPLITTER.split(graphone)) {
          addIfPhone(phones, singlePhone);
        }
      } else {
        addIfPhone(phones, graphone);
      }
    }
    return new TagResult(phoneGrams, phones, score);
  }

  private void addIfPhone(List<String> phones, String predicted) {
    if (PhonemeCrfModel.isNotEps.apply(predicted) && isNotBlank(predicted)) {
      phones.add(predicted);
    }
  }

}
