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

package com.github.steveash.jg2p.train;

import com.google.common.base.Charsets;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.util.Funcs;

import java.io.IOException;

/**
 * allows pipeline trainer to exlude known noisy values from the training set; you can put just the graphemes or you
 * can put graphemes^^phonemesAsSpaceString (no stress indicators)
 * @author Steve Ash
 */
public class SkipTrainings {

  public static SkipTrainings defaultSkips() {
    try {
      ImmutableList<String> lines = Resources.asCharSource(Resources.getResource("skip-training.txt"), Charsets.UTF_8)
          .readLines();
      return new SkipTrainings(FluentIterable.from(lines)
                               .filter(Funcs.onlyNonBlank())
                               .transform(Funcs.trimAndLower())
                               .toSet());

    } catch (IOException e) {
      throw new RuntimeException("Cannot load skip-training.txt from the classpath", e);
    }
  }

  private SkipTrainings(ImmutableSet<String> skips) {
    this.skips = skips;
  }

  private final ImmutableSet<String> skips;

  public boolean skip(InputRecord rec) {
    String graphs = rec.getLeft().getAsNoSpaceString().toLowerCase();
    if (skips.contains(graphs)) {
      return true;
    }
    String phones = rec.getRight().getAsSpaceString().toLowerCase();
    if (skips.contains(graphs + "^^" + phones)) {
      return true;
    }
    return false;
  }
}
