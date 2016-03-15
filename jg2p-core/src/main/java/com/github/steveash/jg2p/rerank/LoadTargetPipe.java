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

package com.github.steveash.jg2p.rerank;

import com.google.common.collect.Lists;

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labels;

/**
 * Loads the target label by inspecting the input list and building a labels instance with the indexes of the
 * target that are relevant.  If no instances are relevant then a -1 is returned because that signifies to skip this
 * NOTE that this is only done when the target has some non-null value.  At train time we put some object there
 * to enable this pipe.  At test time we dont do this and thus the pipe doesn't do anything
 * @author Steve Ash
 */
public class LoadTargetPipe extends Pipe {

  private final LabelAlphabet targetDict;

  public LoadTargetPipe(Alphabet dataDict, LabelAlphabet targetDict) {
    super(dataDict, targetDict);
    this.targetDict = targetDict;
  }


  @Override
  public Instance pipe(Instance inst) {
    Object maybeTarget = inst.getTarget();
    // if null then we are in test mode not train mode
    if (maybeTarget == null) {
      return inst;
    }
    List<RerankExample> entries = (List<RerankExample>) inst.getData();
    List<Integer> goodIndexes = Lists.newArrayListWithExpectedSize(1);
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).isRelevant()) {
        goodIndexes.add(i);
      }
    }
    if (goodIndexes.isEmpty()) {
      inst.setTarget(targetDict.lookupLabel("-1"));
    } else if (goodIndexes.size() == 1) {
      inst.setTarget(targetDict.lookupLabel(Integer.toString(goodIndexes.get(0))));
    } else {
      // there are multiple labels
      Label[] labels = new Label[goodIndexes.size()];
      for (int i = 0; i < goodIndexes.size(); i++) {
        labels[i] = targetDict.lookupLabel(Integer.toString(goodIndexes.get(0)));
      }
      inst.setTarget(new Labels(labels));
    }
    return inst;
  }
}
