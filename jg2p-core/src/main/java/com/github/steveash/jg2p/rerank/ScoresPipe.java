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

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;

/**
 * @author Steve Ash
 */
public class ScoresPipe extends Pipe {

  public ScoresPipe(Alphabet dataDict, Alphabet targetDict) {
    super(dataDict, targetDict);
  }

  @Override
  public Instance pipe(Instance inst) {
    RerankFeature data = (RerankFeature) inst.getData();
    RerankExample ex = data.getExample();
    data.setFeature("A_tagScore", ex.getEncodingA().tagProbability());
    data.setFeature("B_tagScore", ex.getEncodingB().tagProbability());
    data.setFeature("A_alignScore", Math.pow(2.0, ex.getEncodingA().alignScore));
    data.setFeature("B_alignScore", Math.pow(2.0, ex.getEncodingB().alignScore));
    data.setFeature("A_lmScore", ex.getLanguageModelScoreA());
    data.setFeature("B_lmScore", ex.getLanguageModelScoreB());

    return inst;
  }
}
