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

package com.github.steveash.jg2p.util;

import bsh.This;

import java.util.Arrays;
import java.util.List;

/**
 * Calculates the levenstein edit distance between two lists of things.  The things themselves are compared based on
 * equals().  This is stolen from the
 *
 * @author Steve Ash
 */
public class ListEditDistance {

  public static int editDistance(List<?> s, List<?> t, final int threshold) {
    if (s == null || t == null) {
      throw new IllegalArgumentException("Lists must not be null");
    }
    if (threshold < 0) {
      throw new IllegalArgumentException("Threshold must not be negative");
    }

    int n = s.size();
    int m = t.size();

    // if one list is empty, the edit distance is necessarily the length of the other
    if (n == 0) {
      return m <= threshold ? m : -1;
    } else if (m == 0) {
      return n <= threshold ? n : -1;
    }

    if (n > m) {
      // swap the two strings to consume less memory
      final List<?> tmp = s;
      s = t;
      t = tmp;
      n = m;
      m = t.size();
    }

    int p[] = new int[n + 1]; // 'previous' cost array, horizontally
    int d[] = new int[n + 1]; // cost array, horizontally
    int _d[]; // placeholder to assist in swapping p and d

    // fill in starting table values
    final int boundary = Math.min(n, threshold) + 1;
    for (int i = 0; i < boundary; i++) {
      p[i] = i;
    }
    // these fills ensure that the value above the rightmost entry of our
    // stripe will be ignored in following loop iterations
    Arrays.fill(p, boundary, p.length, Integer.MAX_VALUE);
    Arrays.fill(d, Integer.MAX_VALUE);

    // iterates through t
    for (int j = 1; j <= m; j++) {
      final Object t_j = t.get(j - 1); // jth entry of t
      d[0] = j;

      // compute stripe indices, constrain to array size
      final int min = Math.max(1, j - threshold);
      final int max = (j > Integer.MAX_VALUE - threshold) ? n : Math.min(n, j + threshold);

      // the stripe may lead off of the table if s and t are of different sizes
      if (min > max) {
        return -1;
      }

      // ignore entry left of leftmost
      if (min > 1) {
        d[min - 1] = Integer.MAX_VALUE;
      }

      // iterates through [min, max] in s
      for (int i = min; i <= max; i++) {
        if (s.get(i - 1).equals(t_j)) {
          // diagonally left and up
          d[i] = p[i - 1];
        } else {
          // 1 + minimum of cell to the left, to the top, diagonally left and up
          d[i] = 1 + Math.min(Math.min(d[i - 1], p[i]), p[i - 1]);
        }
      }

      // copy current distance counts to 'previous row' distance counts
      _d = p;
      p = d;
      d = _d;
    }

    // if p[n] is greater than the threshold, there's no guarantee on it being the correct
    // distance
    if (p[n] <= threshold) {
      return p[n];
    }
    return -1;
  }
}
