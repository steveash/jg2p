/*
 * Copyright 2016 Steve Ash
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

package com.github.steveash.jg2p.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * @author Steve Ash
 */
public class DiscToArpa {

  private static final Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).trimResults().omitEmptyStrings();
  private static final CharMatcher stress = CharMatcher.anyOf("\'\"");
  private final ImmutableMap<String, String> map;


  public DiscToArpa(ImmutableMap<String, String> map) {

    this.map = map;
  }

  public static DiscToArpa create() {
    try {
      URL resource = Resources.getResource("discToArpa.csv");
      List<String> lines = Resources.asCharSource(resource, Charsets.UTF_8).readLines();
      ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
      for (String line : lines) {
        if (line.trim().equals("") || line.startsWith("//")) continue;

        Iterator<String> iter = splitter.split(line).iterator();
        Preconditions.checkArgument(iter.hasNext(), "problem with ", line);
        String key = iter.next();
        Preconditions.checkArgument(iter.hasNext(), "problem with ", line);
        String val = iter.next();
        builder.put(key, val);
      }

      return new DiscToArpa(builder.build());

    } catch (IOException e) {
      throw new RuntimeException("cant read discToArp.csv rules", e);
    }
  }

  // converts a disc string to an arpa string with dashes for syllable boundaries and -' for syllable with stress
  public List<String> convertToArpa(String disc) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    int i = 0;
    if (stress.matches(disc.charAt(0))) {
      builder.add("-\'");
      i += 1;
    } else {
      builder.add("-");
    }
    for (; i < disc.length(); i++) {
      String dc = Character.toString(disc.charAt(i));
      if (dc.equals("-")) {
        Preconditions.checkState(i < (disc.length() - 1), "cant have a trailing syllable marker");
        if (stress.matches(disc.charAt(i + 1))) {
          builder.add("-'");
          i += 1;
        } else {
          builder.add("-");
        }
        continue;
      }
      String arp = map.get(dc);
      if (arp == null) {
        throw new IllegalArgumentException("missing " + dc + " from " + disc);
      }
      builder.add(arp);
    }
    return builder.build();
  }
}
