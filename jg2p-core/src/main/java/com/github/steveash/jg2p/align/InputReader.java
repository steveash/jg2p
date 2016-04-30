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

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.syll.SWord;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl.ThreadStateMap.Byte1.other;

/**
 * @author Steve Ash
 */
public class InputReader {

  private static final Splitter tabSplit = Splitter.on('\t').trimResults();

  public static interface LineReader {

    /**
     * Return the input record or null if this line shouldnt be included
     */
    InputRecord parse(String line);
  }

  public static InputReader makeDefaultFormatReader() {
    return new InputReader(defaultLineReader);
  }

  public static InputReader makePSaurusReader() {
    return new InputReader(psaurusLineReader);
  }

  public static InputReader makeCmuReader() {
    return new InputReader(cmuReader);
  }

  private final LineReader reader;

  public InputReader() {
    this.reader = defaultLineReader;
  }

  public InputReader(LineReader reader) {
    this.reader = reader;
  }

  public List<InputRecord> readFromClasspath(String resource) throws IOException {
    return read(Resources.asCharSource(Resources.getResource(resource), Charsets.UTF_8));
  }

  public List<InputRecord> readFromFile(File input) throws IOException {
    return read(Files.asCharSource(input, Charsets.UTF_8));
  }

  public List<InputRecord> read(CharSource source) throws IOException {
    return source.readLines(new LineProcessor<List<InputRecord>>() {
      private final List<InputRecord> recs = Lists.newArrayList();

      @Override
      public boolean processLine(String line) throws IOException {
        if (isBlank(line)) {
          return true;
        }
        if (isComment(line)) {
          return true;
        }

        InputRecord maybe = reader.parse(line);
        if (maybe != null) {
          recs.add(maybe);
        }
        return true;
      }

      @Override
      public List<InputRecord> getResult() {
        return recs;
      }
    });
  }

  private boolean isComment(String line) {
    return line.startsWith(";;");
  }

  private static LineReader defaultLineReader = new LineReader() {
    @Override
    public InputRecord parse(String line) {
      Iterator<String> iter = tabSplit.split(line).iterator();
      Word x = Word.fromSpaceSeparated(iter.next());
      Word y = Word.fromSpaceSeparated(iter.next());
      return new InputRecord(x, y);
    }
  };

  private static LineReader psaurusLineReader = new LineReader() {
    @Override
    public InputRecord parse(String line) {
      Iterator<String> iter = tabSplit.split(line).iterator();
      Word x = Word.fromNormalString(iter.next());
      String phones = iter.next();
      Word y;
      if (iter.hasNext()) {
        String sylls = iter.next();
        y = new SWord(phones, sylls);
      } else {
        y = Word.fromSpaceSeparated(phones);
      }
      return new InputRecord(x, y);
    }
  };

  private static final Pattern xChars = Pattern.compile("([A-Z][A-Z']+)(\\(\\d+\\))?", Pattern.CASE_INSENSITIVE);
  private static LineReader cmuReader = new LineReader() {
    @Override
    public InputRecord parse(String line) {
      line = line.toUpperCase();
      // im going to ignore all of the "spelled out" versions of punctuation

      int split = line.indexOf("  ");
      String x = line.substring(0, split);
      Matcher xGood = xChars.matcher(x);
      if (!xGood.matches()) {
        return null;
      }
      if (isNotBlank(xGood.group(2))) {
        return null;
      }
      x = xGood.group(1);

      String y = line.substring(split + 2);
      String phonesOnly = CharMatcher.DIGIT.removeFrom(y);
      if (x.charAt(x.length() - 1) == '\'') {
        x = x.substring(0, x.length() - 1);
      }
      List<Integer> stresses = Lists.newArrayList();
      for (String s : Grams.iterateSymbols(y)) {
        String onlyDigits = CharMatcher.DIGIT.retainFrom(s);
        if (StringUtils.isBlank(onlyDigits)) {
          stresses.add(-1);
        } else {
          stresses.add(Integer.parseInt(onlyDigits));
        }
      }

      Word xx = Word.fromNormalString(x);
      Word yy = Word.fromSpaceSeparated(phonesOnly);
      return new InputRecord(xx, yy, stresses);
    }
  };
}
