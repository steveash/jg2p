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

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.phoseq.Phonemes;

import java.io.Serializable;
import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;

import static com.github.steveash.jg2p.syll.SyllCounter.countSyllablesInSequence;

/**
 * this model knows how to take a sequence of phones and predict where syllable boundaries are
 *
 * @author Steve Ash
 */
public class PhoneSyllTagModel implements Serializable {

  private static final long serialVersionUID = -5644996659039364023L;

  private final CRF crf;

  public PhoneSyllTagModel(CRF crf) {
    this.crf = crf;
  }

  public CRF getCrf() {
    return crf;
  }

  public List<Integer> syllStarts(Word word) {
    return syllStarts(word, -1);
  }

  public List<Integer> syllStarts(Word word, int expectedSylls) {
    Sequence<String> seq = getCoding(word, expectedSylls);
    if (PhoneSyllTagTrainer.USE_ONC_CODING) {
      return SWord.convertOncToBoundaries(seq);
    } else {
      return SWord.convertEndMarkersToBoundaries(seq);
    }
  }

  private Sequence<String> getCoding(Word word, int expectedSylls) {
    Instance instance = new Instance(word, null, null, null);
    instance = crf.getInputPipe().instanceFrom(instance);
    Sequence inSeq = (Sequence) instance.getData();
    List<Sequence<Object>> outSeqs = crf.getMaxLatticeFactory().newMaxLattice(crf, inSeq).bestOutputSequences(10);
    for (Sequence outSeq : outSeqs) {
      // see if the outSeq is legal and if so then return it
      if (isLegal(word.getValue(), outSeq, expectedSylls)) {
        return outSeq;
      }
    }
    // none legal? just return highest probability
    return (Sequence) outSeqs.get(0);
  }

  private boolean isLegal(List<String> phones, Sequence marks, int expectedSylls) {
    if (PhoneSyllTagTrainer.USE_ONC_CODING) {
      if (expectedSylls > 0) {
        return (countSyllablesInSequence(marks) == expectedSylls);
      }
      return true;
    }
    // trying to exclude illegal boundaries instead of just coding the onc...
    boolean sawVowel = false;
    int syllCount = 0;
    Preconditions.checkState(phones.size() == marks.size());
    for (int i = 0; i < marks.size(); i++) {
      if (Phonemes.isVowel(phones.get(i))) {
        sawVowel = true;
      }
      if (marks.get(i).equals("1")) {
        if (!sawVowel) {
          return false; // have to have a vowel
        }
        syllCount += 1;
        sawVowel = false;
      }
    }
    if (expectedSylls > 0) {
      return (expectedSylls == syllCount);
    }
    return true;
  }
}
