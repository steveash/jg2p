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
import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Steve Ash
 */
public class Zipper {

  public static <A,B> List<Pair<A, B>> up(Iterable<A> a, Iterable<B> b) {
    ArrayList<Pair<A, B>> result = Lists.newArrayList();
    Iterator<A> iterA = a.iterator();
    Iterator<B> iterB = b.iterator();
    while (iterA.hasNext()) {
      Preconditions.checkArgument(iterB.hasNext(), "B is shorter than A, must be same size");
      A aa = iterA.next();
      B bb = iterB.next();
      result.add(Pair.of(aa, bb));
    }
    Preconditions.checkArgument(!iterB.hasNext(), "A is shorter than B, must be same size");
    return result;
  }

  public static <A,B> List<Pair<A, B>> upTo(Iterable<A> a, B b) {
    ArrayList<Pair<A,B>> result = Lists.newArrayList();
    for (A aa : a) {
      result.add(Pair.of(aa, b));
    }
    return result;
  }

  public static <A,B> List<Pair<A, B>> upTo(A a, Iterable<B> b) {
    ArrayList<Pair<A,B>> result = Lists.newArrayList();
    for (B bb : b) {
      result.add(Pair.of(a, bb));
    }
    return result;
  }

  public static <A,B> List<Pair<A,B>> replaceRight(List<Pair<A,B>> original, Iterable<B> newRight) {
    ArrayList<Pair<A, B>> result = Lists.newArrayListWithCapacity(original.size());
    Iterator<B> iter = newRight.iterator();
    for (Pair<A, B> pair : original) {
      Preconditions.checkArgument(iter.hasNext(), "newRight is smaller than original");
      result.add(Pair.of(pair.getLeft(), iter.next()));
    }
    Preconditions.checkArgument(!iter.hasNext(), "newRight is bigger than original");
    return result;
  }
}
