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

package com.github.steveash.jg2p;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the encoder that presents a simplified interface if you're just looking for the top encoding or a simple
 * list of encodings for a word
 *
 * @author Steve Ash
 */
public class SimpleEncoder {

  private static final Joiner joiner = Joiner.on(' ');
  private final Encoder encoder;

  public SimpleEncoder(Encoder encoder) {
    this.encoder = encoder;
  }

  public List<String> encodeBest(String word) {
    List<PhoneticEncoder.Encoding> results = encoder.encode(Word.fromNormalString(word));
    if (results.isEmpty()) {
      return ImmutableList.of();
    }
    return results.get(0).phones;
  }

  public String encodeBestAsSpaceString(String word) {
    return joiner.join(encodeBest(word));
  }

  public List<String> encodeAsSpaceString(String word, int topKResults) {
    List<PhoneticEncoder.Encoding> results = encoder.encode(Word.fromNormalString(word));
    if (results.isEmpty()) {
      return ImmutableList.of();
    }
    int size = Math.min(topKResults, results.size());
    ArrayList<String> phoneList = Lists.newArrayListWithCapacity(size);
    for (int i = 0; i < size; i++) {
      phoneList.add(joiner.join(results.get(i).phones));
    }
    return phoneList;
  }

  public List<List<String>> encode(String word, int topKResults) {
    // TODO when switched to java 8 then replace this dup with lambda xform
    List<PhoneticEncoder.Encoding> results = encoder.encode(Word.fromNormalString(word));
    if (results.isEmpty()) {
      return ImmutableList.of();
    }
    int size = Math.min(topKResults, results.size());
    ArrayList<List<String>> phoneList = Lists.newArrayListWithCapacity(size);
    for (int i = 0; i < size; i++) {
      phoneList.add(results.get(i).phones);
    }
    return phoneList;
  }
}
