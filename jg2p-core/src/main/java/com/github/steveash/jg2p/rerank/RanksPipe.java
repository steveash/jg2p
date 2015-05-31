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
import com.github.steveash.jg2p.util.Scaler;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;

/**
 * @author Steve Ash
 */
public class RanksPipe extends Pipe {

  private static final double RANK_SCALE_BASE = 4.0;

  public RanksPipe(Alphabet dataDict, Alphabet targetDict) {
    super(dataDict, targetDict);
  }

  @Override
  public Instance pipe(Instance inst) {
    RerankFeature data = (RerankFeature) inst.getData();
    addRanks(data, "A_", data.getExample().getEncodingA());
    addRanks(data, "B_", data.getExample().getEncodingB());
    return inst;
  }

  private void addRanks(RerankFeature data, String prefix, PhoneticEncoder.Encoding encoding) {
    data.setFeature(prefix + "all", (Scaler.scaleLog(encoding.rank, RANK_SCALE_BASE)));
    data.setFeature(prefix + "alg", (Scaler.scaleLog(encoding.alignRank, RANK_SCALE_BASE)));
  }
}
