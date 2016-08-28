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

/**
 * @author Steve Ash
 */
public class RanksPipe implements RerankFeature {

  private static final double RANK_SCALE_BASE = 4.0;
  private static final long serialVersionUID = 4679033851181086884L;

  private void addRanks(RerankFeatureBag data, PhoneticEncoder.Encoding encoding) {
//    data.setFeature(prefix + "all", (Scaler.scaleLog(encoding.rank, RANK_SCALE_BASE)));
//    data.setFeature(prefix + "alg", (Scaler.scaleLog(encoding.alignRank, RANK_SCALE_BASE)));

    data.setFeature("all", encoding.getRank());
    data.setFeature("alg", encoding.alignRank);
  }

  @Override
  public void emitFeatures(RerankFeatureBag data) {
    addRanks(data, data.getExample().getEncoding());
  }
}
