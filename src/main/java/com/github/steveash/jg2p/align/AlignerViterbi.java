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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.math.DoubleMath;

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.util.Funcs;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

/**
 * Implementation of the viterbi algorithm to walk the k-most probable sequences through the inferred prob table
 * @author Steve Ash
 */
public class AlignerViterbi {

  private static final double minLogProb = -1e12;

  public AlignerViterbi(GramOptions opts, ProbTable probs) {
    this.opts = opts;
    this.probs = probs;
  }

  private final GramOptions opts;
  private final ProbTable probs;

  public List<Alignment> align(Word x, Word y, int bestPathCount) {
    PathTable t = new PathTable(x.unigramCount() + 1, y.unigramCount() + 1, bestPathCount);
    t.offer(0, 0, t.make(0, -1, -1, -1));

    for (int xx = 0; xx < x.unigramCount() + 1; xx++) {
      for (int yy = 0; yy < y.unigramCount() + 1; yy++) {

        if (xx > 0 && opts.isIncludeXEpsilons()) {
          for (int i = 1; (i <= opts.getMaxXGram()) && (xx - i >= 0); i++) {
            String xGram = x.gram(xx - i, i);
            double score = DoubleMath.log2(probs.prob(xGram, Grams.EPSILON)) * i;
            t.extendPath(xx, yy, xx - i, yy, PathTable.Entry.sample(score, i, 0));
          }
        }

        if (yy > 0 && opts.isIncludeEpsilonYs()) {
          for (int j = 1; (j <= opts.getMaxYGram()) && (yy - j >= 0); j++) {
            String yGram = y.gram(yy - j, j);
            double score = DoubleMath.log2(probs.prob(Grams.EPSILON, yGram)) * j;
            t.extendPath(xx, yy, xx, yy - j, PathTable.Entry.sample(score, 0, j));
          }
        }

        if (xx > 0 && yy > 0) {
          for (int i = 1; (i <= opts.getMaxXGram()) && (xx - i >= 0); i++) {
            for (int j = 1; (j <= opts.getMaxYGram()) && (yy - j >= 0); j++) {
              String xGram = x.gram(xx - i, i);
              String yGram = y.gram(yy - j, j);

              double score = DoubleMath.log2(probs.prob(xGram, yGram)) * Math.max(i, j);
              t.extendPath(xx, yy, xx - i, yy - j, PathTable.Entry.sample(score, i, j));
            }
          }
        }
      }
    }

    return createAlignments(x, y, t, bestPathCount);
  }

  private List<Alignment> createAlignments(Word x, Word y, PathTable t, int bestPathCount) {
    List<Alignment> results = Lists.newArrayListWithCapacity(bestPathCount);

    Iterable<PathTable.Entry> lastEntries = t.get(x.unigramCount(), y.unigramCount());

    for (PathTable.Entry lastEntry : lastEntries) {
      if (lastEntry.score < minLogProb) continue;

      results.add(decodePathFrom(x, y, t, lastEntry));
    }
    Collections.sort(results, Ordering.natural().reverse());
    return results;
  }

  private Alignment decodePathFrom(Word x, Word y, PathTable t, PathTable.Entry entry) {
    int xx = x.unigramCount();
    int yy = y.unigramCount();
    Alignment a = new Alignment(entry.score);

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


  public static class Alignment implements Iterable<Pair<String,String>>, Comparable<Alignment> {
    private static final Joiner pipeJoiner = Joiner.on('|');
    private static final Function<Pair<String, String>, String> SELECT_LEFT = Funcs.<String, String>selectLeft();
    private static final Function<Pair<String, String>, String> SELECT_RIGHT = Funcs.<String, String>selectRight();

    private final List<Pair<String, String>> graphones; // the pair of grapheme + phoneme
    private final double score;

    public Alignment(double score) {
      this.graphones = Lists.newArrayList();
      this.score = score;
    }

    private Alignment(List<Pair<String,String>> finalList, double score) {
      this.graphones = finalList;
      this.score = score;
    }

    void append(String xGram, String yGram) {
      graphones.add(Pair.of(xGram, yGram));
    }

    Alignment finish() {
      return new Alignment(Lists.reverse(this.graphones), score);
    }

    @Override
    public Iterator<Pair<String, String>> iterator() {
      return graphones.iterator();
    }

    public Iterable<String> getXTokens() {
      return makeGrams(SELECT_LEFT);
    }

    public Iterable<String> getYTokens() {
      return makeGrams(SELECT_RIGHT);
    }

    private Iterable<String> makeGrams(Function<Pair<String, String>, String> selector) {
      return filter(transform(graphones, selector), Funcs.onlyNonBlank());
    }

    @Override
    public String toString() {
      return pipeJoiner.join(transform(graphones, SELECT_LEFT)) + " -> " +
             pipeJoiner.join(transform(graphones, SELECT_RIGHT)) +
             String.format(" (score %.4f)", score);
    }

    @Override
    public int compareTo(Alignment that) {
      return Double.compare(this.score, that.score);
    }
  }
}
