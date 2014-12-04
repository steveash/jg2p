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

package com.github.steveash.jg2p.align;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.math.DoubleMath;

import com.github.steveash.jg2p.Word;

import java.util.Collections;
import java.util.List;

/**
 * Runs inference on a sequence X to determine the top-k probable alignment(s)
 *
 * @author Steve Ash
 */
public class AlignerInferencer {

  private final GramOptions opts;
  private final ProbTable probs;
  private final ProbTable.Marginals margs;

  public AlignerInferencer(GramOptions opts, ProbTable probs) {
    this.opts = opts;
    this.probs = probs;
    this.margs = probs.calculateMarginals();
  }

  public List<Alignment> bestGraphemes(Word x, int bestPathCount) {
    PathXTable t = new PathXTable(x.unigramCount() + 1, bestPathCount);
    t.offer(0, t.make(0, -1, -1));

    for (int xx = 1; xx < x.unigramCount() + 1; xx++) {
      for (int i = 1; (i <= opts.getMaxXGram()) && (xx - i >= 0); i++) {
        String xGram = x.gram(xx - i, i);
        double margX = margs.probX(xGram);

        double score = DoubleMath.log2(margX) * i;
        t.extendPath(xx, xx - i, PathXTable.Entry.sample(score, i));
      }
    }

    return createAlignments(x, t, bestPathCount);
  }

  private List<Alignment> createAlignments(Word x, PathXTable t, int bestPathCount) {
    List<Alignment> results = Lists.newArrayListWithCapacity(bestPathCount);

    Iterable<PathXTable.Entry> lastEntries = t.get(x.unigramCount());

    for (PathXTable.Entry lastEntry : lastEntries) {
      if (lastEntry.score < ProbTable.minLogProb) {
        continue;
      }

      results.add(decodePathFrom(x, t, lastEntry));
    }
    Collections.sort(results, Ordering.natural().reverse());
    return results;
  }

  private Alignment decodePathFrom(Word x, PathXTable t, PathXTable.Entry entry) {
    int xx = x.unigramCount();
    Alignment a = new Alignment(x, entry.score);

    while (xx > 0) {
      String xGram = x.gram(xx - entry.xBackRef, entry.xBackRef);
      a.append(xGram, "");

      xx -= entry.xBackRef;
      entry = t.get(xx, entry.pathBackRef);
    }
    return a.finish();
  }

}
