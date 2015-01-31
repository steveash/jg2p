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

package com.github.steveash.jg2p.seq;

import cc.mallet.grmm.util.LabelsAssignment;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.LabelsSequence;

/**
 * Converts a normal mallet label sequence in to a grmm labels assignment
 * @author Steve Ash
 */
public class LabelSequenceToLabelsAssignment extends Pipe {

  public LabelSequenceToLabelsAssignment(Alphabet dataDict, Alphabet targetDict) {
    super(dataDict, targetDict);
  }

  @Override
  public Instance pipe(Instance inst) {
    LabelSequence seq = (LabelSequence) inst.getTarget();
    LabelsSequence sseq = new LabelsSequence(seq);
    LabelsAssignment labels = new LabelsAssignment(sseq);
    inst.setTarget(labels);
    return inst;
  }
}
