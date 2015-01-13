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

package com.github.steveash.jg2p.align

import com.github.steveash.jg2p.Word
import org.junit.Test

/**
 * @author Steve Ash
 */
class WindowXyWalkerTest {

  def v = new XyWalker.Visitor() {

    @Override
    void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
      println "($xxBefore,$xxAfter) $xGram -> ($yyBefore,$yyAfter) $yGram"
    }
  }

  def steve = Word.fromNormalString("STEVE")
  def stev = Word.fromNormalString("STEV")
  private opts = new GramOptions(1, 2)
  def w2 = new WindowXyWalker(opts)

  @Test
  public void shouldIterateOverWindows() throws Exception {
    w2.forward(steve, stev, v)
  }

  static class RecordingVisitor implements XyWalker.Visitor {
    def hit = [].toSet()

    @Override
    void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
      hit.add("$xxBefore,$xxAfter,$yyBefore,$yyAfter")
    }
  }

  @Test
  public void shouldHitSameWindows() throws Exception {
    def r1 = new RecordingVisitor()
    def r2 = new RecordingVisitor()

    w2.forward(steve, stev, r1)
    w2.backward(steve, stev, r2)

    assert r1.hit.size() > 0
    assert r1.hit == r2.hit
  }
}
