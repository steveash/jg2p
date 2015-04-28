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

package com.github.steveash.jg2p.phoseq;

import com.google.common.math.DoubleMath;

/**
 * @author Steve Ash
 */
public class Perplexity {

  private double sumLogProb = 0;
  private double normalLogProb = 0;
  private int sentenceCount = 0;

  public void addSentenceProb(double prob, int sentenceLength) {
    double log2 = DoubleMath.log2(prob);
    normalLogProb += (log2 / (sentenceLength + 2)); // adding 2 for the term/start chars
    sumLogProb += log2;
    sentenceCount += 1;
  }

  public double calculate() {
    double exp = -1 * (sumLogProb / sentenceCount);
    return Math.pow(2.0, exp);
  }

  public double averageNormalLogProb() {
    return normalLogProb / sentenceCount;
  }

}
