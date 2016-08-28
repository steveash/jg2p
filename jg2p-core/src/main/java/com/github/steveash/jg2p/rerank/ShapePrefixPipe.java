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

import com.github.steveash.jg2p.phoseq.WordShape;

/**
 * @author Steve Ash
 */
public class ShapePrefixPipe implements RerankFeature {

  private static final int DIST_BASE = 5;
  private static final long serialVersionUID = -8778690531170551758L;

  @Override
  public void emitFeatures(RerankFeatureBag data) {
    String wordShape = WordShape.graphShape(data.getExample().getWordGraphs(), false);
    String ansShape = WordShape.phoneShape(data.getExample().getEncoding().getPhones(), false);
    for (int i = Rerank3Model.minGoodShape; i <= Rerank3Model.maxGoodShape; i++) {
      if (wordShape.length() < i || ansShape.length() < i) {
        continue;
      }
      String gg = wordShape.substring(0, i);
      String pp = ansShape.substring(0, i);
//      if (gg.equalsIgnoreCase(pp) && Rerank2Model.goodShapes.contains(gg)) {
      if (gg.equalsIgnoreCase(pp)) {
        data.setBinary("sp_" + gg);
      }
    }
  }
}
