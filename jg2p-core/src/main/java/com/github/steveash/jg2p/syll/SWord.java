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

package com.github.steveash.jg2p.syll;/*
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

import com.github.steveash.jg2p.Word;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.mallet.types.Sequence;

/**
 * This is a Word that knows its syllable boundaries; used for training the syllble tagger
 *
 * @author Steve Ash
 */
public class SWord extends Word {

  private int[] bounds;

  /**
   * @param celexString this is the celex pipe delimited with hypthen syll markers format
   */
  public SWord(String celexString) {
    super(convertToPhones(celexString));
    // need to record the syllable boundaries
    String[] phones = celexString.split("\\|");
    int realIndex = 0;
    ArrayList<Integer> boundaries = Lists.newArrayList();
    for (int i = 0; i < phones.length; i++) {
      if (phones[i].contains("-")) {
        boundaries.add(realIndex);
      } else {
        realIndex += 1;
      }
    }
    this.bounds = ArrayUtils.toPrimitive(boundaries.toArray(new Integer[0]));
  }

  public SWord(String spaceSepWord, String spaceSepSyllStarts) {
    super(splitter.splitToList(spaceSepWord));
    List<Integer> bs = Lists.newArrayList();
    for (String val : splitter.split(spaceSepSyllStarts)) {
      bs.add(Integer.parseInt(val));
    }
    Preconditions.checkArgument(bs.size() > 0, "must pass at least one syllable boundary");
    this.bounds = Ints.toArray(Ordering.natural().sortedCopy(bs));
  }

  public static List<String> convertToPhones(String entry) {
    List<String> result = Lists.newArrayList();
    String[] parts = entry.split("\\|");
    for (String part : parts) {
      if (!part.contains("-")) {
        result.add(part);
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return "SWord-" + Arrays.toString(bounds) + "-" + super.toString();
  }

  public int[] getBounds() {
    return bounds;
  }

  public boolean isStartOfSyllable(int phoneIndex) {
    if (phoneIndex == 0) {
      return true;
    }
    return Arrays.binarySearch(bounds, phoneIndex) >= 0;
  }

  public List<String> getStartMarkers() {
    ArrayList<String> marks = Lists.newArrayListWithCapacity(this.unigramCount());
    int nextMark = 0;
    for (int i = 0; i < this.unigramCount(); i++) {
      if (nextMark < bounds.length && bounds[nextMark] == i) {
        marks.add("1");
        nextMark += 1;
      } else {
        marks.add("0");
      }
    }
    Preconditions.checkState(nextMark == bounds.length);
    return marks;
  }

  public List<String> getEndMarkers() {
    ArrayList<String> marks = Lists.newArrayListWithCapacity(this.unigramCount());
    int nextMark = 1; // always skip the leading mark
    for (int i = 0; i < this.unigramCount(); i++) {
      if (nextMark < bounds.length && bounds[nextMark] == (i + 1)) {
        marks.add("1");
        nextMark += 1;
      } else {
        marks.add("0");
      }
    }
    Preconditions.checkState(nextMark == bounds.length);
    marks.set(this.unigramCount() - 1, "1"); // the last one is always the end
    return marks;
  }

  public static List<Integer> convertEndMarkersToBoundaries(Sequence<?> marks) {
    ArrayList<Integer> result = Lists.newArrayList();
    result.add(0);
    for (int i = 1; i < marks.size(); i++) {
      if (marks.get(i - 1).equals("1")) {
        result.add(i);
      }
    }
    return result;
  }

  public static List<Integer> convertStartMarkersToBoundaries(Sequence<?> marks) {
    ArrayList<Integer> bounds = Lists.newArrayListWithCapacity(marks.size());
    for (int i = 0; i < marks.size(); i++) {
      Object thisLabel = marks.get(i);
      if (thisLabel.equals("1")) {
        bounds.add(i);
      }
    }
    return bounds;
  }
}
