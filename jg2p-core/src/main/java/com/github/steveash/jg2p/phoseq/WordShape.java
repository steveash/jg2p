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

import java.util.List;

/**
 * @author Steve Ash
 */
public class WordShape {

  public static String phoneShape(List<String> phones, boolean omitAdjacentDuplicates) {
    StringBuilder sb = new StringBuilder(phones.size());
    String last = "";
    for (String phone : phones) {
      boolean isVowel = Phonemes.isVowel(phone);
      String next = (isVowel ? "v" : "C");
      if (omitAdjacentDuplicates && last.equals(next)) {
        // skip it
      } else {
        sb.append(next);
        last = next;
      }
    }
    return sb.toString();
  }

  public static String graphShape(List<String> graphemes, boolean omitAdjacentDuplicates) {
    StringBuilder sb = new StringBuilder(graphemes.size());
    String last = "";
    for (String graph : graphemes) {
      String next;
      if (Graphemes.isVowel(graph)) {
        next = "v";
      } else if (Graphemes.isConsonant(graph)) {
        next = "C";
      } else {
        continue; // some punchation or something
      }
      if (omitAdjacentDuplicates && last.equals(next)) {
        // skip it
      } else {
        sb.append(next);
        last = next;
      }
    }
    return sb.toString();
  }

}
