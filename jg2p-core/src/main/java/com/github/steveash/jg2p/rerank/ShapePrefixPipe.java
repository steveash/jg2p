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
public class ShapePrefixPipe extends Pipe {

  private static final int DIST_BASE = 5;

  public ShapePrefixPipe(Alphabet dataDict, Alphabet targetDict) {
    super(dataDict, targetDict);
  }

  @Override
  public Instance pipe(Instance inst) {
    RerankFeature data = (RerankFeature) inst.getData();
    String wordShape = WordShape.graphShape(data.getExample().getWordGraphs(), false);
    addShapeFeatures(wordShape, "A_", data.getExample().getEncodingA().phones, data);
    addShapeFeatures(wordShape, "B_", data.getExample().getEncodingB().phones, data);
    return inst;
  }

  private void addShapeFeatures(String wordShape, String prefix, List<String> phones, RerankFeature data) {
    String ansShape = WordShape.phoneShape(phones, false);
    for (int i = Rerank2Model.minGoodShape; i <= Rerank2Model.maxGoodShape; i++) {
      if (wordShape.length() < i || ansShape.length() < i) {
        continue;
      }
      String gg = wordShape.substring(0, i);
      String pp = ansShape.substring(0, i);
      if (gg.equalsIgnoreCase(pp) && Rerank2Model.goodShapes.contains(gg)) {
        data.setBinary(prefix + "sp_" + gg);
      }
    }
  }
}
