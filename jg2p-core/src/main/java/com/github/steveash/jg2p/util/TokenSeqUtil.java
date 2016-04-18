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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.phoseq.Graphemes;

import java.util.List;

import javax.annotation.Nullable;

import cc.mallet.types.Token;

import static com.google.common.base.CharMatcher.WHITESPACE;

/**
 * @author Steve Ash
 */
public class TokenSeqUtil {

  public static int countBefore(List<String> grams, int current) {
    int count = 0;
    for (int i = 0; i < grams.size() && i < current; i++) {
      count += Grams.countInGram(grams.get(i));
    }
    return count;
  }

  public static int countAfter(List<String> grams, int current) {
    int count = 0;
    for (int i = current + 1; i < grams.size(); i++) {
      count += Grams.countInGram(grams.get(i));
    }
    return count;
  }

  @Nullable
  public static String getWindow(List<Token> ts, int current, int windowOffset, int windowWidth) {
    List<String> ss = Lists.transform(ts, tokenToString);
    return getWindowFromStrings(ss, current, windowOffset, windowWidth);
  }

  // this skips over spaces so it doesn't matter if gram strings are space separated or not
  public static String getWindowFromStrings(List<String> ts, int current, int windowOffset, int windowWidth) {
    if (windowOffset < 0) {
      return getBakwardWindowFromString(ts, current, windowOffset, windowWidth);
    }
    return getForwardWindowFromString(ts, current, windowOffset, windowWidth);
  }

  private static String getBakwardWindowFromString(List<String> ts, int current, int windowOffset, int windowWidth) {
    Preconditions.checkArgument(windowOffset < 0);
    Preconditions.checkArgument(windowOffset + windowWidth <= 0);

    int start = -(windowOffset + windowWidth);

    StringBuilder sb = new StringBuilder(windowWidth);
    int strIndex = -1;
    String str = "";
    int eaten = 0;
    while (true) {
      if (strIndex < 0) {
        findnext:
        while (true) {
          current -= 1;
          if (current < 0) {
            return null; // ran out of chars to eat
          }
          str = ts.get(current);
          if (str.length() > 0) {
            break findnext;
          }
        }
        strIndex = str.length() - 1;
      }

      char c = str.charAt(strIndex);
      strIndex -= 1;
      if (c == ' ') continue;

      if (eaten >= start) {
        sb.append(c);
        if (sb.length() == windowWidth) {
          return sb.reverse().toString();
        }
      }
      eaten += 1;
    }
  }

  private static String getForwardWindowFromString(List<String> ts, int current, int windowOffset, int windowWidth) {
    Preconditions.checkArgument(windowOffset > 0);
    Preconditions.checkArgument(windowOffset + windowWidth > 0);

    int start = windowOffset - 1; // we're starting one character over from us, to be symmetric needs to be shifted

    StringBuilder sb = new StringBuilder(windowWidth);
    int strIndex = 1;
    String str = "";
    int eaten = 0;
    while (true) {
      if (strIndex >= str.length()) {
        findnext:
        while (true) {
          current += 1;
          if (current > (ts.size() - 1)) {
            return null; // ran out of chars to eat
          }
          str = ts.get(current);
          if (str.length() > 0) {
            break findnext;
          }
        }
        strIndex = 0;
      }

      char c = str.charAt(strIndex);
      strIndex += 1;
      if (c == ' ') continue;

      if (eaten >= start) {
        sb.append(c);
        if (sb.length() == windowWidth) {
          return sb.toString();
        }
      }
      eaten += 1;
    }
  }

  public static final Function<Token, String> tokenToString = new Function<Token, String>() {
    @Override
    public String apply(Token input) {
      return input.getText();
    }
  };

  public static String convertShape(String winStr) {
    if (winStr == null) return null;
    StringBuilder sb = new StringBuilder(winStr.length());
    for (int i = 0; i < winStr.length(); i++) {
      char c = winStr.charAt(i);
      if (Graphemes.consonants.matches(c)) {
        sb.append('c');
      } else if (Graphemes.vowels.matches(c)) {
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
