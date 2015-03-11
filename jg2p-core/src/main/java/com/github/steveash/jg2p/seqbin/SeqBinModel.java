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

package com.github.steveash.jg2p.seqbin;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.InputRecord;

import java.io.Serializable;

import cc.mallet.classify.Classification;
import cc.mallet.classify.MaxEnt;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Labeling;

/**
 * @author Steve Ash
 */
public class SeqBinModel implements Serializable {
  private final long serialVersionUID = 123L;

  private MaxEnt classifier;

  public SeqBinModel(MaxEnt classifier) {
    this.classifier = classifier;
  }

  public Labeling classify(Word word) {
    Instance instance = new Instance(word.getValue(), null, null, null);
    instance = classifier.getInstancePipe().pipe(instance);
    Classification result = classifier.classify(instance);
    return result.getLabeling();
  }
}
