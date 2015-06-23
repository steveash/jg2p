import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord

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

/**
 * @author Steve Ash
 */
List<InputRecord> recs = []
recs.addAll(InputReader.makePSaurusReader().readFromClasspath("g014b2b.test"))
recs.addAll(InputReader.makePSaurusReader().readFromClasspath("g014b2b.train"))
println "Original lines " + recs.size()
List<Map.Entry<String, List<InputRecord>>> grouped = recs.groupBy { it.left.asSpaceString }.entrySet().toList()

int cut = (grouped.size() as double) * 0.90;
println "cutting at $cut"
for (int i = 0; i < 10; i++) {
  Collections.shuffle(grouped)
  new File("../resources/g014b2b_" + i + ".train").withPrintWriter { tr ->
    new File("../resources/g014b2b_" + i + ".test").withPrintWriter { ts ->
      for (int j = 0; j < grouped.size(); j++) {
        def entry = grouped.get(j).value
        if (j < cut) {
          writeTo(tr, entry)
        } else {
          writeTo(ts, entry)
        }
      }
    }
  }
}

def writeTo(PrintWriter pw, List<InputRecord> recs) {
recs.each { rec ->
pw.println(rec.left.value.join("") + "\t" + rec.right.asSpaceString)
}
}