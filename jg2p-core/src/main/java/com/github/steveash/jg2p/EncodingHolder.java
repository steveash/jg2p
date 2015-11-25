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

package com.github.steveash.jg2p;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.Collections;
import java.util.List;

/**
 * Holder of an encoding and a score
 *
 * @author Steve Ash
 */
class EncodingHolder implements Comparable<EncodingHolder> {

  static List<PhoneticEncoder.Encoding> orderedResultsFrom(List<EncodingHolder> unordered) {
    Collections.sort(unordered, Ordering.natural().reverse());
    return Lists.transform(unordered, selectEncoding);
  }

  public static final Function<EncodingHolder, PhoneticEncoder.Encoding> selectEncoding =
      new Function<EncodingHolder, PhoneticEncoder.Encoding>() {
        @Override
        public PhoneticEncoder.Encoding apply(EncodingHolder encodingHolder) {
          return encodingHolder.encoding;
        }
      };

  final PhoneticEncoder.Encoding encoding;
  final double score;

  EncodingHolder(PhoneticEncoder.Encoding encoding, double score) {
    this.encoding = encoding;
    this.score = score;
  }

  @Override
  public int compareTo(EncodingHolder o) {
    return Double.compare(this.score, o.score);
  }
}
