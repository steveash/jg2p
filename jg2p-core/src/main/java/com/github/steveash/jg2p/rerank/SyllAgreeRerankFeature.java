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

package com.github.steveash.jg2p.rerank;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.syll.PhoneSyllTagModel;

import java.util.List;

/**
 * @author Steve Ash
 */
public class SyllAgreeRerankFeature implements RerankFeature {

  private static final long serialVersionUID = -9059365451231193252L;

  private final PhoneSyllTagModel phoneSyllTagModel;

  public SyllAgreeRerankFeature(PhoneSyllTagModel phoneSyllTagModel) {
    this.phoneSyllTagModel = phoneSyllTagModel;
  }

  @Override
  public void emitFeatures(RerankFeatureBag bag) {
    RerankExample ex = bag.getExample();
    int gramSyllCount = ex.getEncoding().wordSyllCount;
    if (gramSyllCount <= 0) {
      return;
    }
    List<Integer> starts = phoneSyllTagModel.syllStarts(Word.fromGrams(ex.getEncoding().phones));
    bag.setFeature("syll_diff", starts.size() - gramSyllCount);
  }
}
