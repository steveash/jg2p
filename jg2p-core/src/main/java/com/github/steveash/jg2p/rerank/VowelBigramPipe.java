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

import com.github.steveash.jg2p.phoseq.Phonemes;

/**
 * @author Steve Ash
 */
public class VowelBigramPipe implements RerankFeature {

  private static final int DIST_BASE = 5;

  @Override
  public void emitFeatures(RerankFeatureBag data) {
    for (int i = 0; i < data.getExample().getEncoding().phones.size(); i++) {
      String p = data.getExample().getEncoding().phones.get(i);
      if (!Phonemes.isVowel(p)) {
        continue;
      }
      if (i > 0) {
        data.setBinary("bgm_" + data.getExample().getEncoding().phones.get(i - 1) + "_" + p);
      }
      if (i + 1 < data.getExample().getEncoding().phones.size()) {
        data.setBinary("bgm_" + p + "_" + data.getExample().getEncoding().phones.get(i + 1));
      }
    }
  }

}
