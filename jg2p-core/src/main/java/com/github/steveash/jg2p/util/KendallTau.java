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


import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Calculates the KendallTau-b coefficient of a list of elements that have two different orderings A and B. Each
 * elements is given by Pair<A, B> and the return value is oin [-1, +1] where -1 means this is the exact opposite order
 * and +1 means exact same order
 */
public class KendallTau {

  private static final Comparator<Pair<Comparable<?>, Comparable<?>>>
      byA =
      new Comparator<Pair<Comparable<?>, Comparable<?>>>() {
        @Override
        public int compare(Pair<Comparable<?>, Comparable<?>> left, Pair<Comparable<?>, Comparable<?>> right
        ) {
          return ((Comparable) left.getLeft()).compareTo(((Comparable) right.getLeft()));
        }
      };
  private static final Comparator<Pair<Comparable<?>, Comparable<?>>>
      byB =
      new Comparator<Pair<Comparable<?>, Comparable<?>>>() {
        @Override
        public int compare(Pair<Comparable<?>, Comparable<?>> left, Pair<Comparable<?>, Comparable<?>> right
        ) {
          return ((Comparable) left.getRight()).compareTo(((Comparable) right.getRight()));
        }
      };
  private static final Comparator<Pair<Comparable<?>, Comparable<?>>> byAThenB = Ordering.from(byA).compound(byB);

  public static double calculate(Iterable<? extends Pair<Comparable<?>, Comparable<?>>> elements) {
    List<Pair<Comparable<?>, Comparable<?>>> sortedByA = Lists.newArrayList(elements);
    Collections.sort(sortedByA, byAThenB);
    if (sortedByA.size() == 0) {
      return 0.0;
    }
    if (sortedByA.size() == 1) {
      return 1.0;
    }

    // we're going to do a merge sort and keep track of the numbers of "swaps" we have to do
    AtomicLong swapCount = new AtomicLong(0); // just using this as a box to pass around
    int mid = sortedByA.size() / 2;
    List<Pair<Comparable<?>, Comparable<?>>> sortedByB = mergeSort(sortedByA.subList(0, mid),
                                                                   sortedByA.subList(mid, sortedByA.size()), byB,
                                                                   swapCount
    );

    long n = sortedByA.size();
    long pairs = pairs(n);
    long tiePairsLeft = countPairs(sortedByA, byA);
    long tiePairsRight = countPairs(sortedByB, byB);
    long tiePairsBoth = countPairs(sortedByA, byAThenB);
    long num = pairs - tiePairsLeft - tiePairsRight + tiePairsBoth - (2 * swapCount.longValue());
    double den = Math.sqrt((pairs - tiePairsLeft) * (pairs - tiePairsRight));
    double coeff = ((double) num) / den;
    return coeff;
  }

  private static long pairs(long n) {
    return n * (n - 1) / 2;
  }

  private static long countPairs(List<Pair<Comparable<?>, Comparable<?>>> sorted,
                                 Comparator<Pair<Comparable<?>, Comparable<?>>> comp
  ) {
    GroupCounter counter = new GroupCounter(sorted, comp);
    long total = 0;
    while (counter.hasNext()) {
      int count = counter.next();
      if (count == 1) {
        continue;
      }
      total += pairs(count);
    }
    return total;
  }

  private static <T> List<T> mergeSort(List<T> left, List<T> right, Comparator<T> comp, AtomicLong swapCount
  ) {

    if (left.size() > 1) {
      int mid = left.size() / 2;
      left = mergeSort(left.subList(0, mid), left.subList(mid, left.size()), comp, swapCount);
    }
    if (right.size() > 1) {
      int mid = right.size() / 2;
      right = mergeSort(right.subList(0, mid), right.subList(mid, right.size()), comp, swapCount);
    }
    ArrayList<T> merged = Lists.newArrayListWithCapacity(left.size() + right.size());
    int i = 0;
    int j = 0;
    while (i < left.size() && j < right.size()) {
      T ll = left.get(i);
      T rr = right.get(j);
      if (comp.compare(ll, rr) > 0) {
        merged.add(rr);
        j += 1;
        swapCount.addAndGet(left.size() - i);
      } else {
        merged.add(ll);
        i += 1;
      }
    }
    drainTo(merged, left, i);
    drainTo(merged, right, j);
    return merged;
  }

  private static <T> void drainTo(ArrayList<T> merged, List<T> source, int index) {
    for (int i = index; i < source.size(); i++) {
      merged.add(source.get(i));
    }
  }

  static class GroupCounter extends AbstractIterator<Integer> {

    private final Iterator iter;
    private final Comparator comp;
    private Object last = null;
    private int count = 0;

    private <T> GroupCounter(List<T> objs, Comparator<T> comp) {
      this.iter = objs.iterator();
      this.comp = comp;
    }

    @Override
    protected Integer computeNext() {
      while (iter.hasNext()) {
        Object next = checkNotNull(iter.next());
        if (last == null) {
          last = next;
          count += 1;
          continue;
        }
        if (comp.compare(last, next) != 0) {
          int toReturn = count;
          this.last = next;
          count = 1;
          return toReturn;
        } else {
          count += 1;
        }
      }
      if (count > 0) {
        int toReturn = count;
        count = 0;
        return toReturn;
      }
      return endOfData();
    }
  }
}

