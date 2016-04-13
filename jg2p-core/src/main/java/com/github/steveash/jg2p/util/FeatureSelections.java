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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.Files;

import com.github.steveash.jg2p.eval.ParallelEval;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

import cc.mallet.fst.CRF;
import cc.mallet.fst.SumLattice;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.LabelVector;
import cc.mallet.types.RankedFeatureVector;

/**
 * @author Steve Ash
 */
public class FeatureSelections {

  private static final Logger log = LoggerFactory.getLogger(FeatureSelections.class);

  /**
   * Returns a ranked feature vector of the gradient gain for the given training data on the given trained CRF.  The
   * instance list must have the target labels as LabelSequence
   */
  public static RankedFeatureVector gradientGainFrom(InstanceList ilist, CRF crf) {
    int numFeatures = ilist.getDataAlphabet().size();
    double[] gradientgains = new double[numFeatures];
    fillResults(ilist, crf, gradientgains, null, null);
    return new RankedFeatureVector(ilist.getDataAlphabet(), gradientgains);
  }

  public static RankedFeatureVector gradientGainRatioFrom(InstanceList ilist, CRF crf) {
    int numFeatures = ilist.getDataAlphabet().size();
    double[] gradientgains = new double[numFeatures];
    double[] gradientlosses = new double[numFeatures];
    fillResults(ilist, crf, null, gradientgains, gradientlosses);

    return makeRatioVector(ilist, numFeatures, gradientgains, gradientlosses);
  }

  public static Pair<RankedFeatureVector, RankedFeatureVector> gradientsFrom(InstanceList ilist, CRF crf) {
    int numFeatures = ilist.getDataAlphabet().size();
    double[] gradientgains = new double[numFeatures];
    double[] pos = new double[numFeatures];
    double[] neg = new double[numFeatures];
    fillResults(ilist, crf, gradientgains, pos, neg);
    return Pair.of(new RankedFeatureVector(ilist.getDataAlphabet(), gradientgains),
                   makeRatioVector(ilist, numFeatures, pos, neg));
  }

  private static RankedFeatureVector makeRatioVector(InstanceList ilist, int numFeatures,
                                                     double[] gradientgains,
                                                     double[] gradientlosses) {
    double[] ratios = new double[numFeatures];
    for (int i = 0; i < numFeatures; i++) {
      double pos = gradientgains[i];
      double neg = gradientlosses[i];
      ratios[i] = (pos + 1.0) / (neg + 1.0);
    }
    return new RankedFeatureVector(ilist.getDataAlphabet(), ratios);
  }

  private static void fillResults(final InstanceList ilist, CRF crf, final double[] abssum, final double[] pos,
                                  final double[] neg) {
    // Populate targetFeatureCount, et al
    final AtomicLong count = new AtomicLong(0);
    new ParallelEval(crf).parallelSum(ilist, new ParallelEval.SumVisitor() {
      @Override
      public void visit(int index, Instance inst, SumLattice lattice) {
        LabelSequence targetSeq = (LabelSequence) inst.getTarget();
        FeatureVectorSequence fvs = (FeatureVectorSequence) inst.getData();
        double instanceWeight = ilist.getInstanceWeight(index);
        Preconditions.checkState(targetSeq.size() == fvs.size(), "input output size diff");
        for (int j = 0; j < targetSeq.size(); j++) {
          // The code below relies on labelWeights summing to 1 over all labels!
          LabelVector predicated = lattice.getLabelingAtPosition(j);
          Label expected = targetSeq.getLabelAtPosition(j);
          FeatureVector fv = fvs.get(j);
          for (int ll = 0; ll < predicated.numLocations(); ll++) {
            int li = predicated.indexAtLocation(ll);
            double[] toUpdate = (expected.getBestIndex() == li ? pos : neg);
            double expectedWeight = (expected.getBestIndex() == li ? 1.0 : 0.0);
            double labelWeightDiff = Math.abs(expectedWeight - predicated.value(li));
            synchronized (count) {
              for (int fl = 0; fl < fv.numLocations(); fl++) {
                int fli = fv.indexAtLocation(fl);
                if (abssum != null) {
                  abssum[fli] += fv.valueAtLocation(fl) * labelWeightDiff * instanceWeight;
                }
                if (toUpdate != null) {
                  toUpdate[fli] += fv.valueAtLocation(fl) * labelWeightDiff * instanceWeight;
                }
              }
            }
          }
        }
        long newCount = count.incrementAndGet();
        if (newCount % 10000 == 0) {
          log.info("Processed " + newCount + " examples for grad accum...");
        }
      }
    });
  }

  public static RankedFeatureVector featureCountsFrom(InstanceList ilist) {
    return countFeatures(ilist, true);
  }

  public static RankedFeatureVector featureSumFrom(InstanceList ilist) {
    return countFeatures(ilist, false);
  }

  private static RankedFeatureVector countFeatures(InstanceList ilist, boolean countInstances) {
    int numFeatures = ilist.getDataAlphabet().size();
    double[] counts = new double[numFeatures];
    for (int i = 0; i < ilist.size(); i++) {
      Instance inst = ilist.get(i);
      if (ilist.getInstanceWeight(i) == 0) {
        continue;
      }
      Object data = inst.getData();
      if (data instanceof FeatureVectorSequence) {
        FeatureVectorSequence fvs = (FeatureVectorSequence) data;
        for (int j = 0; j < fvs.size(); j++) {
          countVector(counts, fvs.get(j), countInstances);
        }
      } else {
        throw new IllegalArgumentException("Currently only handles FeatureVectorSequence data");
      }
    }
    return new RankedFeatureVector(ilist.getDataAlphabet(), counts);
  }

  private static void countVector(double[] counts, FeatureVector fv, boolean countInstances) {
    for (int j = 0; j < fv.numLocations(); j++) {
      if (countInstances) {
        counts[fv.indexAtLocation(j)] += 1;
      } else {
        counts[fv.indexAtLocation(j)] += fv.valueAtLocation(j);
      }
    }
  }

  public static void writeRankedToFile(RankedFeatureVector rfv, File outputFile) {
    try {
      try (PrintWriter writer = new PrintWriter(Files.newWriter(outputFile, Charsets.UTF_8))) {

        for (int i = 0; i < rfv.singleSize(); i++) {
          Object objectAtRank = rfv.getObjectAtRank(i);
          double gradAtRank = rfv.getValueAtRank(i);
          writer.println(String.format("%s,%.5f", objectAtRank.toString(), gradAtRank));
        }
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
