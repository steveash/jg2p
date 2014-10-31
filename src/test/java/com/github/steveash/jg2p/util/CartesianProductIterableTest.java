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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Iterables.limit;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class CartesianProductIterableTest {

  List<String> source = ImmutableList.of("A", "B", "C", "D");

      @Test
      public void testIteratorEmpty() {
          CartesianProductIterable<String> set = CartesianProductIterable.of(new ArrayList<String>());
          assertFalse(set.iterator().hasNext());
          assertEquals(0, Iterables.size(set));
      }

      @Test
      public void testIteratorOne() {
          CartesianProductIterable<String> set = CartesianProductIterable.of(source.subList(0, 1));
          assertFalse(set.iterator().hasNext());
      }

      @Test
      public void testIteratorTwo() {
          assertThat(CartesianProductIterable.of(limit(source, 2)), contains(
                  Pair.of("A", "B")
          ));
      }

      @Test
      public void testIteratorThree() {
          CartesianProductIterable<String> set = CartesianProductIterable.of(source.subList(0, 3));
          assertThat(set, contains(
                  Pair.of("A", "B"),
                  Pair.of("A", "C"),
                  Pair.of("B", "C")
          ));

          assertEquals(3, Iterables.size(set));
      }

      @SuppressWarnings("unchecked")
      @Test
      public void testIteratorFour() {
          assertThat(CartesianProductIterable.of(limit(source, 4)), contains(
              Pair.of("A", "B"),
              Pair.of("A", "C"),
              Pair.of("A", "D"),
              Pair.of("B", "C"),
              Pair.of("B", "D"),
              Pair.of("C", "D")
          ));
      }
}