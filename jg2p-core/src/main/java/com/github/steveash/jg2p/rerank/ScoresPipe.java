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

/**
 * @author Steve Ash
 */
public class ScoresPipe implements RerankFeature {

  private static final long serialVersionUID = -8862818937464442838L;

  @Override
  public void emitFeatures(RerankFeatureBag data) {
    RerankExample ex = data.getExample();
    data.setFeature("A_tagScore", ex.getEncoding().tagProbability());
//        data.setFeature("A_retagScore", ex.getEncodingA().retagProbability());
    data.setFeature("A_alignScore", ex.getEncoding().alignScore);
    data.setFeature("A_lmScore", ex.getLanguageModelScore());
  }
}
