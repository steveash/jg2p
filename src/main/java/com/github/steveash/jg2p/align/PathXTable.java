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

package com.github.steveash.jg2p.align;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.util.MinHeap;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A memoisation table for calculating the best sequences for a given X.  For the X to Y case see PathXYTable
 *
 * @author Steve Ash
 */
public class PathXTable {

  private final List<MinHeap<Entry>> table;
  private final AtomicInteger ids = new AtomicInteger(0);

  public static class Entry implements Comparable<Entry> {

    public static Entry sample(double score, int xBackRef) {
      return new Entry(-1, score, xBackRef, -1);
    }

    public final int id;
    public double score;
    public int xBackRef;
    public int pathBackRef;

    private Entry(int id, double score, int xBackRef, int pathBackRef) {
      this.id = id;
      this.score = score;
      this.xBackRef = xBackRef;
      this.pathBackRef = pathBackRef;
    }

    @Override
    public int compareTo(Entry that) {
      return ComparisonChain.start()
          .compare(this.score, that.score)
          .compare(this.xBackRef, that.xBackRef)
          .compare(this.pathBackRef, that.pathBackRef)
          .result();
    }

    @Override
    public String toString() {
      return "Entry{" +
             "id=" + id +
             ", score=" + score +
             ", xBackRef=" + xBackRef +
             ", pathBackRef=" + pathBackRef +
             '}';
    }
  }

  public PathXTable(int xSize, int maxBestPath) {
    this.table = Lists.newArrayListWithCapacity(xSize);
    init(xSize, maxBestPath);
  }

  public Entry make(double score, int xBackRef, int pathBackRef) {
    return new Entry(ids.getAndIncrement(), score, xBackRef, pathBackRef);
  }

  public void offer(int x, Entry candidate) {
    MinHeap<Entry> paths = table.get(x);
    if (!paths.isFull()) {
      paths.add(candidate);
      return;
    }
    Entry minEntry = paths.peek();
    if (minEntry.compareTo(candidate) < 0) {
      paths.remove();
      paths.add(candidate);
    }
  }

  /**
   * Extend the paths in the particular x,y location by constructing new ones based on the sample with the addative
   * score and back refs
   *
   * @param sample @return
   */
  public void extendPath(int newX, int oldX, final Entry sample) {
    Iterable<Entry> newEntries = Iterables.transform(table.get(oldX), new Function<Entry, Entry>() {
      @Override
      public Entry apply(Entry input) {
        return make(input.score + sample.score, sample.xBackRef, input.id);
      }
    });

    for (Entry newEntry : newEntries) {
      this.offer(newX, newEntry);
    }
  }

  public Iterable<Entry> get(int x) {
    return table.get(x);
  }

  public Entry get(int x, int id) {
    Preconditions.checkArgument(id >= 0);
    for (Entry candidate : get(x)) {
      if (candidate.id == id) {
        return candidate;
      }
    }
    throw new IllegalStateException("Shouldn't be possible but somehow couldnt find " + id);
  }

  private void init(int xSize, int pathSize) {
    for (int i = 0; i < xSize; i++) {
      table.add(i, new MinHeap<Entry>(pathSize));
    }
  }
}
