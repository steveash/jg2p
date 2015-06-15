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

package com.github.steveash.jg2p.seqvow;

import java.io.Serializable;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Steve Ash
 */
public class NearVowel implements RetaggerPipe, Serializable {

  private static final long serialVersionUID = 6068740923097116990L;
  private final int inc;
  private final int sibling;

  public NearVowel(int sibling) {
    if (sibling < 0) {
      inc = -1;
    } else {
      inc = +1;
    }
    this.sibling = Math.abs(sibling);
  }

  @Override
  public void pipe(int gramIndex, PartialTagging tagging) {
    String lbl = Integer.toString(inc * sibling);
    int foundCount = 0;
    for (int i = (gramIndex + inc); i < tagging.count() && i >= 0; i += inc) {
      String gram = tagging.getPartialPhoneGrams().get(i);
      String vowel = PartialPhones.extractVowelOrPartialFromGram(gram);
      if (isBlank(vowel)) {
        continue;
      }
      foundCount += 1;
      if (foundCount == sibling) {
        tagging.addFeature(gramIndex, "VP" + lbl + vowel);
        tagging.addFeature(gramIndex, "VG" + lbl + tagging.getGraphemeGrams().get(i));
        return;
      }
    }
    // if we're here then there isn't a sibling that satisfies
    tagging.addFeature(gramIndex, "VP" + lbl + "*");
    tagging.addFeature(gramIndex, "VG" + lbl + "*");
  }
}
