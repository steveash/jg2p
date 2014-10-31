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

package com.github.steveash.jg2p;

import java.util.Arrays;

/**
 * a 2d array of double primitives instead of double the java array of array's thing
 * @author Steve Ash
 */
public class DoubleTable {
  private final double[] table;
  private int xSize = -1;
  private int ySize = -1;

  public DoubleTable(int maxX, int maxY) {
    this.table = new double[maxX * maxY];
    init(maxX, maxY);
  }

  public void init(int xSize, int ySize) {
    this.xSize = xSize;
    this.ySize = ySize;
    int size = xSize * ySize;
    if (size > table.length) {
      throw new IllegalArgumentException("DoubleTable doesnt have enough buffer space");
    }

    Arrays.fill(table, 0, size, 0);
  }

  public double get(int x, int y) {
    int slot = index(x, y);
    return table[slot];
  }

  public void put(int x, int y, double value) {
    int slot = index(x, y);
    table[slot] = value;
  }

  public void add(int x, int y, double deltaToAdd) {
    int slot = index(x, y);
    table[slot] += deltaToAdd;
  }

  private int index(int x, int y) {
    return (x * ySize) + y;
  }
}
