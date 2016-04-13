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

package com.github.steveash.jg2p.eval

import cc.mallet.fst.CRF
import cc.mallet.fst.SumLattice
import cc.mallet.types.Instance
import cc.mallet.types.InstanceList
import cc.mallet.types.LabelAlphabet
import cc.mallet.types.Sequence
import groovy.transform.CompileStatic
import groovyx.gpars.GParsPool
import groovyx.gpars.GParsPoolUtil

/**
 * @author Steve Ash
 */
@CompileStatic
class ParallelEval {

  private final CRF crf

  ParallelEval(CRF crf) {
    this.crf = crf
  }

  List<SumLattice> parallelSum(InstanceList ilist) {
    def alpha = (LabelAlphabet) ilist.getTargetAlphabet()
    GParsPool.withPool {
      GParsPoolUtil.collectParallel(ilist) { Instance inst ->
        crf.getSumLatticeFactory().newSumLattice(crf, (Sequence) inst.getData(), null, null, alpha);
      }
    } as List<SumLattice>
  }
}
