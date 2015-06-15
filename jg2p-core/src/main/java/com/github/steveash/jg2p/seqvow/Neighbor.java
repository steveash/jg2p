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

package com.github.steveash.jg2p.seqvow;

import com.google.common.base.Preconditions;

import com.github.steveash.jg2p.util.GramBuilder;
import com.github.steveash.jg2p.util.GramWalker;
import com.github.steveash.jg2p.util.TokenSeqUtil;

import java.io.Serializable;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Emits features for neighbors (either graph or phone)
 * @author Steve Ash
 */
public class Neighbor implements RetaggerPipe, Serializable {

  private static final long serialVersionUID = 8291176976227518187L;
  private final boolean isGraph;
  private final int windowOffset;
  private final int windowLength;

  public Neighbor(boolean isGraph, int windowOffset, int windowLength) {
    this.isGraph = isGraph;
    this.windowOffset = windowOffset;
    this.windowLength = windowLength;
  }

  @Override
  public void pipe(int gramIndex, PartialTagging tagging) {
    if (isGraph) {
      List<String> target = tagging.getGraphemeGrams();
      String window = TokenSeqUtil.getWindowFromStrings(target, gramIndex, windowOffset, windowLength);
      if (isNotBlank(window)) {
        tagging.addFeature(gramIndex, "NG" + Integer.toString(windowOffset) + Integer.toString(windowLength) + window);
      }
    } else {
      List<String> target = tagging.getPartialPhoneGrams();
      int symbolInGramOffset;
      if (windowOffset < 0) {
        symbolInGramOffset = findFirstIndexPartialInGram(target.get(gramIndex));
      } else {
        symbolInGramOffset = findLastIndexPartialInGram(target.get(gramIndex));
      }
      String window = GramWalker.window(target, gramIndex, symbolInGramOffset, windowOffset, windowLength);
      if (isNotBlank(window)) {
        tagging.addFeature(gramIndex, "NP" + Integer.toString(windowOffset) + Integer.toString(windowLength) + window);
      }
    }
  }

  private int findFirstIndexPartialInGram(String gram) {
    if (GramBuilder.isUnaryGram(gram)) {
      return 0;
    }
    int i = 0;
    for (String s : GramBuilder.SPLITTER.split(gram)) {
      if (PartialPhones.isPartialPhone(s)) {
        return i;
      }
      i += 1;
    }
    throw new IllegalArgumentException("shouldnt be doing this on a gram that doesn't contain a partial");
  }

  private int findLastIndexPartialInGram(String gram) {
    if (GramBuilder.isUnaryGram(gram)) {
      return 0;
    }
    int last = -1;
    int idx = 0;
    for (String s : GramBuilder.SPLITTER.split(gram)) {
      if (PartialPhones.isPartialPhone(s)) {
        last = idx;
      }
      idx += 1;
    }
    Preconditions.checkState(last >= 0, "shouldnt ne doing this on a gram that doesnt contain a partial");
    return last;
  }
}
