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

package com.github.steveash.jg2p;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Word is a string with some helper methods for creating n-grams, etc.
 * Grams are space separated
 *
 * @author Steve Ash
 */
public class Word implements Iterable<String>, Comparable<Word> {
  protected static final Splitter splitter = Splitter.on(' ').trimResults().omitEmptyStrings();
  protected static final Joiner joiner = Joiner.on(' ');
  protected static final Joiner noJoiner = Joiner.on("");
  protected static final CharMatcher spaces = CharMatcher.is(' ').precomputed();
  protected static final int MAX_CACHED_GRAM_SIZE = 2;

  public static Word fromSpaceSeparated(String spaceSeparated) {
    return new Word(splitter.splitToList(spaceSeparated));
  }

  public static Word fromGrams(Iterable<String> grams) {
    return new Word(ImmutableList.copyOf(grams));
  }

  public static Word fromNormalString(String normalString) {
    List<String> chars = Lists.newArrayListWithCapacity(normalString.length());
    for (int i = 0; i < normalString.length(); i++) {
      chars.add(normalString.substring(i, i + 1).intern());
    }
    return new Word(chars);
  }

  private final List<String> value;
//  private final StringTable gramCache;

  public static void throwIfNotUnigram(List<String> grams) {
    boolean gotOne = false;
    for (int i = 0; i < grams.size(); i++) {
      String gram = grams.get(i);
      if (isBlank(gram) || spaces.matchesAnyOf(gram)) {
        throw new IllegalArgumentException("The input grams list " + grams + " contains n-grams");
      }
      gotOne = true;
    }
    if (!gotOne) {
      throw new IllegalArgumentException("Word is empty: " + grams);
    }
  }

  public void throwIfNotUnigram() {
    throwIfNotUnigram(this.value);
  }

  public final int unigramCount() {
    return value.size();
  }

  protected Word(List<String> value) {
    this.value = value;
    // cache the common grams len 1-2
//    this.gramCache = new StringTable(value.size(), MAX_CACHED_GRAM_SIZE);
//    for (int i = 0; i < value.size(); i++) {
//      for (int j = 0; j < MAX_CACHED_GRAM_SIZE && (i + j) < value.size(); j++) {
//        this.gramCache.set(i, j, gramRaw(i, j + 1).intern());
//      }
//    }
  }

  public String getAsSpaceString() {
    return joiner.join(value);
  }

  public String getAsNoSpaceString() {
    return noJoiner.join(value);
  }

  public String gram(int index, int size) {
//    if (size <= MAX_CACHED_GRAM_SIZE) {
//      return gramCache.get(index, size - 1);
//    }
    return gramRaw(index, size);
  }

  public String gramRaw(int index, int size) {
    StringBuilder sb = new StringBuilder(size * 4);
    for (int i = index; i < index + size; i++) {
      sb.append(value.get(i));
      if (i + 1 < index + size) {
        sb.append(' ');
      }
    }
    return sb.toString();
  }

  public Iterable<String> gramsSize(final int size) {
    return new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return new AbstractIterator<String>() {
          int next = 0;
          @Override
          protected String computeNext() {
            if (next + size > value.size()) {
              return endOfData();
            }
            String gram = gram(next, size);
            next += 1;
            return gram;
          }
        };
      }
    };
  }

  public Iterable<String> gramsSizes(final int minSize, final int maxSize) {
    List<Iterable<String>> grams = Lists.newArrayListWithCapacity(maxSize - minSize + 1);
    for (int i = minSize; i <= maxSize; i++) {
      grams.add(gramsSize(i));
    }
    return Iterables.concat(grams);
  }

  public List<String> getValue() {
    return value;
  }

  public String gramAt(int index) {
    return value.get(index);
  }

  public List<Pair<String,String>> getLeftOnlyPairs() {
    return Lists.transform(value, new Function<String, Pair<String, String>>() {
      @Override
      public Pair<String, String> apply(String input) {
        return Pair.of(input, "");
      }
    });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Word word = (Word) o;

    if (!value.equals(word.value)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return getAsSpaceString();
  }
  
  @Override
  public Iterator<String> iterator() {
    return value.iterator();
  }

  @Override
  public int compareTo(Word o) {
    int min = Math.min(this.value.size(), o.value.size());
    for (int i = 0; i < min; i++) {
      int elem = Ordering.natural().compare(this.value.get(i), o.value.get(i));
      if (elem != 0) {
        return elem;
      }
    }
    return Integer.compare(this.value.size(), o.value.size());
  }
}
