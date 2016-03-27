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

import com.google.common.collect.Lists;

import com.github.steveash.jg2p.Word;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.mallet.fst.CRF;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;

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
    Instance instance = new Instance(word, null, null, null);
    instance = crf.getInputPipe().instanceFrom(instance);
    Sequence inSeq = (Sequence) instance.getData();
    Sequence<Object> seq = crf.transduce(inSeq);
    return SWord.convertEndMarkersToBoundaries(seq);
  }
}
