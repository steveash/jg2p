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

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Iterator;

import static com.google.common.collect.Iterables.size;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NestedLoopPairIterableTest {

  @Test
  public void shouldReturnEmptyIfInnerIsEmpty() throws Exception {
    ImmutableList<String> a = ImmutableList.of("a", "b");
    ImmutableList<Integer> b = ImmutableList.of();
    NestedLoopPairIterable<String, Integer> ible = NestedLoopPairIterable.of(a, b);

    assertEquals(0, size(ible));
  }

  @Test
  public void shouldReturnEmptyIfOuterIsEmpty() throws Exception {
    ImmutableList<Integer> a = ImmutableList.of();
    ImmutableList<String> b = ImmutableList.of("a", "b");
    NestedLoopPairIterable<Integer, String> ible = NestedLoopPairIterable.of(a, b);

    assertEquals(0, size(ible));
  }

  @Test
  public void shouldReturnEmptyIfBothAreEmpty() throws Exception {
    ImmutableList<Integer> a = ImmutableList.of();
    ImmutableList<String> b = ImmutableList.of();
    NestedLoopPairIterable<Integer, String> ible = NestedLoopPairIterable.of(a, b);

    assertEquals(0, size(ible));
  }

  @Test
  public void shouldWorkIfInnerIsOnlyOne() throws Exception {
    ImmutableList<String> a = ImmutableList.of("a", "b");
    ImmutableList<Integer> b = ImmutableList.of(1);
    NestedLoopPairIterable<String, Integer> ible = NestedLoopPairIterable.of(a, b);
    Iterator<Pair<String, Integer>> iter = ible.iterator();

    assertEquals(2, size(ible));
    assertEquals(Pair.of("a", 1), iter.next());
    assertEquals(Pair.of("b", 1), iter.next());

    assertFalse(iter.hasNext());
  }

  @Test
  public void shouldWorkIfOuterIsOnlyOne() throws Exception {
    ImmutableList<Integer> a = ImmutableList.of(1);
    ImmutableList<String> b = ImmutableList.of("a", "b");
    NestedLoopPairIterable<Integer, String> ible = NestedLoopPairIterable.of(a, b);
    Iterator<Pair<Integer, String>> iter = ible.iterator();

    assertEquals(2, size(ible));
    assertEquals(Pair.of(1, "a"), iter.next());
    assertEquals(Pair.of(1, "b"), iter.next());

    assertFalse(iter.hasNext());
  }

  @Test
  public void shouldWorkIfBothAreOnlyOne() throws Exception {
    ImmutableList<Integer> a = ImmutableList.of(1);
    ImmutableList<String> b = ImmutableList.of("a");
    NestedLoopPairIterable<Integer, String> ible = NestedLoopPairIterable.of(a, b);
    Iterator<Pair<Integer, String>> iter = ible.iterator();

    assertEquals(1, size(ible));
    assertEquals(Pair.of(1, "a"), iter.next());

    assertFalse(iter.hasNext());
  }

  @Test
  public void shouldReturnGoodIterableForNormalInput() throws Exception {
    ImmutableList<Integer> a = ImmutableList.of(1, 2, 3, 4, 5);
    ImmutableList<String> b = ImmutableList.of("a", "b", "c", "d");
    NestedLoopPairIterable<Integer, String> ible = NestedLoopPairIterable.of(a, b);

    Iterator<Pair<Integer, String>> iter = ible.iterator();
    assertEquals(Pair.of(1, "a"), iter.next());
    assertEquals(Pair.of(1, "b"), iter.next());
    assertEquals(Pair.of(1, "c"), iter.next());
    assertEquals(Pair.of(1, "d"), iter.next());

    assertEquals(Pair.of(2, "a"), iter.next());
    assertEquals(Pair.of(2, "b"), iter.next());
    assertEquals(Pair.of(2, "c"), iter.next());
    assertEquals(Pair.of(2, "d"), iter.next());

    assertEquals(Pair.of(3, "a"), iter.next());
    assertEquals(Pair.of(3, "b"), iter.next());
    assertEquals(Pair.of(3, "c"), iter.next());
    assertEquals(Pair.of(3, "d"), iter.next());

    assertEquals(Pair.of(4, "a"), iter.next());
    assertEquals(Pair.of(4, "b"), iter.next());
    assertEquals(Pair.of(4, "c"), iter.next());
    assertEquals(Pair.of(4, "d"), iter.next());

    assertEquals(Pair.of(5, "a"), iter.next());
    assertEquals(Pair.of(5, "b"), iter.next());
    assertEquals(Pair.of(5, "c"), iter.next());
    assertEquals(Pair.of(5, "d"), iter.next());

    assertFalse(iter.hasNext());
  }
}