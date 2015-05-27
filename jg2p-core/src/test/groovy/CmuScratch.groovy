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
import com.github.steveash.jg2p.phoseq.Phonemes
import com.github.steveash.jg2p.util.ListEditDistance
import com.google.common.collect.HashMultiset
import com.google.common.collect.Lists
import com.google.common.collect.Multisets

/**
 * Used to play with the failing examples to try and figure out some areas for improvement
 * @author Steve Ash
 */
def counts = HashMultiset.create()

new File("../resources/bad_rerank_missed.txt").eachLine { line ->
  def fields = line.split(",")
  def cand = Lists.newArrayList(fields[2].split("\\|"))
  def possibleAnswers = fields[3].split(" ~ ")
  def rank = fields[1] as int
  counts.add("__total")

  // find one candidate that fits (if possible)
  def candCons = stampVowels(cand);
  possibleAnswers.any { maybe ->
    def ans = Lists.newArrayList(maybe.split("\\|"))
    def ansCons = stampVowels(ans);
    if (candCons == ansCons) {
      counts.add("__structure_match")
      def edits = ListEditDistance.editDistance(cand, ans, 256)
      
      counts.add("__vowel_edits_" + edits)
      def vowelsHit = 0
      for (int i = 0; i < cand.size(); i++) {
        if (cand[i] != ans[i]) {
          counts.add("Vowel: " + cand[i] + " -> " + ans[i])
          if (edits == 1) {
            counts.add("__1edit_vowelsPosition_" + vowelsHit)
            int reportedRank = rank
            if (reportedRank > 5) reportedRank = 6
            counts.add("__1edit_rank_" + reportedRank)
          }
        }
        if (Phonemes.isVowel(cand[i])) {
          vowelsHit += 1
        }
      }
      return true
    }
    return false
  }
}
counts.entrySet().sort { it.element }.each {
  println it.element + " = " + it.count
}
Multisets.copyHighestCountFirst(counts).entrySet().each {
  println it.element + " = " + it.count
}
println "done"

List<String> stampVowels(ArrayList<String> phones) {
  phones.collect {
    if (Phonemes.isVowel(it)) {
      "VV"
    } else {
      it
    };
  }
}