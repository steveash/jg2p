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

package com.github.steveash.jg2p.align;

import com.github.steveash.jg2p.Grams;

/**
 * @author Steve Ash
 */
public class CityBlockPenalizer implements Penalizer {

  public static final CityBlockPenalizer Instance = new CityBlockPenalizer();

  private static final int EPS_PENALTY = 2;
  private static final double PENALTY_SCALE = 1.0;

  @Override
  public double penalize(String xGram, String yGram, double prob) {
    int xCount = Grams.countInGram(xGram);
    int yCount = Grams.countInGram(yGram);
    if (xCount < 1) {
      xCount = EPS_PENALTY;
    }
    if (yCount < 1) {
      yCount = EPS_PENALTY;
    }
    return Math.pow(prob, (xCount + yCount) * PENALTY_SCALE);
  }
}
