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

import com.google.common.base.Function;

import com.github.steveash.jg2p.PhoneticEncoder;


/**
 * @author Steve Ash
 */
public class RerankerResult implements Comparable<RerankerResult> {

  public static final Function<RerankerResult, PhoneticEncoder.Encoding> SelectEncoding = new Function<RerankerResult, PhoneticEncoder.Encoding>() {
    @Override
    public PhoneticEncoder.Encoding apply(RerankerResult input) {
      return input.getExample().getEncoding();
    }
  };

  private final RerankExample example;
  private final double score;

  public RerankerResult(RerankExample example, double score) {
    this.example = example;
    this.score = score;
  }

  public RerankExample getExample() {
    return example;
  }

  public double getScore() {
    return score;
  }

  @Override
  public int compareTo(RerankerResult o) {
    return Double.compare(this.score, o.score);
  }
}
