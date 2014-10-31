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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import com.carrotsearch.hppc.ObjectDoubleMap;
import com.carrotsearch.hppc.ObjectDoubleOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectDoubleCursor;
import com.github.steveash.jg2p.util.Assert;

import java.util.Iterator;

import static com.github.steveash.jg2p.util.Assert.assertProb;
import static com.google.common.base.Preconditions.checkState;

/**
 * Table of probabilities from Xi to Yi
 *
 * @author Steve Ash
 */
public class ProbTable implements Iterable<Table.Cell<String,String,Double>> {

  @Override
  public Iterator<Table.Cell<String, String, Double>> iterator() {
    return xyProb.cellSet().iterator();
  }

  public static class Marginals {
    private final ObjectDoubleMap<String> xMarginals;
    private final ObjectDoubleMap<String> yMarginals;
    private final double sumJointMass; // sum of all probability mass across all X x Y joint distrib

    Marginals(ObjectDoubleMap<String> xMarginals, ObjectDoubleMap<String> yMarginals, double sumJointMass) {
      this.xMarginals = xMarginals;
      this.yMarginals = yMarginals;
      this.sumJointMass = sumJointMass;
    }

    public double probY(String y) {
      return assertProb(yMarginals.getOrDefault(y, -1));
    }

    public double probX(String x) {
      return assertProb(xMarginals.getOrDefault(x, -1));
    }

    public int countY() {
      return yMarginals.size();
    }

    public int countX() {
      return xMarginals.size();
    }

    public double sumOfAllJointProbabilities() {
      return sumJointMass;
    }
  }

  private final Table<String, String, Double> xyProb = HashBasedTable.create();

  public double prob(String x, String y) {
    Double maybe = xyProb.get(x, y);
    if (maybe == null) {
      return 0;
    }
    return maybe;
  }

  public void clear() { xyProb.clear(); }

  public void setProb(String x, String y, double value) {
    xyProb.put(x, y, value);
  }

  public void addProb(String x, String y, double valueToAdd) {
    Double maybe = xyProb.get(x, y);
    if (maybe == null) maybe = 0.0;
    xyProb.put(x, y, maybe + valueToAdd);
  }

  public long entryCount() { return xyProb.size(); }

  public Marginals calculateMarginals() {
    ObjectDoubleOpenHashMap<String> x = ObjectDoubleOpenHashMap.newInstance();
    ObjectDoubleOpenHashMap<String> y = ObjectDoubleOpenHashMap.newInstance();
    double sum = 0;
    for (Table.Cell<String, String, Double> cell : xyProb.cellSet()) {
      x.putOrAdd(cell.getRowKey(), cell.getValue(), cell.getValue());
      y.putOrAdd(cell.getColumnKey(), cell.getValue(), cell.getValue());
      sum += cell.getValue();
    }
    return new Marginals(x, y, sum);
  }

}
