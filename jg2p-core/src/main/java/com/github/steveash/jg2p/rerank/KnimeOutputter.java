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

package com.github.steveash.jg2p.rerank;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;

import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

/**
 * @author Steve Ash
 */
public class KnimeOutputter {

  private static final Logger log = LoggerFactory.getLogger(KnimeOutputter.class);

  private static final Joiner COMMA = Joiner.on(",");

  public void output(InstanceList insts, PrintWriter pw) {
    int size = insts.getAlphabet().size();
    List<String> row = Lists.newArrayListWithCapacity(size + 1);
    row.add("label");
    for (int i = 0; i < size; i++) {
      row.add(insts.getAlphabet().lookupObject(i).toString());
    }
    pw.println(COMMA.join(row));

    // print all instances densely
    int count = 0;
    for (Instance inst : insts) {
      if (++count % 2048 == 0) {
        log.info("Emitted " + count + " rows into the knime dense format");
      }
      row.clear();
      row.add(inst.getTarget().toString());
      FeatureVector fv = (FeatureVector) inst.getData();
      int pos = 0;
      // do a quick merge sort between the two to know what value to put
      int[] indices = fv.getIndices();
      int lastIdx = -1;
      for (int idx = 0; idx < size; idx++) {
        while (pos < indices.length && indices[pos] < idx) {
          int next = indices[pos];
          if (lastIdx > next) {
            throw new IllegalStateException("not sorted indexes");
          }
          lastIdx = next;
          pos++; // catch up
        }
        // either current position is >= what im trying to put out
        if (pos < indices.length && indices[pos] == idx) {
          double v = fv.valueAtLocation(pos);
          if (v == 0.0) {
            throw new IllegalStateException("supposed to be sparse");
          }
          if (DoubleMath.fuzzyEquals(v, 1.0, 0.0001)) {
            row.add("1");
          } else {
            row.add(String.format("%.8f", v));
          }
          int next = indices[pos];
          if (lastIdx > next) {
            throw new IllegalStateException("not sorted indexes");
          }
          lastIdx = next;
          pos++;
        } else {
          row.add("0");
        }
      }
      Preconditions.checkState(row.size() == size + 1, "got ");
      pw.println(COMMA.join(row));
    }
  }
}
