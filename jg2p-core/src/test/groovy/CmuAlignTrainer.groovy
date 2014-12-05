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

import com.github.steveash.jg2p.align.AlignerTrainer

/*
Script the builds the CMU alignment model and generates some stats
 */
println "I am " + (new File(".")).canonicalPath
def cmuFile = "cmudict.0.7a"
String[] args = [
    "--infile", "../resources/$cmuFile",
    "--outfile", "../../../target/cmua_2eps.model.dat",
    "--maxX", "2",
    "--maxY", "1",
    "--includeYEps",
    "--format", "CMU"
] as String[]
def model = AlignerTrainer.trainAndSave(args)
println "done with everything!"
