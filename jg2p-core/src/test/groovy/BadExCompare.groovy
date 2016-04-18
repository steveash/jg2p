import com.google.common.base.Splitter
import com.google.common.collect.Sets
import org.apache.commons.lang3.StringUtils

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

def fileA = "../resources/badexamples_all_shapes.txt"
def fileB = "../resources/badexamples-20160414143006-apparently-best-recently.txt"

def rowsA = readRows(fileA)
def rowsB = readRows(fileB)

Map<String,Map<String,String>> readRows(String filePath) {
  def split = Splitter.on('\t')
  def header = split.splitToList("word\tedits\trank\tprediction\texpected")
  return new File(filePath).readLines().drop(1).collect { line ->
    [header, split.splitToList(line)].transpose().collectEntries()
  }.collectEntries { [it['word'], it]}
}

println "A missed ${rowsA.size()}, B missed ${rowsB.size()}"
println "Both missed " + Sets.intersection(rowsA.keySet(), rowsB.keySet()).size() + " in common"
println "-------Entries in A but not in B-------"
printDiff(rowsA, rowsB)
println "-------Entries in B but not in A-------"
printDiff(rowsB, rowsA)


public printDiff(Map<String, Map<String, String>> rowsA, Map<String, Map<String, String>> rowsB) {

  def delta = Sets.difference(rowsA.keySet(), rowsB.keySet())
  def max = delta.collect { it.length() }.max()

  delta.sort().collate(5).
      each {
        println it.collect { StringUtils.rightPad(it, max + 1) }.join(" ")
      }
}

