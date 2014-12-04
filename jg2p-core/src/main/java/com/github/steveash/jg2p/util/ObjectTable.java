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

import java.util.Arrays;

/**
 * A 2d table (row x value) that is implemented as a single array which can be better depending on access patterns
 *
 * @author Steve Ash
 */
public class ObjectTable<T> {

  private final Object[] table;
  private int xSize;
  private int ySize;

  public ObjectTable(int maxX, int maxY) {
    this.table = new Object[maxX * maxY];
    initAndNullOut(maxX, maxY);
  }

  private void initAndNullOut(int xSize, int ySize) {
    setNewSize(xSize, ySize);
    int size = xSize * ySize;
    if (size > table.length) {
      throw new IllegalArgumentException("ObjectTable doesnt have enough buffer space");
    }

    Arrays.fill(table, 0, size, null);
  }

  public void setNewSize(int xSize, int ySize) {
    this.xSize = xSize;
    this.ySize = ySize;
  }

  @SuppressWarnings("unchecked")
  public T get(int x, int y) {
    int slot = index(x, y);
    return (T) table[slot];
  }

  public void put(int x, int y, T value) {
    int slot = index(x, y);
    table[slot] = value;
  }

  private int index(int x, int y) {
    return (x * ySize) + y;
  }
}
