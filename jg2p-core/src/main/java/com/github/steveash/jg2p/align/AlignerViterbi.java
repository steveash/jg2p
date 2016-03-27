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

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.Word;

import java.util.Collections;
import java.util.List;

/**
 * Implementation of the viterbi algorithm to walk the k-most probable sequences through the inferred prob table
 * @author Steve Ash
 */
public class AlignerViterbi {


  public AlignerViterbi(GramOptions opts, ProbTable probs) {
    this.opts = opts;
    this.probs = probs;
    this.penalizer = opts.makePenalizer();
  }

  private final GramOptions opts;
  private final ProbTable probs;
  private final Penalizer penalizer;

  public List<Alignment> align(Word x, Word y, int bestPathCount) {
    PathXYTable t = new PathXYTable(x.unigramCount() + 1, y.unigramCount() + 1, bestPathCount);
    t.offer(0, 0, t.make(0, -1, -1, -1));

    for (int xx = 0; xx < x.unigramCount() + 1; xx++) {
      for (int yy = 0; yy < y.unigramCount() + 1; yy++) {

        if (xx > 0 && opts.isIncludeXEpsilons()) {
          for (int i = 1; (i <= opts.getMaxXGram()) && (xx - i >= 0); i++) {
            String xGram = x.gram(xx - i, i);
            double score = DoubleMath.log2(penalizer.penalize(xGram, Grams.EPSILON, probs.prob(xGram, Grams.EPSILON))); // what was this * i business
            t.extendPath(xx, yy, xx - i, yy, PathXYTable.Entry.sample(score, i, 0));
          }
        }

        if (yy > 0 && opts.isIncludeEpsilonYs()) {
          for (int j = 1; (j <= opts.getMaxYGram()) && (yy - j >= 0); j++) {
            String yGram = y.gram(yy - j, j);
            double score = DoubleMath.log2(penalizer.penalize(Grams.EPSILON, yGram, probs.prob(Grams.EPSILON, yGram))); // * j;
            t.extendPath(xx, yy, xx, yy - j, PathXYTable.Entry.sample(score, 0, j));
          }
        }

        if (xx > 0 && yy > 0) {
          for (int i = 1; (i <= opts.getMaxXGram()) && (xx - i >= 0); i++) {
            for (int j = 1; (j <= opts.getMaxYGram()) && (yy - j >= 0); j++) {
              String xGram = x.gram(xx - i, i);
              String yGram = y.gram(yy - j, j);

              double score = DoubleMath.log2(penalizer.penalize(xGram, yGram, probs.prob(xGram, yGram))); // * Math.max(i, j);
              t.extendPath(xx, yy, xx - i, yy - j, PathXYTable.Entry.sample(score, i, j));
            }
          }
        }
      }
    }

    return createAlignments(x, y, t, bestPathCount);
  }

  private List<Alignment> createAlignments(Word x, Word y, PathXYTable t, int bestPathCount) {
    List<Alignment> results = Lists.newArrayListWithCapacity(bestPathCount);

    Iterable<PathXYTable.Entry> lastEntries = t.get(x.unigramCount(), y.unigramCount());

    for (PathXYTable.Entry lastEntry : lastEntries) {
      if (lastEntry.score < ProbTable.minLogProb) continue;

      results.add(decodePathFrom(x, y, t, lastEntry));
    }
    Collections.sort(results, Ordering.natural().reverse());
    return results;
  }

  private Alignment decodePathFrom(Word x, Word y, PathXYTable t, PathXYTable.Entry entry) {
    int xx = x.unigramCount();
    int yy = y.unigramCount();
    Alignment a = new Alignment(x, entry.score);

    while (xx > 0 || yy > 0) {
      String xGram = x.gram(xx - entry.xBackRef, entry.xBackRef);
      String yGram = y.gram(yy - entry.yBackRef, entry.yBackRef);
      a.append(xGram, yGram);

      xx -= entry.xBackRef;
      yy -= entry.yBackRef;
      entry = t.get(xx, yy, entry.pathBackRef);
    }
    return a.finish();
  }


}
