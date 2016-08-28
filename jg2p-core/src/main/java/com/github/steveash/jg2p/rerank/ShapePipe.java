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

import org.apache.commons.lang3.StringUtils;

/**
 * @author Steve Ash
 */
public class ShapePipe implements RerankFeature {

  private static final int DIST_BASE = 5;
  private static final long serialVersionUID = -2962634341623299872L;

  @Override
  public void emitFeatures(RerankFeatureBag data) {
    String wordShape = WordShape.graphShape(data.getExample().getWordGraphs(), false);
    String ansShape = WordShape.phoneShape(data.getExample().getEncoding().getPhones(), false);
    int dist = StringUtils.getLevenshteinDistance(wordShape, ansShape, DIST_BASE);
    double distFeature = 1.0;
    if (dist > 0) {
//      distFeature = Scaler.scaleLogSquash(dist, DIST_BASE, 1.0);
      distFeature = dist;
    }
    data.setFeature("shpDst", distFeature);

    int lenDiff = Math.abs(wordShape.length() - ansShape.length());
//    double lenDiffFeature = Scaler.scaleLogSquash(lenDiff, 3.0, 1.0);
    double lenDiffFeature = lenDiff;
    data.setFeature("shpLenDiff", lenDiffFeature);
  }
}
