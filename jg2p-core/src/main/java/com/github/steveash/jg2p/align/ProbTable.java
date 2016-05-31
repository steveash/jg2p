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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import com.carrotsearch.hppc.ObjectDoubleMap;
import com.carrotsearch.hppc.ObjectDoubleOpenHashMap;

import org.apache.commons.lang3.tuple.Pair;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.github.steveash.jg2p.util.Assert.assertProb;

/**
 * Table of probabilities from Xi to Yi
 *
 * @author Steve Ash
 */
public class ProbTable implements Iterable<Table.Cell<String,String,Double>>, Externalizable {
  private static final long serialVersionUID = -8001165446102770332L;
  public static final double minLogProb = -1e12;

  /**
   * Returns a set of all non-empty x,y pairs from a unioned with all non-empty x,y pairs from b
   * @param a
   * @param b
   * @return
   */
  public static Set<Pair<String, String>> unionOfAllCells(ProbTable a, ProbTable b) {
    Set<Pair<String, String>> xys = Sets.newHashSetWithExpectedSize(Math.max(a.xyProb.size(), b.xyProb.size()));
    addAllPresent(a, xys);
    addAllPresent(b, xys);
    return xys;
  }

  public static void mergeAll(Iterable<ProbTable> sources, ProbTable sink) {
    for (ProbTable source : sources) {
      for (Table.Cell<String, String, Double> cell : source) {
        sink.addProb(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
      }
    }
  }

  private static void addAllPresent(ProbTable tbl, Set<Pair<String, String>> output) {
    for (Table.Cell<String, String, Double> aa : tbl) {
      if (aa.getValue() != null && aa.getValue() > 0) {
        output.add(Pair.of(aa.getRowKey(), aa.getColumnKey()));
      }
    }
  }

  @Override
  public Iterator<Table.Cell<String, String, Double>> iterator() {
    return xyProb.cellSet().iterator();
  }

  public Map<String, Double> getYProbForX(String x) {
    return xyProb.row(x);
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
      return assertProb(xMarginals.getOrDefault(x, 0));
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

  private /*final*/ Table<String, String, Double> xyProb = HashBasedTable.create();

  public ProbTable() {
  }

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

  public ProbTable makeNormalizedCopy() {
    ProbTable result = new ProbTable();
    Marginals marginals = this.calculateMarginals();
    double sum = marginals.sumOfAllJointProbabilities();
    for (Table.Cell<String, String, Double> cell : this) {
      double normalValue = cell.getValue() / sum;
      result.setProb(cell.getRowKey(), cell.getColumnKey(), normalValue);
    }
    return result;
  }

  /**
   * Returns a new table where each row's values are replaced so that the sum of all
   * Y's for the row equal 1
   * @return
   */
  public ProbTable makeRowNormalizedCopy() {
    ProbTable result = new ProbTable();
    for (String row : xRows()) {
      Map<String, Double> yProbs = getYProbForX(row);
      double total = 0;
      for (Double dbl : yProbs.values()) {
        total += dbl;
      }
      if (total != 0.0) {
        for (String y : yProbs.keySet()) {
          Double maybe = yProbs.get(y);
          if (maybe == null) {
            maybe = 0.0;
          }
          result.setProb(row, y, maybe / total);
        }
      }
    }
    return result;
  }

  public Set<String> xRows() {
    return xyProb.rowKeySet();
  }

  public Set<String> yCols() {
    return xyProb.columnKeySet();
  }

  @Override
   public void writeExternal(ObjectOutput out) throws IOException {
     out.writeObject(this.xyProb);
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.xyProb = (Table<String, String, Double>) in.readObject();
   }
}
