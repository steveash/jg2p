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

package com.github.steveash.jg2p.syll;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.Aligner;
import com.github.steveash.jg2p.align.Alignment;

import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;

import static com.github.steveash.jg2p.syll.SyllTagTrainer.AlignBegin;
import static com.github.steveash.jg2p.syll.SyllTagTrainer.onlySyllStructreFromTag;

/**
 * Learns how to separate letters in to likely graphemes
 *
 * @author Steve Ash
 */
public class SyllTagModel implements Aligner, Serializable {

  private static final long serialVersionUID = 2315790267922134027L;

  private final CRF crf;

  public SyllTagModel(CRF crf) {
    this.crf = crf;
  }

  public CRF getCrf() {
    return crf;
  }

  @Override
  public List<Alignment> inferAlignments(Word x, int nBest) {
    Instance instance = new Instance(x.getValue(), null, null, null);
    instance = crf.getInputPipe().instanceFrom(instance);

    Sequence inSeq = (Sequence) instance.getData();
    List<Sequence<Object>> outSeqs = crf.getMaxLatticeFactory().newMaxLattice(crf, inSeq).bestOutputSequences(nBest);

    ArrayList<Alignment> results = Lists.newArrayListWithCapacity(outSeqs.size());
    double z = crf.getSumLatticeFactory().newSumLattice(crf, inSeq).getTotalWeight();
    for (Sequence<Object> outSeq : outSeqs) {
      double score = crf.getSumLatticeFactory().newSumLattice(crf, inSeq, outSeq).getTotalWeight();
      Alignment align = makeAlignment(x, outSeq, score - z);
      if (!resultsContain(align.getGraphones(), results)) {
        results.add(align);
      }
    }
    return results;
  }

  private boolean resultsContain(List<Pair<String, String>> candidate, ArrayList<Alignment> existing) {
    for (Alignment alignment : existing) {
      if (alignment.getGraphones().equals(candidate)) {
        return true;
      }
    }
    return false;
  }

  private Alignment makeAlignment(Word x, Sequence<Object> outSeq, double normalScore) {
    List<String> letters = x.getValue();
    Preconditions.checkArgument(outSeq.size() == letters.size());
    ArrayList<Pair<String,String>> result = Lists.newArrayListWithCapacity(letters.size());
    List<String> syllGrams = Lists.newArrayListWithCapacity(letters.size());

    StringBuilder sb = new StringBuilder();
    StringBuilder sg = new StringBuilder();
    for (int i = 0; i < letters.size(); i++) {
      if (sb.length() > 0) {
        sb.append(' ');
        sg.append(' ');
      }
      sb.append(letters.get(i));
      sg.append(onlySyllStructreFromTag((String) outSeq.get(i)));
      if ((i + 1) < outSeq.size() && SyllTagTrainer.isAlignBegin((String) outSeq.get(i + 1))) {
        result.add(Pair.of(sb.toString(), ""));
        syllGrams.add(sg.toString());
        sb.delete(0, sb.length());
        sg.delete(0, sg.length());
      }
    }
    if (sb.length() > 0) {
      result.add(Pair.of(sb.toString(), ""));
      syllGrams.add(sg.toString());
    }
    return new Alignment(x, result, normalScore, syllGrams, null);
  }
}
