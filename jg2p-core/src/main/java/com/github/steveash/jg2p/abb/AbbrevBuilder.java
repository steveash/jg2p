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

package com.github.steveash.jg2p.abb;

import com.github.steveash.jg2p.Grams;

/**
 * @author Steve Ash
 */
public class AbbrevBuilder {

  private final StringBuilder sb = new StringBuilder();
  private String lastGraph = null;
  private final boolean collapseDupGram;

  public AbbrevBuilder(boolean collapseDupGram) {
    this.collapseDupGram = collapseDupGram;
  }

  public AbbrevBuilder append(String gram) {
    if (!collapseDupGram) {
      doAppend(gram.trim());
      return this;
    }
    boolean firstThisTime = true;
    for (String uni : Grams.iterateSymbols(gram)) {
      if (firstThisTime) {
        firstThisTime = false;
        if (lastGraph != null && lastGraph.equalsIgnoreCase(uni)) {
          // we're skipping this one because it matches the last in the last gram written
          continue;
        }
      }
      doAppend(uni);
      lastGraph = uni;
    }
    return this;
  }

  private void doAppend(String gram) {
    if (sb.length() > 0) {
      sb.append(" ");
    }
    sb.append(gram);
  }

  public String build() {
    return sb.toString();
  }

  public void reset() {
    sb.delete(0, sb.length());
    lastGraph = null;
  }

  @Override
  public String toString() {
    return sb.toString();
  }
}
