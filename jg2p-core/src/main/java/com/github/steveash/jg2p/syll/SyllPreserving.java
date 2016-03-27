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

package com.github.steveash.jg2p.syll;import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.XyWalker;
import com.github.steveash.jg2p.syll.SWord;

public class SyllPreserving implements XyWalker {

  private final XyWalker delegate;

  public SyllPreserving(XyWalker delegate) {
    this.delegate = delegate;
  }

  @Override
  public void forward(Word x, final Word y, final Visitor visitor) {
    delegate.forward(x, y, new Visitor() {
      @Override
      public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
        if (isAllowed((SWord) y, yyBefore, yyAfter)) {
          visitor.visit(xxBefore, xxAfter, xGram, yyBefore, yyAfter, yGram);
        }
      }

    });
  }

  private static boolean isAllowed(SWord y, int yyStart, int yyEndExcl) {
    if (yyEndExcl - yyStart <= 1) {
      return true;
    }
    for (int i = 0; i < y.getBounds().length; i++) {
      int bound = y.getBounds()[i];
      if (bound <= yyStart) {
        continue;// we haven't gotten to the relevant bounds yet
      }
      if (bound >= yyEndExcl) {
        return true;// we are past any bounds that might matter
      }
      // this is a bad bound since it falls inside the phoneme that im trying to slice
      return false;
    }

    return true;
  }

  @Override
  public void backward(Word x, final Word y, final Visitor visitor) {
    delegate.backward(x, y, new Visitor() {
      @Override
      public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
        if (isAllowed((SWord) y, yyBefore, yyAfter)) {
          visitor.visit(xxBefore, xxAfter, xGram, yyBefore, yyAfter, yGram);
        }

      }

    });
  }
}
