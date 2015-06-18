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

package com.github.steveash.jg2p.util;

import com.google.common.base.Splitter;

/**
 * useful for building up gram sequences that are space separated symbols
 * @author Steve Ash
 */
public class GramBuilder {

  public static final Splitter SPLITTER = Splitter.on(" ").trimResults();
  public static final String EPS = "<EPS>";

  private final StringBuilder sb = new StringBuilder();
  private boolean isEmpty = true;

  public static boolean isUnaryGram(String gram) {
    return !gram.contains(" ");
  }

  public void append(String symbol) {
    if (!isEmpty) {
      sb.append(" ");
    } else {
      isEmpty = false;
    }
    sb.append(symbol);
  }

  public String make() {
    return sb.toString();
  }

  @Override
  public String toString() {
    return sb.toString();
  }
}
