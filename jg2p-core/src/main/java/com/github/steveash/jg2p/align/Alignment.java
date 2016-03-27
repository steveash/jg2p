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
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.util.Funcs;
import com.github.steveash.jg2p.util.Zipper;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Represents one alignment from X to Y.  In the case of running alignment between X to Y the x represents the grapheme
 * and the y represents the phoneme from the training example.  In the inference case, only the X side will be populated
 * and the Y side will be null
 *
 * @author Steve Ash
 */
public class Alignment implements Iterable<Pair<String, String>>, Comparable<Alignment> {

  private static final Joiner pipeJoiner = Joiner.on('|');
  private static final Splitter spaceSplit = Splitter.on(' ');
  private static final Function<Pair<String, String>, String> SELECT_LEFT = Funcs.selectLeft();
  private static final Function<Pair<String, String>, String> SELECT_RIGHT = Funcs.selectRight();

  private final List<Pair<String, String>> graphones; // the pair of grapheme + phoneme
  private final double score;
  private final Word input;


  public Alignment(Word input, double score) {
    this.input = input;
    this.graphones = Lists.newArrayList();
    this.score = score;
  }

  public Alignment(Word input, List<Pair<String, String>> finalList, double score) {
    this.input = input;
    this.graphones = finalList;
    this.score = score;
  }

  public List<Pair<String, String>> getGraphones() {
    return graphones;
  }

  public Iterable<Pair<List<String>, List<String>>> getGraphonesSplit() {
    return Iterables.transform(graphones, splitBoth);
  }

  void append(String xGram, String yGram) {
    graphones.add(Pair.of(xGram, yGram));
  }

  Alignment finish() {
    return new Alignment(input, Lists.reverse(this.graphones), score);
  }

  public Alignment withReplacedYs(Iterable<String> newYs) {
    return new Alignment(input, Zipper.replaceRight(this.graphones, newYs), score);
  }

  @Override
  public Iterator<Pair<String, String>> iterator() {
    return graphones.iterator();
  }

  public double getScore() {
    return score;
  }

  public Iterable<String> getXTokens() {
    return makeGrams(SELECT_LEFT);
  }

  public List<String> getAllXTokensAsList() {
    return Lists.newArrayList(transform(graphones, SELECT_LEFT));
  }

  public Iterable<String> getYTokens() {
    return makeGrams(SELECT_RIGHT);
  }

  public List<String> getAllYTokensAsList() {
    return Lists.newArrayList(transform(graphones, SELECT_RIGHT));
  }

  private Iterable<String> makeGrams(Function<Pair<String, String>, String> selector) {
    return filter(transform(graphones, selector), Funcs.onlyNonBlank());
  }

  @Override
  public String toString() {
    return getXAsPipeString() + " -> " +
           getYAsPipeString() +
           String.format(" (score %.4f)", score);
  }

  public String getYAsPipeString() {
    return pipeJoiner.join(transform(graphones, SELECT_RIGHT));
  }

  public String getXAsPipeString() {
    return pipeJoiner.join(transform(graphones, SELECT_LEFT));
  }

  public String getAsPipeString(Iterable<String> symbols) {
    return pipeJoiner.join(symbols);
  }

  public String getWordAsSpaceString() {
    return input.getAsSpaceString();
  }

  public List<String> getWordUnigrams() {
    return input.getValue();
  }

  public Pair<Word, Word> xyWordPair() {
    return Pair.of(input, Word.fromGrams(getYTokens()));
  }

  /**
   * @return a list of flags that indicate the _last_ letter in the grapheme group for the X word; this doesn't work if
   * you allow epsilons on the X side
   */
  public List<Boolean> getXBoundaryMarks() {
    Preconditions.checkArgument(graphones.size() > 0);
    Iterator<Pair<String, String>> xIter = Iterators.filter(this.graphones.iterator(), nonEmptyXGraphones);
    List<String> xEntry = getNextX(xIter);
    int xChar = 0;

    List<Boolean> marks = Lists.newArrayListWithCapacity(input.unigramCount());
    for (int i = 0; i < input.unigramCount(); i++) {

      // have we exhasuted the graphone entry we're on
      if (xChar >= xEntry.size()) {
        xChar = 0;
        xEntry = getNextX(xIter);
      }

      String wordGram = input.getValue().get(i);
      String graphoneGram = xEntry.get(xChar);
      Preconditions.checkState(wordGram.equals(graphoneGram), "Should be equal %s and %s", wordGram, graphoneGram);

      boolean isLast = xChar == xEntry.size() - 1;
      marks.add(isLast);
      xChar += 1;
    }
    Preconditions.checkState(!xIter.hasNext());
    return marks;
  }

  public List<Boolean> getXStartMarks() {
    List<Boolean> marks = getXBoundaryMarks();
    List<Boolean> starts = Lists.newArrayListWithCapacity(marks.size());
    starts.add(true); // first spot is always a start
    for (int i = 1; i < marks.size(); i++) {
      starts.add(marks.get(i - 1));
    }
    return starts;
  }

  public String getXBoundaryMarksAsString() {
    return getBoolsAsString(getXBoundaryMarks());
  }

  public String getXStartMarksAsString() {
    return getBoolsAsString(getXStartMarks());
  }

  protected String getBoolsAsString(List<Boolean> marks) {
    StringBuilder sb = new StringBuilder(marks.size());
    for (Boolean mark : marks) {
      sb.append(mark ? "1" : "0");
    }
    return sb.toString();
  }

  private List<String> getNextX(Iterator<Pair<String, String>> iter) {
    Pair<String, String> graphone = iter.next();
    return spaceSplit.splitToList(graphone.getLeft());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Alignment pairs = (Alignment) o;

    if (!graphones.equals(pairs.graphones)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return graphones.hashCode();
  }

  @Override
  public int compareTo(Alignment that) {
    return Double.compare(this.score, that.score);
  }

  private static final Predicate<Pair<String, String>> nonEmptyXGraphones = new Predicate<Pair<String, String>>() {
    @Override
    public boolean apply(Pair<String, String> input) {
      return isNotBlank(input.getLeft());
    }
  };

  private static final Function<Pair<String, String>, Pair<List<String>, List<String>>> splitBoth =
      new Function<Pair<String, String>, Pair<List<String>, List<String>>>() {
        @Override
        public Pair<List<String>, List<String>> apply(Pair<String, String> input) {
          return Pair.of(spaceSplit.splitToList(input.getLeft()), spaceSplit.splitToList(input.getRight()));
        }
      };
}
