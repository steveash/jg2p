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
import com.google.common.collect.AbstractIterator;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Iterator;

import static com.google.common.collect.Iterators.advance;
import static org.apache.commons.math3.util.CombinatoricsUtils.binomialCoefficient;

/**
 * Iterable that returns the cartesian product of another iterable but only the _unique_ pairs and no self-pairs
 * (although it would be simple to support either of those cases) <p> Thus, given an iterable that returns A, B, C, D
 * Then this will return A-B, A-C, A-D, B-C, B-D, C-D <p> Nothing is cached, so the source iterable of size N will be
 * traversed N times
 *
 * @author Steve Ash
 */
public class CartesianProductIterable<T> implements Iterable<Pair<T, T>> {

  public static <T> CartesianProductIterable<T> of(Iterable<T> source) {
    return new CartesianProductIterable<T>(source);
  }

  private final Iterable<T> source;
//  private final long totalCount;

  private CartesianProductIterable(Iterable<T> source) {
    this.source = source;
//    this.totalCount = calculateTotalCount(source);
  }

//  private static <T> long calculateTotalCount(Iterable<T> source) {
//    if (source instanceof Collection) {
//      int inputSize = ((Collection) source).size();
//      if (inputSize >= 2) {
//        return binomialCoefficient(inputSize, 2);
//      }
//
//      return 0;
//    }
//    return -1;
//  }

//  /**
//   * @return total count of pairs that will be returned or -1 if the total is unknown
//   */
//  public long getTotalCount() {
//    return totalCount;
//  }

  @Override
  public Iterator<Pair<T, T>> iterator() {
    return new AbstractIterator<Pair<T, T>>() {

      private Iterator<T> outer = source.iterator();
      private Iterator<T> inner = source.iterator();

      private int nextInnerSkip = 1; // start at one as no self-pairs
      private T lastOuter;

      {
        // setup for the initial state
        if (outer.hasNext()) {
          lastOuter = outer.next();
          positionInnerIterator();
        }
      }

      @Override
      protected Pair<T, T> computeNext() {
        if (!inner.hasNext()) {
          if (!outer.hasNext()) {
            // this can only happen in the empty input case or if we enhance this to do self-pairs
            return endOfData();
          }

          lastOuter = outer.next();
          inner = source.iterator();
          positionInnerIterator();
          // because we don't do self refs we will actually skip ahead to the end of the inner before
          // running out in the outer
          if (!inner.hasNext()) {
            Preconditions.checkState(!outer.hasNext());
            return endOfData();
          }
        }
        Pair<T, T> result = Pair.of(lastOuter, inner.next());
        return result;
      }

      private void positionInnerIterator() {
        // precondition: inner iterator is right before first element
        int actuallySkipped = advance(inner, this.nextInnerSkip);
        Preconditions.checkState(actuallySkipped == this.nextInnerSkip);
        nextInnerSkip += 1;
      }
    };
  }
}
