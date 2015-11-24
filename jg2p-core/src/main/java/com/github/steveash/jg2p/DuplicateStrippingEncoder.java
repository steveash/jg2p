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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * Decorator for encoders that might return multiple Encodings in the results list that are identical phoneme
 * sequences. For exmaple, different alignments of the graphemes might end up with the same phoneme sequence
 * This post-processes the return value and if there are any duplicate entries - this will strip them leaving
 * the list in the same order but with subsequent duplicates removed. It will re-assign the ranks also
 * @author Steve Ash
 */
public class DuplicateStrippingEncoder implements Encoder {

  private final Encoder encoder;

  public static Encoder decorateIfNotAlready(Encoder delegate) {
    if (delegate instanceof DuplicateStrippingEncoder) {
      return delegate;
    }
    return new DuplicateStrippingEncoder(delegate);
  }

  public DuplicateStrippingEncoder(Encoder encoder) {
    this.encoder = encoder;
  }

  @Override
  public List<PhoneticEncoder.Encoding> encode(Word input) {
    List<PhoneticEncoder.Encoding> results = encoder.encode(input);
    List<PhoneticEncoder.Encoding> output = Lists.newArrayListWithCapacity(results.size());
    Set<List<String>> seenPhoneSeqs = Sets.newHashSetWithExpectedSize(results.size());
    int newRank = 0;
    for (PhoneticEncoder.Encoding result : results) {
      if (seenPhoneSeqs.add(result.phones)) {
        result.rank = newRank++;
        output.add(result);
      }
    }
    return output;
  }
}
