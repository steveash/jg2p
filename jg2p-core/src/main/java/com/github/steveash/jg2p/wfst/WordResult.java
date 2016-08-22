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

package com.github.steveash.jg2p.wfst;

import com.google.common.collect.ComparisonChain;

import com.github.steveash.jg2p.Word;

/**
 * @author Steve Ash
 */
public class WordResult implements Comparable<WordResult> {

  private final Word word;
  private final double score;

  public WordResult(Word word, double score) {
    this.word = word;
    this.score = score;
  }

  public Word getWord() {
    return word;
  }

  public double getScore() {
    return score;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WordResult that = (WordResult) o;

    if (Double.compare(that.score, score) != 0) {
      return false;
    }
    return word != null ? word.equals(that.word) : that.word == null;

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    result = word != null ? word.hashCode() : 0;
    temp = Double.doubleToLongBits(score);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public int compareTo(WordResult o) {
    return ComparisonChain.start()
        .compare(this.score, o.score)
        .compare(this.word, o.word)
        .result();
  }

  @Override
  public String toString() {
    return "WordResult{" +
           "word=" + word +
           ", score=" + score +
           '}';
  }
}
