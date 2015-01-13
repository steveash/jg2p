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

package com.github.steveash.jg2p.seq;

import com.google.common.base.CharMatcher;

import java.util.List;

import cc.mallet.types.TokenSequence;

import static com.google.common.base.CharMatcher.WHITESPACE;

/**
 * Feature that represents a neighbor as just consonants and vowels (normalizes distinct letters to v or c) also
 * normalizes all punc to p
 *
 * @author Steve Ash
 */
public class NeighborShapeFeature extends NeighborTokenFeature {

  private static final CharMatcher vowels = CharMatcher.anyOf("AEIOUY").precomputed();
  private static final CharMatcher consonants = CharMatcher.inRange('A', 'Z').and(vowels.negate()).precomputed();
  private static final CharMatcher other = CharMatcher.ANY.and(vowels.or(consonants).negate()).precomputed();

  public NeighborShapeFeature(boolean includeCurrent, int... neighbors) {
    super(includeCurrent, neighbors);
  }

  public NeighborShapeFeature(boolean includeCurrent, List<TokenWindow> windows) {
    super(includeCurrent, windows);
  }

  @Override
  protected String getWindow(TokenSequence ts, int i, TokenWindow window) {
    String winStr = super.getWindow(ts, i, window);
    if (winStr == null) return null;

    return convertShape(winStr);
  }

  static String convertShape(String winStr) {
    StringBuilder sb = new StringBuilder(winStr.length());
    for (int i = 0; i < winStr.length(); i++) {
      char c = winStr.charAt(i);
      if (consonants.matches(c)) {
        sb.append('c');
      } else if (vowels.matches(c)) {
        sb.append('v');
      } else if (WHITESPACE.matches(c)) {
        sb.append('s');
      } else {
        sb.append('p');
      }
    }
    return sb.toString();
  }
}
