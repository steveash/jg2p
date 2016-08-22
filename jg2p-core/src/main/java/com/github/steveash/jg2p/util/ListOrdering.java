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

package com.github.steveash.jg2p.util;

import com.google.common.collect.Ordering;

import java.util.List;

/**
 * A list ordering that gives a total ordering by element wise ordering
 * @author Steve Ash
 */
public class ListOrdering<T extends Comparable<T>> extends Ordering<List<T>> {

  private static final ListOrdering instance = new ListOrdering();

  public static <T extends Comparable<T>> ListOrdering<T> getInstance() {
    return instance;
  }

  public static <T extends Comparable<T>> ListOrdering<T> getInstance(Class<T> clazz) {
    return instance;
  }

  @Override
  public int compare(List<T> left, List<T> right) {
    int size = Math.min(left.size(), right.size());
    for (int i = 0; i < size; i++) {
      int element = Ordering.natural().compare(left.get(i), right.get(i));
      if (element == 0) continue;
      return element;
    }
    return Integer.compare(left.size(), right.size());
  }
}
