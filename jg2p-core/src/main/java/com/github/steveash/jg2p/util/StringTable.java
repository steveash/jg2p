/*
 * Copyright 2016 Steve Ash
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

/**
 * @author Steve Ash
 */
public class StringTable {

  private final String[] table;
  private final int rowCount;
  private final int colCount;

  public StringTable(int rowCount, int colCount) {
    this.rowCount = rowCount;
    this.colCount = colCount;
    this.table = new String[rowCount * colCount];
  }

  public void set(int row, int col, String value) {
    Preconditions.checkArgument(row < rowCount);
    Preconditions.checkArgument(col < colCount);
    table[cellFor(row, col)] = value;
  }

  public String get(int row, int col) {
    Preconditions.checkArgument(row < rowCount);
    Preconditions.checkArgument(col < colCount);
    return table[cellFor(row, col)];
  }

  private int cellFor(int row, int col) {
    return (row * colCount) + col;
  }
}
