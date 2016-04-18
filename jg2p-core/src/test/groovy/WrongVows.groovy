import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.phoseq.Phonemes
import com.google.common.base.CharMatcher
import com.google.common.collect.HashMultiset

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

/**
 * @author Steve Ash
 */

// lets make two histograms one with first two vowels patterns that we get right
// one with the first two vowel patterns that we get wrong
def bad = new File ("../resources/badexamples-20160413051113.txt")
def goo = new File("../resources/g014b2b.test")

bad = bad.readLines().drop(1).collect {it.split("\\t")}.collectEntries {
  [Word.fromNormalString(it[0]), Word.fromSpaceSeparated(it[3].split("->")[1].trim())]
}
goo = goo.readLines().collect {it.split("\\t")}.collectEntries {
  [Word.fromNormalString(it[0]),Word.fromSpaceSeparated(it[1])]
}

int splitCount = 0;
int mergedCount = 0
goo.each {Word k, Word v ->
  for (int i = 0; i < v.unigramCount(); i++) {
    if (i < (v.unigramCount() - 1) && v.getValue()[i] == 'EH' && v.getValue()[i+1] == 'R') {
      splitCount += 1
      return
    }
    if (v.getValue()[i] == 'ER') {
      mergedCount += 1
      return
    }
  }
}
println "Words with split $splitCount, words with merged $mergedCount"
/*
def only2 = [:]
bad.each {k, v ->
  def exp = goo[k]
  if (isOnlyWrongFirst2(v, exp)) {
    only2.put(k, v)
  }
}
println "The only 1 bucket is " + only2.size()

def rgx = goo.collectEntries { k, v -> [k, xform(v)]}
def o2x = only2.collectEntries {k, v -> [k, xform(v)]}

o2x.keySet().each { rgx.remove(it) }
//println "wrx is ${wrx.size()}, rgx is ${rgx.size()}"

def o2c = HashMultiset.create(o2x.values())
def rgc = HashMultiset.create(rgx.values())

o2c.entrySet().take(20).each {
  def goodcount = rgc.count(it.element)
  println "pattern " + it.element + " good count " + goodcount + ", bad " + it.count
}

def isOnlyWrongFirst2(Word actual, Word expected) {
  int vc = 0
  if (actual.unigramCount() != expected.unigramCount()) return false
  def vowelsbad = [].toSet()
  for (int i = 0; i < actual.unigramCount(); i++) {
    def aa = actual.value[i]
    def ee = expected.value[i]
    if (Phonemes.isVowel(aa) && Phonemes.isVowel(ee)) {
      if (aa != ee) {
        vowelsbad << vc
      }
      vc++
    } else {
      if (Phonemes.isVowel(aa) || Phonemes.isVowel(ee)) {
        return false
      }
      if (aa != ee) {
        return false
      }
    }
  }
  // we've made it through and the only things that are wrong are vowels
  if (vowelsbad.size() == 1 && vowelsbad.contains(0)) {
    return true
  }
  return false
}

def xform(Word input) {
  StringBuilder sb = new StringBuilder()
  int vc = 0
  input.each {
    if (vc < 1 && Phonemes.isVowel(it)) {
      sb.append(it)
      vc += 1
    } else {
      sb.append("c")
    }
  }
  return sb.toString()
//  return CharMatcher.anyOf("c").collapseFrom(sb.toString(), 'c' as char)
}
*/