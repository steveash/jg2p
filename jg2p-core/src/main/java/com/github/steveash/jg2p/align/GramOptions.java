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

package com.github.steveash.jg2p.align;

import java.io.Serializable;

/**
 * @author Steve Ash
 */
public class GramOptions implements Serializable {
  private static final long serialVersionUID = -1;

  private int minXGram = 1;  // these have to be 1 right now
  private int maxXGram = 2;
  private int minYGram = 1;  // these have to be 1 right now
  private int maxYGram = 1;
  private boolean includeXEpsilons = true;
  private boolean includeEpsilonYs = false;
  private boolean onlyOneGrams = true;  // if you have X>1 && Y>1 then only allow 1:y or x:1 not 2:2, 3:2, etc.

  public GramOptions(int minXGram, int maxXGram, int minYGram, int maxYGram, boolean includeXEpsilons,
                     boolean includeEpsilonYs, boolean onlyOneGrams) {
    this.minXGram = minXGram;
    this.maxXGram = maxXGram;
    this.minYGram = minYGram;
    this.maxYGram = maxYGram;
    this.includeXEpsilons = includeXEpsilons;
    this.includeEpsilonYs = includeEpsilonYs;
    this.onlyOneGrams = onlyOneGrams;
  }

  public GramOptions(int minGram, int maxGram) {
    this(minGram, maxGram, minGram, maxGram, false, false, true);
  }

  public GramOptions() {
  }

  public int getMinXGram() {
    return minXGram;
  }

  public void setMinXGram(int minXGram) {
    this.minXGram = minXGram;
  }

  public int getMaxXGram() {
    return maxXGram;
  }

  public void setMaxXGram(int maxXGram) {
    this.maxXGram = maxXGram;
  }

  public int getMinYGram() {
    return minYGram;
  }

  public void setMinYGram(int minYGram) {
    this.minYGram = minYGram;
  }

  public int getMaxYGram() {
    return maxYGram;
  }

  public void setMaxYGram(int maxYGram) {
    this.maxYGram = maxYGram;
  }

  public boolean isIncludeXEpsilons() {
    return includeXEpsilons;
  }

  public void setIncludeXEpsilons(boolean includeXEpsilons) {
    this.includeXEpsilons = includeXEpsilons;
  }

  public boolean isIncludeEpsilonYs() {
    return includeEpsilonYs;
  }

  public void setIncludeEpsilonYs(boolean includeEpsilonYs) {
    this.includeEpsilonYs = includeEpsilonYs;
  }

  public boolean isOnlyOneGrams() {
    return onlyOneGrams;
  }

  public void setOnlyOneGrams(boolean onlyOneGrams) {
    this.onlyOneGrams = onlyOneGrams;
  }
}
