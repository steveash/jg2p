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

package com.github.steveash.jg2p;

import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Aligner;
import com.github.steveash.jg2p.aligntag.AlignTagModel;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.util.ReadWrite;

import java.io.IOException;

import static com.github.steveash.jg2p.util.ReadWrite.readFromClasspath;

/**
 * @author Steve Ash
 */
public class PhoneticEncoderFactory {

  private static final double ALIGN_MIN_SCORE = -60;
//  private static final double TAG_MIN_SCORE = -1.6094;
  private static final double TAG_MIN_SCORE = -1.894;
  private static final int BEST_ALIGNMENTS = 5;

  public static PhoneticEncoder makeDefault() throws IOException, ClassNotFoundException {
    return makeFrom("cmu3.model.dat", "g2p_crf3.dat");
  }

  public static PhoneticEncoder makeFrom(String alignModelFile, String crfModelFile) throws IOException, ClassNotFoundException {
    AlignTagModel alignModel = readFromClasspath(AlignTagModel.class, alignModelFile);
    PhonemeCrfModel phoneModel = readFromClasspath(PhonemeCrfModel.class, crfModelFile);

    return new PhoneticEncoder(alignModel, phoneModel, BEST_ALIGNMENTS, ALIGN_MIN_SCORE, TAG_MIN_SCORE);
  }

  public static PhoneticEncoder make(Aligner alignModel, PhonemeCrfModel crfModel) {
    return new PhoneticEncoder(alignModel, crfModel, BEST_ALIGNMENTS, ALIGN_MIN_SCORE, TAG_MIN_SCORE);
  }
}
