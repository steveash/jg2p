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

import com.github.steveash.jg2p.align.G2PModel;
import com.github.steveash.jg2p.align.ModelInputOutput;
import com.github.steveash.jg2p.seq.PhonemeCrfInputOutput;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;

import java.io.IOException;

/**
 * @author Steve Ash
 */
public class PhoneticEncoderFactory {

  private static final double ALIGN_MIN_SCORE = -60;
//  private static final double TAG_MIN_SCORE = -1.6094;
  private static final double TAG_MIN_SCORE = -1.894;

  public static PhoneticEncoder makeDefault() throws IOException, ClassNotFoundException {
    G2PModel alignModel = ModelInputOutput.readFromClasspath("cmu2.model.dat");
    PhonemeCrfModel phoneModel = PhonemeCrfInputOutput.readFromClasspath("g2p_crf.dat");

    return new PhoneticEncoder(alignModel, phoneModel, 5, ALIGN_MIN_SCORE, TAG_MIN_SCORE);
  }
}
