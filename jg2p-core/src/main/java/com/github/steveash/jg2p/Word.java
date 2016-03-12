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
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Word is a string with some helper methods for creating n-grams, etc.
 * Grams are space separated
 *
 * @author Steve Ash
 */
public class Word implements Iterable<String> {
  private static final Splitter splitter = Splitter.on(' ').trimResults().omitEmptyStrings();
  private static final Joiner joiner = Joiner.on(' ');
  private static final CharMatcher spaces = CharMatcher.is(' ').precomputed();

  public static Word fromSpaceSeparated(String spaceSeparated) {
    return new Word(splitter.splitToList(spaceSeparated));
  }

  public static Word fromGrams(Iterable<String> grams) {
    return new Word(ImmutableList.copyOf(grams));
  }

  public static Word fromNormalString(String normalString) {
    List<String> chars = Lists.newArrayListWithCapacity(normalString.length());
    for (int i = 0; i < normalString.length(); i++) {
      chars.add(normalString.substring(i, i + 1));
    }
    return new Word(chars);
  }

  private final List<String> value;

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
  }

  public String getAsSpaceString() {
    return joiner.join(value);
  }

  public String gram(int index, int size) {
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
}
