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

package com.github.steveash.jg2p.phoseq;

import com.google.common.collect.ImmutableMap;

/**
 * @author Steve Ash
 */
public class Phonemes {

  // key is phone, value is mono, dipthong, r-color, stop, affricate, fricative, nasal, liquid, semivowels
  private static final ImmutableMap<String, String> phoneClass = ImmutableMap.<String, String>builder()
      .put("HH", "F")
      .put("B", "S")
      .put("D", "S")
      .put("DH", "F")
      .put("F", "F")
      .put("G", "S")
      .put("K", "S")
      .put("L", "L")
      .put("M", "N")
      .put("N", "N")
      .put("P", "S")
      .put("R", "L")
      .put("S", "F")
      .put("UH", "M")
      .put("T", "S")
      .put("SH", "F")
      .put("V", "F")
      .put("W", "I")
      .put("Y", "I")
      .put("Z", "F")
      .put("IH", "M")
      .put("AA", "M")
      .put("UW", "M")
      .put("EH", "M")
      .put("AE", "M")
      .put("CH", "A")
      .put("AH", "M")
      .put("OW", "D")
      .put("OY", "D")
      .put("ER", "R")
      .put("AO", "M")
      .put("ZH", "F")
      .put("IY", "M")
      .put("EY", "D")
      .put("TH", "F")
      .put("AW", "D")
      .put("AY", "D")
      .put("NG", "N")
      .put("JH", "A")
      .build();

}
