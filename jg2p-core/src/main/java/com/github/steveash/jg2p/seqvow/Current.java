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

import com.google.common.base.Preconditions;

import java.io.Serializable;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @author Steve Ash
 */
public class Current implements RetaggerPipe, Serializable {

  private static final long serialVersionUID = -145933051168768617L;

  @Override
  public void pipe(int gramIndex, PartialTagging tagging) {
    String g = "CG" + tagging.getGraphemeGrams().get(gramIndex);
    tagging.addFeature(gramIndex, g);

    String partialGram = tagging.getPartialPhoneGrams().get(gramIndex);
    String partialGramOnly = PartialPhones.extractPartialPhoneGramFromGram(partialGram);
    Preconditions.checkArgument(isNotBlank(partialGramOnly), "got no eligible from ", partialGram);
    String p = "CP" + partialGramOnly;
    tagging.addFeature(gramIndex, p);

    String notPartialGramOnly = PartialPhones.extractNotPartialPhoneGramFromGram(partialGram);
    if (isNotBlank(notPartialGramOnly)) {
      String i = "CI" + notPartialGramOnly;
      tagging.addFeature(gramIndex, i);
    }
  }
}
