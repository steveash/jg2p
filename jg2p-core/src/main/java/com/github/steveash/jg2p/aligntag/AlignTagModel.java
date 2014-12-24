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

package com.github.steveash.jg2p.aligntag;

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

/**
 * Learns how to separate letters in to likely graphemes
 *
 * @author Steve Ash
 */
public class AlignTagModel implements Aligner, Serializable {

  private final CRF crf;

  public AlignTagModel(CRF crf) {
    this.crf = crf;
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
      List<Pair<String, String>> graphemes = makeGraphemes(x, outSeq);
      if (!resultsContain(graphemes, results)) {
        results.add(new Alignment(x, graphemes, score - z));
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

  private List<Pair<String, String>> makeGraphemes(Word x, Sequence<Object> outSeq) {
    List<String> letters = x.getValue();
    Preconditions.checkArgument(outSeq.size() == letters.size());
    ArrayList<Pair<String,String>> result = Lists.newArrayListWithCapacity(letters.size());

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < letters.size(); i++) {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append(letters.get(i));
      if (outSeq.get(i).equals("1")) {
        result.add(Pair.of(sb.toString(), ""));
        sb.delete(0, sb.length());
      }
    }
    if (sb.length() > 0) {
      result.add(Pair.of(sb.toString(), ""));
    }
    return result;
  }
}
