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

import com.google.common.base.Preconditions;

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.Word;

/**
 * In this walker we don't visit all possible combinations of X & Y.  We ignore epsilons (assume they are rare) and only
 * visit the _possible_ combinations of X and Y given that we have to assign the other X's or Y's to _something_
 *
 * @author Steve Ash
 */
public class WindowXyWalker implements XyWalker {

  public WindowXyWalker(GramOptions opts) {
    this.opts = opts;
    Preconditions.checkArgument(!opts.isIncludeEpsilonYs(), "window walker doesnt support epsilon -> Y");
  }

  private final GramOptions opts;

  @Override
  public void forward(Word x, Word y, Visitor visitor) {
    int xsize = x.unigramCount();
    int ysize = y.unigramCount();

    // in the forward pass we only look at windows _behind_ us so the window is [xx - i, xx)
    for (int xx = 0; xx <= xsize; xx++) {
      for (int yy = 0; yy <= ysize; yy++) {

        if (yy > 0 && opts.isIncludeEpsilonYs()) {
          for (int j = 1; j <= opts.getMaxYGram() && (yy - j) >= 0; j++) {

            if (isValidForwardWindow(xx, yy, 0, j, xsize, ysize)) {
              String yGram = y.gram(yy - j, j);
              visitor.visit(xx, xx, Grams.EPSILON, yy - j, yy, yGram);
            }
          }
        }

        if (xx > 0 && opts.isIncludeXEpsilons()) {
          for (int i = 1; i <= opts.getMaxXGram() && (xx - i) >= 0; i++) {

            if (isValidForwardWindow(xx, yy, i, 0, xsize, ysize)) {
              String xGram = x.gram(xx - i, i);
              visitor.visit(xx - i, xx, xGram, yy, yy, Grams.EPSILON);
            }
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
            if (isValidForwardWindow(xx, yy, i, j, xsize, ysize)) {
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
  }

  private boolean isValidBackwardWindow(int xx, int yy, int i, int j, int xsize, int ysize) {
    return isValidForwardWindow(xx + i, yy + j, i, j, xsize, ysize);
  }

  // note that the xx and yy cursors are _after_ the window; i.e. window would be [xx - i, xx)
  private boolean isValidForwardWindow(int xx, int yy, int i, int j, int xsize, int ysize) {
    int minPhonesAfterX = (int) Math.ceil(((double) (xsize - xx)) / (double) opts.getMaxXGram());
    int minPhonesBeforeX = (int) Math.max(0, Math.ceil(((double) (xx - i)) / (double) opts.getMaxXGram()));

    // is this going to be good from X to Y
    if (((yy - j) < minPhonesBeforeX) || ((ysize - yy) < minPhonesAfterX)) {
      return false;
    }

    // is this going to be good from Y to X
    int minGraphsAfterY = (int) Math.ceil(((double) (ysize - yy)) / (double) opts.getMaxYGram());
    int minGraphsBeforeY = (int) Math.max(0, Math.ceil(((double) (yy - j)) / (double) opts.getMaxYGram()));

    // if i choose this x window are there enough graphs to satisfy min Y reqs (and vice versa)
    if (((xx - i) < minGraphsBeforeY) || ((xsize - xx) < minGraphsAfterY)) {
      return false;
    }

    return true;
  }

  @Override
  public void backward(Word x, Word y, Visitor visitor) {
    int xsize = x.unigramCount();
    int ysize = y.unigramCount();

    for (int xx = xsize; xx >= 0; xx--) {
      for (int yy = ysize; yy >= 0; yy--) {

        if (yy < y.unigramCount() && opts.isIncludeEpsilonYs()) {
          for (int j = 1; j <= opts.getMaxYGram() && (yy + j <= y.unigramCount()); j++) {

            if (isValidBackwardWindow(xx, yy, 0, j, xsize, ysize)) {
              String yGram = y.gram(yy, j);
              visitor.visit(xx, xx, Grams.EPSILON, yy, yy + j, yGram);
            }
          }
        }

        if (xx < xsize && opts.isIncludeXEpsilons()) {
          for (int i = 1; i <= opts.getMaxXGram() && (xx + i <= xsize); i++) {

            if (isValidBackwardWindow(xx, yy, i, 0, xsize, ysize)) {
              String xGram = x.gram(xx, i);
              visitor.visit(xx, xx + i, xGram, yy, yy, Grams.EPSILON);
            }
          }
        }

        if (xx == xsize || yy == ysize) {
          continue;
        }
        for (int i = 1; i <= opts.getMaxXGram() && (xx + i <= xsize); i++) {
          for (int j = 1; j <= opts.getMaxYGram() && (yy + j <= ysize); j++) {
            if (opts.isOnlyOneGrams()) {
              if (i > 1 && j > 1) {
                continue;
              }
            }
            if (isValidBackwardWindow(xx, yy, i, j, xsize, ysize)) {
              String xGram = x.gram(xx, i);
              String yGram = y.gram(yy, j);
              visitor.visit(xx, xx + i, xGram, yy, yy + j, yGram);
            }
          }
        }
      }
    }
  }
}
