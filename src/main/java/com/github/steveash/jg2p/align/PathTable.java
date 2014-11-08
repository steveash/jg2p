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
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;

import com.github.steveash.jg2p.util.MinHeap;
import com.github.steveash.jg2p.util.ObjectTable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * dynamic programming table to assist calculating the viterbi sequence
 * @author Steve Ash
 */
public class PathTable {

  private final ObjectTable<MinHeap<Entry>> table;
  private final AtomicInteger ids = new AtomicInteger(0);

  public static class Entry implements Comparable<Entry> {

    public static Entry sample(double score, int xBackRef, int yBackRef) {
      return new Entry(-1, score, xBackRef, yBackRef, -1);
    }

    public final int id;
    public double score;
    public int xBackRef;
    public int yBackRef;
    public int pathBackRef;

    private Entry(int id, double score, int xBackRef, int yBackRef, int pathBackRef) {
      this.id = id;
      this.score = score;
      this.xBackRef = xBackRef;
      this.yBackRef = yBackRef;
      this.pathBackRef = pathBackRef;
    }

    @Override
    public int compareTo(Entry that) {
      return ComparisonChain.start()
          .compare(this.score, that.score)
          .compare(this.xBackRef, that.xBackRef)
          .compare(this.yBackRef, that.yBackRef)
          .compare(this.pathBackRef, that.pathBackRef)
          .result();
    }
  }


  public PathTable(int xSize, int ySize, int maxBestPath) {
    this.table = new ObjectTable<>(xSize, ySize);
    init(xSize, ySize, maxBestPath);
  }

  public Entry make(double score, int xBackRef, int yBackRef, int pathBackRef) {
    return new Entry(ids.getAndIncrement(), score, xBackRef, yBackRef, pathBackRef);
  }

  public void offer(int x, int y, Entry candidate) {
    MinHeap<Entry> paths = table.get(x, y);
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
   * @param newX
   * @param newY
   *@param sample  @return
   */
  public void extendPath(int newX, int newY, int oldX, int oldY, final Entry sample) {
    Iterable<Entry> newEntries = Iterables.transform(table.get(oldX, oldY), new Function<Entry, Entry>() {
      @Override
      public Entry apply(Entry input) {
        return make(input.score + sample.score, sample.xBackRef, sample.yBackRef, input.pathBackRef);
      }
    });

    for (Entry newEntry : newEntries) {
      this.offer(newX, newY, newEntry);
    }
  }

  public Iterable<Entry> get(int x, int y) {
    return table.get(x, y);
  }

  public Entry get(int x, int y, int pathBackRef) {
    for (Entry candidate : get(x, y)) {
      if (candidate.pathBackRef == pathBackRef) {
        return candidate;
      }
    }
    throw new IllegalStateException("Shouldn't be possible but somehow couldnt find " + pathBackRef);
  }

  private void init(int xSize, int ySize, int pathSize) {
    for (int i = 0; i < xSize; i++) {
      for (int j = 0; j < ySize; j++) {
        table.put(i, j, new MinHeap<Entry>(pathSize));
      }
    }
  }
}
