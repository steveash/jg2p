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

import com.google.common.base.Preconditions;

import java.util.List;

import javax.annotation.Nullable;

import cc.mallet.types.Token;

/**
 * @author Steve Ash
 */
public class TokenSeqUtil {

  @Nullable
  public static String getWindow(List<Token> ts, int current, int windowOffset, int windexWidth) {
    if (windowOffset < 0) {
      return getBakwardWindow(ts, current, windowOffset, windexWidth);
    }
    return getForwardWindow(ts, current, windowOffset, windexWidth);
  }

  private static String getBakwardWindow(List<Token> ts, int current, int windowOffset, int windexWidth) {
    Preconditions.checkArgument(windowOffset < 0);
    Preconditions.checkArgument(windowOffset + windexWidth <= 0);

    int start = -(windowOffset + windexWidth);

    StringBuilder sb = new StringBuilder(windexWidth);
    int strIndex = -1;
    String str = "";
    int eaten = 0;
    while (true) {
      if (strIndex < 0) {
        findnext: while (true) {
          current -= 1;
          if (current < 0) {
            return null; // ran out of chars to eat
          }
          str = ts.get(current).getText();
          if (str.length() > 0) {
            break findnext;
          }
        }
        strIndex = str.length() - 1;
      }

      if (eaten >= start) {
        char c = str.charAt(strIndex);
        sb.append(c);
        if (sb.length() == windexWidth) {
          return sb.reverse().toString();
        }
      }
      strIndex -= 1;
      eaten += 1;
    }
  }

  private static String getForwardWindow(List<Token> ts, int current, int windowOffset, int windexWidth) {
    Preconditions.checkArgument(windowOffset > 0);
    Preconditions.checkArgument(windowOffset + windexWidth > 0);

    int start = windowOffset - 1; // we're starting one character over from us, to be symmetric needs to be shifted

    StringBuilder sb = new StringBuilder(windexWidth);
    int strIndex = 1;
    String str = "";
    int eaten = 0;
    while (true) {
      if (strIndex >= str.length()) {
        findnext: while (true) {
          current += 1;
          if (current > (ts.size() - 1)) {
            return null; // ran out of chars to eat
          }
          str = ts.get(current).getText();
          if (str.length() > 0) {
            break findnext;
          }
        }
        strIndex = 0;
      }

      if (eaten >= start) {
        char c = str.charAt(strIndex);
        sb.append(c);
        if (sb.length() == windexWidth) {
          return sb.toString();
        }
      }
      strIndex += 1;
      eaten += 1;
    }
  }
}
