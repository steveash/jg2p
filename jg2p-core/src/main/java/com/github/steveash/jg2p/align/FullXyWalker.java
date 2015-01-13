/*
 * Copyright 2015 Steve Ash
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

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.Word;

/**
 * @author Steve Ash
 */
public class FullXyWalker implements XyWalker {

  public FullXyWalker(GramOptions opts) {
    this.opts = opts;
  }

  private final GramOptions opts;

  @Override
  public void forward(Word x, Word y, Visitor visitor) {
    int xsize = x.unigramCount();
    int ysize = y.unigramCount();

    for (int xx = 0; xx <= xsize; xx++) {
      for (int yy = 0; yy <= ysize; yy++) {

        if (xx > 0 && opts.isIncludeXEpsilons()) {
          for (int i = 1; i <= opts.getMaxXGram() && (xx - i) >= 0; i++) {
            String xGram = x.gram(xx - i, i);
            visitor.visit(xx - i, xx, xGram, yy, yy, Grams.EPSILON);
          }
        }

        if (yy > 0 && opts.isIncludeEpsilonYs()) {
          for (int j = 1; j <= opts.getMaxYGram() && (yy - j) >= 0; j++) {
            String yGram = y.gram(yy - j, j);
            visitor.visit(xx, xx, Grams.EPSILON, yy - j, yy, yGram);
          }
        }

        if (xx == 0 || yy == 0) {
          continue;
        }
        for (int i = 1; i <= opts.getMaxXGram() && (xx - i) >= 0; i++) {
          for (int j = 1; j <= opts.getMaxYGram() && (yy - j) >= 0; j++) {
            if (opts.isOnlyOneGrams()) {
              if (i > 1 && j > 1) {
                continue;
              }
            }
            int xGramIndex = xx - i;
            int yGramIndex = yy - j;
            String xGram = x.gram(xGramIndex, i);
            String yGram = y.gram(yGramIndex, j);
            visitor.visit(xGramIndex, xx, xGram, yGramIndex, yy, yGram);
          }
        }
      }
    }
  }

  @Override
  public void backward(Word x, Word y, Visitor visitor) {
    for (int xx = x.unigramCount(); xx >= 0; xx--) {
      for (int yy = y.unigramCount(); yy >= 0; yy--) {

        if (xx < x.unigramCount() && opts.isIncludeXEpsilons()) {
          for (int i = 1; i <= opts.getMaxXGram() && (xx + i <= x.unigramCount()); i++) {
            String xGram = x.gram(xx, i);
            visitor.visit(xx, xx + i, xGram, yy, yy, Grams.EPSILON);
          }
        }

        if (yy < y.unigramCount() && opts.isIncludeEpsilonYs()) {
          for (int j = 1; j <= opts.getMaxYGram() && (yy + j <= y.unigramCount()); j++) {
            String yGram = y.gram(yy, j);
            visitor.visit(xx, xx, Grams.EPSILON, yy, yy + j, yGram);
          }
        }

        if (xx == x.unigramCount() || yy == y.unigramCount()) {
          continue;
        }
        for (int i = 1; i <= opts.getMaxXGram() && (xx + i <= x.unigramCount()); i++) {
          for (int j = 1; j <= opts.getMaxYGram() && (yy + j <= y.unigramCount()); j++) {

            String xGram = x.gram(xx, i);
            String yGram = y.gram(yy, j);
            visitor.visit(xx, xx + i, xGram, yy, yy + j, yGram);
          }
        }
      }
    }
  }
}
