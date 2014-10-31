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

import com.google.common.collect.Table;

/**
 * Abstraction that knows how to maximize in the M step of EM
 *
 * @author Steve Ash
 */
public enum Maximizer {

  COND_X_GIVEN_Y(new Func() {
    @Override
    public double maximize(Table.Cell<String, String, Double> xyCell, ProbTable.Marginals marginals) {
      return xyCell.getValue() / marginals.probY(xyCell.getColumnKey());
    }

    @Override
    public double normalize(double value, ProbTable.Marginals marginals) {
      return value / marginals.countY();
    }
  }),

  COND_Y_GIVEN_X(new Func() {
    @Override
    public double maximize(Table.Cell<String, String, Double> xyCell, ProbTable.Marginals marginals) {
      return xyCell.getValue() / marginals.probX(xyCell.getRowKey());
    }

    @Override
    public double normalize(double value, ProbTable.Marginals marginals) {
      return value / marginals.countX();
    }
  }),

  JOINT(new Func() {
    @Override
    public double maximize(Table.Cell<String, String, Double> xyCell, ProbTable.Marginals marginals) {
      return xyCell.getValue() / marginals.sumOfAllJointProbabilities();
    }

    @Override
    public double normalize(double value, ProbTable.Marginals marginals) {
      return value;
    }
  });

  private static interface Func {

    double maximize(Table.Cell<String, String, Double> xyCell, ProbTable.Marginals marginals);

    double normalize(double value, ProbTable.Marginals marginals);
  }

  private final Func func;

  Maximizer(Func func) {
    this.func = func;
  }

  public double maximize(Table.Cell<String,String,Double> cell, ProbTable.Marginals marginals) {
    return func.maximize(cell, marginals);
  }

  public double normalize(double value, ProbTable.Marginals marginals) {
    return func.normalize(value, marginals);
  }
}
