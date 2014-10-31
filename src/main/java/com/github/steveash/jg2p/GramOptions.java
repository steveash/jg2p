/*
 * Copyright 2014 Steve Ash
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

package com.github.steveash.jg2p;

/**
 * @author Steve Ash
 */
public class GramOptions {

  public int minXGram = 1;  // these have to be 1 right now
  public int maxXGram = 2;
  public int minYGram = 1;  // these have to be 1 right now
  public int maxYGram = 1;
  public boolean includeXEpsilons = true;
  public boolean includeEpsilonYs = true;
  public Maximizer maximizer = Maximizer.JOINT;
  public int maxIterations = 100;
  public double probDeltaConvergenceThreshold = 1.0e-5;

  public GramOptions(int minXGram, int maxXGram, int minYGram, int maxYGram, boolean includeXEpsilons,
                     boolean includeEpsilonYs) {
    this.minXGram = minXGram;
    this.maxXGram = maxXGram;
    this.minYGram = minYGram;
    this.maxYGram = maxYGram;
    this.includeXEpsilons = includeXEpsilons;
    this.includeEpsilonYs = includeEpsilonYs;
  }

  public GramOptions(int minGram, int maxGram) {
    this(minGram, maxGram, minGram, maxGram, false, false);
  }

  public GramOptions() {

  }

}
