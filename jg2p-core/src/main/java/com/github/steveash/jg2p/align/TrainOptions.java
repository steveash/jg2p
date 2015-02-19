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

package com.github.steveash.jg2p.align;

import org.kohsuke.args4j.Option;

import java.io.File;

/**
 * Options for the training procedure; also defines the training command line arguments
 *
 * @author Steve Ash
 */
public class TrainOptions {

  public enum InputFormat { TAB, CMU }

  @Option(name = "--minX")
  public int minXGram = 1;  // these have to be 1 right now

  @Option(name = "--maxX")
  public int maxXGram = 2;

  @Option(name = "--minY")
  public int minYGram = 1;  // these have to be 1 right now

  @Option(name = "--maxY")
  public int maxYGram = 1;

  @Option(name = "--includeXEps")
  public boolean includeXEpsilons = false;

  @Option(name = "--includeEpsY")
  public boolean includeEpsilonYs = false;

  @Option(name = "--onlyOneGrams")
  public boolean onlyOneGrams = true;

  @Option(name = "--maximizer")
  public transient Maximizer maximizer = Maximizer.JOINT;

  @Option(name = "--maxTrainingIterations")
  public int maxIterations = 100;

  @Option(name = "--trainingConvergence")
  public double probDeltaConvergenceThreshold = 1.0e-5;

  @Option(name = "--maxCrfTrainingIterations")
  public int maxCrfIterations = 100;

  @Option(name = "--trimFeaturesUnderPercentile")
  public int trimFeaturesUnderPercentile = 0;

  @Option(name = "--initCrfFromModel")
  public String initCrfFromModelFile = null;

  @Option(name = "--semiSupervisedFactor")
  public double semiSupervisedFactor = 0.6;

  @Option(name = "--useWindowWalker")
  public boolean useWindowWalker = false;

  @Option(name= "--windowWalkerPadding")
  public int windowPadding = 0;

  @Option(name = "--topKAlignCandidates")
  public int topKAlignCandidates = 5;

  @Option(name = "--minAlignScore")
  public int minAlignScore = -150;

  @Option(name = "--infile", required = true)
  public File trainingFile;

  @Option(name = "--outfile")
  public File outputFile;

  @Option(name = "--format")
  public InputFormat format = InputFormat.TAB;

  public void afterParametersSet() {
    if (outputFile == null) {
      this.outputFile = new File(trainingFile.getAbsolutePath() + ".model.dat");
    }
  }

  public GramOptions makeGramOptions() {
    return new GramOptions(minXGram, maxXGram, minYGram, maxYGram, includeXEpsilons, includeEpsilonYs, onlyOneGrams,
                           windowPadding);
  }

  public InputReader makeReader() {
    if (format == InputFormat.CMU)
      return InputReader.makeCmuReader();

    return InputReader.makeDefaultFormatReader();
  }

  @Override
  public String toString() {
    return "TrainOptions{" +
           "\n\tminXGram=" + minXGram +
           ",\n\tmaxXGram=" + maxXGram +
           ",\n\tminYGram=" + minYGram +
           ",\n\tmaxYGram=" + maxYGram +
           ",\n\tincludeXEpsilons=" + includeXEpsilons +
           ",\n\tincludeEpsilonYs=" + includeEpsilonYs +
           ",\n\tonlyOneGrams=" + onlyOneGrams +
           ",\n\tmaximizer=" + maximizer +
           ",\n\tmaxIterations=" + maxIterations +
           ",\n\tprobDeltaConvergenceThreshold=" + probDeltaConvergenceThreshold +
           ",\n\tmaxCrfIterations=" + maxCrfIterations +
           ",\n\ttrimFeaturesUnderPercentile=" + trimFeaturesUnderPercentile +
           ",\n\tinitCrfFromModelFile='" + initCrfFromModelFile + '\'' +
           ",\n\tsemiSupervisedFactor=" + semiSupervisedFactor +
           ",\n\tuseWindowWalker=" + useWindowWalker +
           ",\n\twindowPadding=" + windowPadding +
           ",\n\ttopKAlignCandidates=" + topKAlignCandidates +
           ",\n\tminAlignScore=" + minAlignScore +
           ",\n\ttrainingFile=" + trainingFile +
           ",\n\toutputFile=" + outputFile +
           ",\n\tformat=" + format +
           '}';
  }
}
