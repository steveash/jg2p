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

package com.github.steveash.jg2p.lm;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.TrainOptions;

import java.io.Serializable;
import java.util.List;

import kylm.model.ngram.NgramLM;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Wraps a lang model and knows how to produce a score given an encoding
 * @author Steve Ash
 */
public class LangModel implements Serializable {
  private static final long serialVersionUID = -843134336202792076L;

  private final NgramLM gramLm;
  private final boolean isGraphoneModel;

  public LangModel(NgramLM gramLm, boolean isGraphoneModel) {
    this.gramLm = gramLm;
    this.isGraphoneModel = isGraphoneModel;
  }

  public double score(PhoneticEncoder.Encoding enc) {
    String[] gramSeq = makeSequence(enc);
    return gramLm.getSentenceProbNormalized(gramSeq);
  }

  private String[] makeSequence(PhoneticEncoder.Encoding enc) {
    if (isGraphoneModel) {
      return makeGraphoneSeq(enc.alignment, enc.graphones);
    }
    return makePhonemeSeq(enc.phones);
  }

  public static String[] makeSequenceFromAlignment(Alignment align, boolean useGraphoneModel) {
    if (useGraphoneModel) {
      return makeGraphoneSeq(align.getAllXTokensAsList(), align.getAllYTokensAsList());
    }
    return makePhonemeSeq(Lists.newArrayList(align.getYTokens()));
  }

  public static String[] makeGraphoneSeq(List<String> graphemes, List<String> phonemes) {
    Preconditions.checkArgument(graphemes.size() == phonemes.size(), "must be same length");
    String[] seq = new String[graphemes.size()];
    for (int i = 0; i < graphemes.size(); i++) {
      seq[i] = graphemes.get(i) + "^" + phonemes.get(i);
    }
    return seq;
  }

  public static String[] makePhonemeSeq(List<String> phonesNoEps) {
    String[] seq = new String[phonesNoEps.size()];
    for (int i = 0; i < phonesNoEps.size(); i++) {
      seq[i] = phonesNoEps.get(i);
      Preconditions.checkState(isNotBlank(seq[i]), "cant have an epsilon phoneme here");
    }
    return seq;
  }
}
