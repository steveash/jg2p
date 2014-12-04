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
import com.google.common.base.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Steve Ash
 */
public class Funcs {

  private static final Function selectLeft = new Function<Pair<?,?>, Object>() {
    @Override
    public Object apply(Pair<?, ?> input) {
      return input.getLeft();
    }
  };

  private static final Function selectRight = new Function<Pair<?,?>, Object>() {
    @Override
    public Object apply(Pair<?, ?> input) {
      return input.getRight();
    }
  };

  private static final Predicate<String> onlyNonBlank = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return StringUtils.isNotBlank(input);
    }
  };

  @SuppressWarnings("unchecked")
  public static <I,O> Function<Pair<I,O>, I> selectLeft() {
    return selectLeft;
  }

  @SuppressWarnings("unchecked")
  public static <I,O> Function<Pair<I,O>, O> selectRight() {
    return selectRight;
  }

  public static Predicate<String> onlyNonBlank() {
    return onlyNonBlank;
  }

  public static Function<String, String> transformBlanks(final String blanksBecome) {
    return new Function<String, String>() {
      @Nullable
      @Override
      public String apply(String input) {
        if (isBlank(input)) {
          return blanksBecome;
        }
        return input;
      }
    };
  }
}
