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

import com.google.common.base.Joiner;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.phoseq.Phonemes;
import com.github.steveash.jg2p.phoseq.WordShape;
import com.github.steveash.jg2p.util.Scaler;

import org.apache.commons.lang3.StringUtils;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;

/**
 * @author Steve Ash
 */
public class ShapePipe extends Pipe {

  private static final int DIST_BASE = 5;

  public ShapePipe(Alphabet dataDict, Alphabet targetDict) {
    super(dataDict, targetDict);
  }

  @Override
  public Instance pipe(Instance inst) {
    RerankFeature data = (RerankFeature) inst.getData();
    String wordShape = WordShape.graphShape(data.getExample().getWordGraphs(), false);
    addShapeFeatures(wordShape, "A_", data.getExample().getEncodingA(), data);
    addShapeFeatures(wordShape, "B_", data.getExample().getEncodingB(), data);
    return inst;
  }

  private void addShapeFeatures(String wordShape, String prefix, PhoneticEncoder.Encoding encoding, RerankFeature data) {
    String ansShape = WordShape.phoneShape(encoding.phones, false);
    int dist = StringUtils.getLevenshteinDistance(wordShape, ansShape, DIST_BASE);
    double distFeature = 1.0;
    if (dist > 0) {
      distFeature = Scaler.scaleLogSquash(dist, DIST_BASE, 1.0);
    }
    data.setFeature(prefix + "shpDst", distFeature);

    int lenDiff = Math.abs(wordShape.length() - ansShape.length());
    double lenDiffFeature = Scaler.scaleLogSquash(lenDiff, 3.0, 1.0);
    data.setFeature(prefix + "shpLenDiff", lenDiffFeature);
  }
}
