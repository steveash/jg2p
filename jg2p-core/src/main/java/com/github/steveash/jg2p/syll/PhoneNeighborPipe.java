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

package com.github.steveash.jg2p.syll;

import com.google.common.base.Joiner;

import com.github.steveash.jg2p.seq.NeighborTokenFeature;
import com.github.steveash.jg2p.seq.TokenWindow;

import java.util.List;

/**
 * @author Steve Ash
 */
public class PhoneNeighborPipe extends NeighborTokenFeature {
  private static final long serialVersionUID = -3192955985141276670L;

  private static final Joiner joiner = Joiner.on('_');

  public PhoneNeighborPipe(boolean includeCurrent, List<TokenWindow> windows) {
    super(includeCurrent, windows);
  }

  @Override
  protected String getWindow(List<String> ts, int i, TokenWindow window) {
    // we don't use the super window because it assumes that the units that we want to
    // use to make a window are individual letters. but for phonemes there are multi-letter
    // codes
    int start = i + window.offset;
    if (start < 0) return null;
    int end = start + window.width;
    if (end > ts.size()) return null;
    return joiner.join(ts.subList(start, end));
  }
}
