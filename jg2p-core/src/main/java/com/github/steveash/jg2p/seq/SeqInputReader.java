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

package com.github.steveash.jg2p.seq;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.io.LineProcessor;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.util.Zipper;

import java.io.IOException;
import java.util.List;

/**
 * @author Steve Ash
 */
public class SeqInputReader {

  private static final Splitter caretSplit = Splitter.on("^");
  private static final Splitter pipeSplit = Splitter.on("|");

  public static class AlignGroup {

    public final ImmutableList<Alignment> alignments;

    public AlignGroup(Iterable<Alignment> alignments) {
      this.alignments = ImmutableList.copyOf(alignments);
    }
  }

  public List<AlignGroup> readInput(CharSource source) throws IOException {
    return source.readLines(new LineProcessor<List<AlignGroup>>() {

      final List<AlignGroup> groups = Lists.newArrayList();
      String lastGroup = "";
      List<Alignment> aligns = Lists.newArrayList();

      @Override
      public boolean processLine(String line) throws IOException {
        List<String> fields = caretSplit.splitToList(line);
        if (!lastGroup.equals(fields.get(0))) {
          lastGroup = fields.get(0);
          if (!aligns.isEmpty()) {
            groups.add(new AlignGroup(aligns));
          }
          aligns.clear();
        }
        Double score = Double.parseDouble(fields.get(1));
        Word input = Word.fromSpaceSeparated(fields.get(2));
        Iterable<String> graphs = pipeSplit.split(fields.get(3));
        Iterable<String> phones = pipeSplit.split(fields.get(4));

        aligns.add(new Alignment(input, Zipper.up(graphs, phones), score));
        return true;
      }

      @Override
      public List<AlignGroup> getResult() {
        if (!aligns.isEmpty()) {
          groups.add(new AlignGroup(aligns));
        }
        return groups;
      }
    });
  }
}
