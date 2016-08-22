import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap
import com.google.common.collect.Sets

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
def phfile = '/home/steve/code/Phonetisaurus/src/script/tt_1/langmodel.fst.txt'
def myfile = '/home/steve/code/jg2p/jg2p-core/src/test/groovy/modelfst.bottom.fst.txt'

private SetMultimap<String,String> parse(String file) {
  def mm = HashMultimap.create()
  new File(file).eachLine {line ->
    def fields = line.split("\\t")
    if (fields.length != 5) {
      if (fields.length == 1 || fields.length == 2) {
        // i think this is the state ending/starting costs...but where do they come from....
      } else {
        println "bad line $line got $fields"
      }
    } else {
      def vv = fields[3]
      if (vv == "MISSING") {
        vv = "<eps>"
      }
      mm.put(fields[2], vv)
    }
  }
  return mm
}

def pp = parse(phfile)
def mm = parse(myfile)

Sets.difference(pp.keySet(), mm.keySet()).each { println "pp - mm: " + it}
Sets.difference(mm.keySet(), pp.keySet()).each { println "mm - pp: " + it}

Sets.intersection(pp.keySet(), mm.keySet()).each { kk ->
  def ppp = pp.get(kk)
  def mmm = mm.get(kk)
  if (ppp.size() != mmm.size() || !Sets.symmetricDifference(ppp, mmm).isEmpty()) {
    println "For $kk got " + mmm + " by ph got $ppp"
  }
}
println "done"