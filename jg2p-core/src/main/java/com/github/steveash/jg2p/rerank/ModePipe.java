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
public class ModePipe extends Pipe {

  public ModePipe(Alphabet dataDict, Alphabet targetDict) {
    super(dataDict, targetDict);
  }

  @Override
  public Instance pipe(Instance inst) {
    RerankFeature data = (RerankFeature) inst.getData();
    if (data.getExample().isUniqueMatchingModeA()) {
      data.setBinary("A_uniqueMode");
    }
    if (data.getExample().isUniqueMatchingModeB()) {
      data.setBinary("B_uniqueMode");
    }
    return inst;
  }
}
