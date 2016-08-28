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

package com.github.steveash.jg2p.wfst;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.kylm.model.ngram.NgramLM;
import com.github.steveash.kylm.model.ngram.smoother.MKNSmoother;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * @author Steve Ash
 */
public class G2pFstTrainer {

  private static final Logger log = LoggerFactory.getLogger(G2pFstTrainer.class);

  private static final Joiner tieJoiner = Joiner.on(SeqTransducer.SEP);

  private NgramLM lastLm;

  public SeqTransducer alignAndTrain(List<InputRecord> records, AlignModel alignModel, int order) {
    log.info("Preparing training input for WFST trainer");
    List<String[]> sentences = Lists.newArrayListWithCapacity(records.size());
    for (InputRecord record : records) {
      List<Alignment> best = alignModel.align(record.xWord, record.yWord, 1);
      if (!best.isEmpty()) {
        sentences.add(alignToExample(best.get(0)));
      }
    }
    return trainWithSentences(sentences, order);
  }

  public SeqTransducer trainWithAligned(List<Alignment> records, int order) {
    log.info("Preparing training input for WFST trainer");
    List<String[]> sentences = Lists.newArrayListWithCapacity(records.size());
    for (Alignment record : records) {
      sentences.add(alignToExample(record));
    }
    return trainWithSentences(sentences, order);
  }

  public SeqTransducer trainWithSentences(List<String[]> sentences, int order) {
    log.info("Training LM on " + sentences.size() + " training examples");
    MKNSmoother smoother = new MKNSmoother();
    smoother.setSmoothUnigrams(true);
    NgramLM lm = new NgramLM(order, smoother);
    lm.trainModel(sentences);
    this.lastLm = lm;
    return new LangModelToFst().fromModel(lm);
  }

  public NgramLM getLastLm() {
    return lastLm;
  }

  private String[] alignToExample(Alignment alignment) {
    Iterator<Pair<List<String>, List<String>>> graphones = alignment.getGraphonesSplit().iterator();
    String[] words = new String[alignment.getGraphones().size()];
    for (int i = 0; i < alignment.getGraphones().size(); i++) {
      Pair<List<String>, List<String>> graphone = graphones.next();
      words[i] = tieJoiner.join(graphone.getLeft()) + SeqTransducer.GRAPHONE_DELIM +
                 tieJoiner.join(graphone.getRight());
    }
    Preconditions.checkState(!graphones.hasNext());
    return words;
  }
}
