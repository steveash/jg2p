/*
 * Copyright 2015 Steve Ash
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

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.Collections;
import java.util.List;

public class JenksBreaks {

  public static List<Double> computeBreaks(Iterable<Double> inData, int numClasses) {
//        Preconditions.isTrue(data.size() >= numClasses, "Data is too small to split into " + numClasses + " classes");
    List<Double> data = Ordering.natural().sortedCopy(inData);
    int[][] lowerClassLimits = jenksMatrix(data, numClasses);
    return jenksBreaks(data, lowerClassLimits, numClasses);
  }

  public static double goodnessOfFit(Iterable<Double> inData, List<Double> breaks) {
    List<Double> data = Ordering.natural().sortedCopy(inData);

    double sumOfSse = 0;
    int start = 0;
    for (int i = 1; i < breaks.size() - 1; i++) {
      int nextStart = findFirstGt(data, breaks.get(i), start);
      assert nextStart >= 0;
      sumOfSse += sse(data.subList(start, nextStart));
      start = nextStart;
    }
    // last segment
    sumOfSse += sse(data.subList(start, data.size()));

    double overallSse = sse(data);
    return (overallSse - sumOfSse) / overallSse;
  }

  private static int findFirstGt(List<Double> data, double gteValue, int startFrom) {
    for (int i = startFrom; i < data.size(); i++) {
      if (data.get(i) >= gteValue) {
        return i;
      }
    }
    return -1;
  }

  private static double avg(List<Double> data) {
    int count = 0;
    double sum = 0;
    for (Double val : data) {
      count += 1;
      sum += val;
    }
    return sum / count;
  }

  private static double sse(List<Double> data) {
    double mean = avg(data);
    double sum = 0;
    for (Double val : data) {
      sum += ((val - mean) * (val - mean));
    }
    return sum;
  }

  private static List<Double> jenksBreaks(List<Double> data, int[][] lowerClassLimits, int numClasses) {
    int k = data.size() - 1;
    List<Double> kClass = Lists.newArrayList(Collections.nCopies(numClasses + 1, 0D));
    int countNum = numClasses;

    kClass.set(numClasses, data.get(data.size() - 1));
    kClass.set(0, data.get(0));

    while (countNum > 1) {
      kClass.set(countNum - 1, data.get(lowerClassLimits[k][countNum] - 1));
      k = lowerClassLimits[k][countNum] - 1;
      countNum--;
    }

    return kClass;
  }

  private static int[][] jenksMatrix(List<Double> data, int numClasses) {
    int[][] lowerClassLimits = new int[data.size() + 1][numClasses + 1];
    double[][] varianceCombinations = new double[data.size() + 1][numClasses + 1];
    for (int i = 1; i < numClasses + 1; i++) {
      lowerClassLimits[1][i] = 1;
      for (int j = 2; j < data.size() + 1; j++) {
        varianceCombinations[j][i] = Double.MAX_VALUE;
      }
    }

    double variance = 0;
    for (int l = 2; l < data.size() + 1; l++) {
      double sum = 0, sumSq = 0;
      int w = 0, i4;
      for (int m = 1; m < l + 1; m++) {
        int lowerClassLimit = l - m + 1;
        double val = data.get(lowerClassLimit - 1);
        w++;
        sum += val;
        sumSq += val * val;
        variance = sumSq - (sum * sum) / w;

        i4 = lowerClassLimit - 1;
        if (i4 != 0) {
          for (int j = 2; j < numClasses + 1; j++) {
            if (varianceCombinations[l][j] >= (variance + varianceCombinations[i4][j - 1])) {
              lowerClassLimits[l][j] = lowerClassLimit;
              varianceCombinations[l][j] = variance + varianceCombinations[i4][j - 1];
            }
          }
        }
      }

      lowerClassLimits[l][1] = 1;
      varianceCombinations[l][1] = variance;
    }
    return lowerClassLimits;
  }
}
