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

import com.github.steveash.jg2p.util.Funcs;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

/**
 * Represents one alignment from X to Y.  In the case of running alignment between X to Y the x represents the
 * grapheme and the y represents the phoneme from the training example.  In the inference case, only the X side
 * will be populated and the Y side will be null
* @author Steve Ash
*/
public class Alignment implements Iterable<Pair<String,String>>, Comparable<Alignment> {
  private static final Joiner pipeJoiner = Joiner.on('|');
  private static final Function<Pair<String, String>, String> SELECT_LEFT = Funcs.selectLeft();
  private static final Function<Pair<String, String>, String> SELECT_RIGHT = Funcs.selectRight();

  private final List<Pair<String, String>> graphones; // the pair of grapheme + phoneme
  private final double score;

  public Alignment(double score) {
    this.graphones = Lists.newArrayList();
    this.score = score;
  }

  public Alignment(List<Pair<String, String>> finalList, double score) {
    this.graphones = finalList;
    this.score = score;
  }

  public List<Pair<String,String>> getGraphones() { return graphones; }

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

  @Override
  public int compareTo(Alignment that) {
    return Double.compare(this.score, that.score);
  }
}
