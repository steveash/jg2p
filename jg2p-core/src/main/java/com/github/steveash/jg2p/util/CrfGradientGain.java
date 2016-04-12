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

import cc.mallet.fst.CRF;
import cc.mallet.fst.SumLattice;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.LabelVector;
import cc.mallet.types.Labeling;
import cc.mallet.types.RankedFeatureVector;
import cc.mallet.types.Sequence;

/**
 * @author Steve Ash
 */
public class CrfGradientGain {

  /**
   * Returns a ranked feature vector of the gradient gain for the given training data
   * on the given trained CRF.  The instance list must have the target labels as LabelSequence
   * @param ilist
   * @param crf
   * @return
   */
  public static RankedFeatureVector gradientGainFrom(InstanceList ilist, CRF crf) {
    int numFeatures = ilist.getDataAlphabet().size();
    double[] gradientgains = new double[numFeatures];
    int fli; // feature location index
    // Populate targetFeatureCount, et al
    for (int i = 0; i < ilist.size(); i++) {
      Instance inst = ilist.get(i);
      LabelSequence targetSeq = (LabelSequence) inst.getTarget();
      FeatureVectorSequence fvs = (FeatureVectorSequence) inst.getData();
      double instanceWeight = ilist.getInstanceWeight(i);
      SumLattice lattice = crf.getSumLatticeFactory()
              .newSumLattice(crf, (Sequence) inst.getData(), null, null, (LabelAlphabet) ilist.getTargetAlphabet());
      Preconditions.checkState(targetSeq.size() == fvs.size(), "input output size diff");
      for (int j = 0; j < targetSeq.size(); j++) {
        // The code below relies on labelWeights summing to 1 over all labels!
        LabelVector predicated = lattice.getLabelingAtPosition(j);
        Label expected = targetSeq.getLabelAtPosition(j);
        FeatureVector fv = fvs.get(j);
        for (int ll = 0; ll < predicated.numLocations(); ll++) {
          int li = predicated.indexAtLocation(ll);
          double expectedWeight = (expected.getBestIndex() == li ? 1.0 : 0.0);
          double labelWeightDiff = Math.abs(expectedWeight - predicated.value(li));
          for (int fl = 0; fl < fv.numLocations(); fl++) {
            fli = fv.indexAtLocation(fl);
            gradientgains[fli] += fv.valueAtLocation(fl) * labelWeightDiff * instanceWeight;
          }
        }
      }
    }
    return new RankedFeatureVector(ilist.getDataAlphabet(), gradientgains);
  }

}
