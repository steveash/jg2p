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

import com.google.common.collect.ImmutableList;

import java.io.Serializable;

/**
* @author Steve Ash
*/
public class TokenWindow implements Serializable {

  public final int offset;
  public final int width;

  public TokenWindow(int offset, int width) {
    this.offset = offset;
    this.width = width;
  }

  public static ImmutableList<TokenWindow> makeTokenWindowsForInts(int[] neighbors) {
    ImmutableList.Builder<TokenWindow> builder = ImmutableList.builder();
    for (int i = 0; i < neighbors.length; i++) {
      builder.add(new TokenWindow(neighbors[i], 1));
    }
    return builder.build();
  }
}
