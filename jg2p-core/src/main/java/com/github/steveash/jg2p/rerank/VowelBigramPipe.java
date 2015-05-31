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

package com.github.steveash.jg2p.rerank;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.phoseq.Phonemes;
import com.github.steveash.jg2p.phoseq.WordShape;
import com.github.steveash.jg2p.util.Scaler;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;

/**
 * @author Steve Ash
 */
public class VowelBigramPipe extends Pipe {

  private static final int DIST_BASE = 5;

  public VowelBigramPipe(Alphabet dataDict, Alphabet targetDict) {
    super(dataDict, targetDict);
  }

  @Override
  public Instance pipe(Instance inst) {
    RerankFeature data = (RerankFeature) inst.getData();
    addBigrams(data, "A_", data.getExample().getEncodingA().phones);
    addBigrams(data, "B_", data.getExample().getEncodingB().phones);
    return inst;
  }

  private void addBigrams(RerankFeature data, String prefix, List<String> phones) {
    for (int i = 0; i < phones.size(); i++) {
      String p = phones.get(i);
      if (!Phonemes.isVowel(p)) {
        continue;
      }
      if (i > 0) {
        data.setBinary(prefix + "bgm_" + phones.get(i-1) + "_" + p);
      }
      if (i + 1 < phones.size()) {
        data.setBinary(prefix + "bgm_" + p + "_" + phones.get(i + 1));
      }
    }
  }

}
