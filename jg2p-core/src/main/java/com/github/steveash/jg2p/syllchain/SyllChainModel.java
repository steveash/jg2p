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

package com.github.steveash.jg2p.syllchain;

import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.syll.SyllTagTrainer;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import cc.mallet.fst.CRF;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;

/**
 * @author Steve Ash
 */
public class SyllChainModel implements Serializable {

  private static final long serialVersionUID = -2809305117637940696L;
  private static final int MAX_SYLL_LEN = 6;

  private final CRF crf;

  public CRF getCrf() {
    return crf;
  }

  public SyllChainModel(CRF crf) {
    this.crf = crf;
  }

  public Alignment sylls(Alignment align) {
    Set<Integer> starts = tagSyllStarts(align.getWordUnigrams());
    return enrichWithSyllStarts(align, starts);
  }

  public Alignment enrichWithSyllStarts(Alignment align, Set<Integer> starts) {
    List<String> syllCodes = SyllTagTrainer.makeOncGramsForTesting(align, starts);
    return align.withGraphoneSyllGrams(syllCodes).withGraphemeSyllStarts(starts);
  }

  public Set<Integer> tagSyllStarts(List<String> wordUnigrams) {
    Instance instance = new Instance(wordUnigrams, null, null, null);
    instance = crf.getInputPipe().instanceFrom(instance);

    Sequence inSeq = (Sequence) instance.getData();
    Sequence<Object> outSeqs = crf.getMaxLatticeFactory().newMaxLattice(crf, inSeq).bestOutputSequence();
    return SyllTagTrainer.startsFromGraphemeSyllEnding(outSeqs);
//    return Sets.newHashSet(SWord.convertOncToBoundaries(outSeqs));
  }
}
