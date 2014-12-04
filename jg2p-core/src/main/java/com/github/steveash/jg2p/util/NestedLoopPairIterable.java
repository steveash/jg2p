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

import com.google.common.collect.AbstractIterator;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;

/**
 * Allows you to lazily do a nested loop of P x Q where P is the outer and Q is the inner
 *
 * @author Steve Ash
 */
public class NestedLoopPairIterable<P, Q> implements Iterable<Pair<P, Q>> {

  public static <P, Q> NestedLoopPairIterable<P, Q> of(Iterable<P> outerSource, Iterable<Q> innerSource) {
    return new NestedLoopPairIterable<>(outerSource, innerSource);
  }

  private final Iterable<P> outerSource;
  private final Iterable<Q> innerSource;

  private NestedLoopPairIterable(Iterable<P> outerSource, Iterable<Q> innerSource) {
    this.outerSource = outerSource;
    this.innerSource = innerSource;
  }

  @Override
  public Iterator<Pair<P, Q>> iterator() {
    return new AbstractIterator<Pair<P, Q>>() {

      private final Iterator<P> outer = outerSource.iterator();
      private Iterator<Q> inner = innerSource.iterator();

      private P lastOuter;
      private boolean hasAtLeastOne = false;

      {
        // setup for the initial state
        if (outer.hasNext() && inner.hasNext()) {
          lastOuter = outer.next();
          hasAtLeastOne = true;
        }
      }

      @Override
      protected Pair<P, Q> computeNext() {
        if (!hasAtLeastOne) {
          return endOfData();
        }

        if (!inner.hasNext()) {
          if (!outer.hasNext()) {
            return endOfData();
          }

          lastOuter = outer.next();
          inner = innerSource.iterator();
        }
        return Pair.of(lastOuter, inner.next());
      }
    };
  }
}
