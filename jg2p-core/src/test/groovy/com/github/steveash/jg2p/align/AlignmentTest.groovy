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

package com.github.steveash.jg2p.align

import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.util.Zipper
import org.junit.Test

/**
 * @author Steve Ash
 */
class AlignmentTest {

  @Test
  public void shouldProduceMarks() throws Exception {
    def s = Word.fromNormalString("STEVE")
    def a = new Alignment(s, Zipper.upTo(["S T", "E", "V", "E"], ""), 0)
    assert a.getXBoundaryMarksAsString() == "01111"

    def s1 = Word.fromNormalString("STEVE")
    def a1 = new Alignment(s1, Zipper.upTo(["S T", "E V", "E"], ""), 0)
    assert a1.getXBoundaryMarksAsString() == "01011"

    def s2 = Word.fromNormalString("STEVE")
    def a2 = new Alignment(s2, Zipper.upTo(["S T", "E V E"], ""), 0)
    assert a2.getXBoundaryMarksAsString() == "01001"
  }

  @Test
  public void shouldProduceStarts() throws Exception {
    def s = Word.fromNormalString("STEVE")
    def a = new Alignment(s, Zipper.upTo(["S T", "E", "V", "E"], ""), 0)
    assert a.getXStartMarksAsString() == "10111"

    def s1 = Word.fromNormalString("STEVE")
    def a1 = new Alignment(s1, Zipper.upTo(["S T", "E V", "E"], ""), 0)
    assert a1.getXStartMarksAsString() == "10101"

    def s2 = Word.fromNormalString("STEVE")
    def a2 = new Alignment(s2, Zipper.upTo(["S T", "E V E"], ""), 0)
    assert a2.getXStartMarksAsString() == "10100"
  }
}
