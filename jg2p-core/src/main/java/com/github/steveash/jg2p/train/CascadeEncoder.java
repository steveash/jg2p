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

package com.github.steveash.jg2p.train;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.seqbin.SeqBinModel;

import java.io.Serializable;
import java.util.List;

import cc.mallet.types.Labeling;

/**
 * @author Steve Ash
 */
public class CascadeEncoder implements Serializable {

  private static final long serialVersionUID = -6011185185779914214L;

  private final AlignModel alignModel;
  private final PhoneticEncoder encoderG;
  private final PhoneticEncoder encoderB;
  private final SeqBinModel seqBin;

  public CascadeEncoder(AlignModel alignModel, PhoneticEncoder encoderG, PhoneticEncoder encoderB, SeqBinModel seqBin) {
    this.alignModel = alignModel;
    this.encoderG = encoderG;
    this.encoderB = encoderB;
    this.seqBin = seqBin;
  }

  public PhoneticEncoder getEncoderG() {
    return encoderG;
  }

  public PhoneticEncoder getEncoderB() {
    return encoderB;
  }

  public List<PhoneticEncoder.Encoding> encode(String word) {
    Word input = Word.fromNormalString(word);
    return encode(input);
  }

  public List<PhoneticEncoder.Encoding> encode(Word input) {

    Labeling gOrB = seqBin.classify(input);
    List<PhoneticEncoder.Encoding> resultsG = encoderG.encode(input);
    List<PhoneticEncoder.Encoding> resultsB = encoderB.encode(input);
    double gProb, bProb;
    boolean firstG = ((String)gOrB.getBestLabel().getEntry()).equalsIgnoreCase("G");
    if (firstG) {
      gProb = gOrB.getBestValue();
      bProb = 1.0 - gProb;
    } else {
      bProb = gOrB.getBestValue();
      gProb = 1.0 - bProb;
    }
    PhoneticEncoder.Encoding best = null;
    double bestProb = Double.NEGATIVE_INFINITY;
    for (PhoneticEncoder.Encoding encoding : resultsG) {
      double thisProb = encoding.tagProbability() * gProb;
      if (thisProb > bestProb) {
        bestProb = thisProb;
        best = encoding;
      }
    }
    for (PhoneticEncoder.Encoding encoding : resultsB) {
      double thisProb = encoding.tagProbability() * bProb;
      if (thisProb > bestProb) {
        bestProb = thisProb;
        best = encoding;
      }
    }
    return ImmutableList.of(best);
  }
}
