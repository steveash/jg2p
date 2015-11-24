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

import com.google.common.base.Preconditions;
import com.google.common.math.DoubleMath;

/**
 * @author Steve Ash
 */
public class RerankerResult {

  public enum Winner {A, B}

  public RerankerResult(double probabilityA, double probabilityB) {
    Preconditions.checkArgument(probabilityA >= 0 && probabilityA <= 1, "didnt get a real prob", probabilityA);
    Preconditions.checkArgument(probabilityB >= 0 && probabilityB <= 1, "didnt get a real prob", probabilityB);
    this.probabilityA = probabilityA;
    this.probabilityB = probabilityB;
  }

  private final double probabilityA;
  private final double probabilityB;

  public double getProbabilityA() {
    return probabilityA;
  }

  public double getProbabilityB() {
    return probabilityB;
  }

  public double logOddsAOverB() {
    return DoubleMath.log2(getProbabilityA()) - DoubleMath.log2(getProbabilityB());
  }

  public double logOddsBOverA() {
    return DoubleMath.log2(getProbabilityB()) - DoubleMath.log2(getProbabilityA());
  }
}
