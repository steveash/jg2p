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

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.util.DoubleTable;
import com.github.steveash.jg2p.util.ReadWrite;

import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.github.steveash.jg2p.util.Assert.assertProb;
import static com.google.common.collect.Tables.immutableCell;

/**
 * Owns the training algorithms for an Aligner
 *
 * @author Steve Ash
 */
public class AlignerTrainer {

  private static final Logger log = LoggerFactory.getLogger(AlignerTrainer.class);

  private final ProbTable counts = new ProbTable();
  private final ProbTable probs = new ProbTable();
  private final TrainOptions trainOpts;
  private final GramOptions gramOpts;
  private final XyWalker walker;
  private ProbTable labelledProbs;

  public AlignerTrainer(TrainOptions trainOpts) {
    this.trainOpts = trainOpts;
    this.gramOpts = trainOpts.makeGramOptions();
    this.walker = new FullXyWalker(gramOpts);
  }

  public AlignModel train(List<InputRecord> records) {
    return train(records, new ProbTable());
  }

  public AlignModel train(List<InputRecord> records, ProbTable labelledExamples) {
    this.labelledProbs = labelledExamples.makeNormalizedCopy();
    initCounts(records);
    maximization(); // this just initializes the probabilities for the first time

    int iteration = 0;
    boolean keepTraining = true;
    log.info("Starting EM rounds...");
    while (keepTraining) {
      iteration += 1;

      expectation(records);
      double thisChange = maximization();

      keepTraining = !hasConverged(thisChange, iteration);
      log.info("Completed EM round " + iteration + " mass delta " + String.format("%.15f", thisChange));
    }
    log.info("Training complete in " + iteration + " rounds!");
    return new AlignModel(gramOpts, probs);
  }

  private boolean hasConverged(double thisChange, int iteration) {
    if (thisChange < trainOpts.probDeltaConvergenceThreshold) {
      log.info("EM only had a mass shift by " + thisChange + " training is complete.");
      return true;
    }
    if (iteration >= trainOpts.maxIterations) {
      return true;
    }
    return false;
  }

  private void expectation(List<InputRecord> records) {
    for (InputRecord record : records) {
      expectationForRecord(record);
    }
  }

  private void expectationForRecord(InputRecord record) {
    Word x = record.xWord;
    Word y = record.yWord;
    int xsize = x.unigramCount();
    int ysize = y.unigramCount();
    final DoubleTable alpha = new DoubleTable(xsize + 1, ysize + 1);
    final DoubleTable beta = new DoubleTable(xsize + 1, ysize + 1);

    forward(x, y, alpha);
    backward(x, y, beta);

    final double alphaXy = alpha.get(xsize, ysize);
    if (alphaXy == 0) {
      return;
    }

    walker.forward(x, y, new XyWalker.Visitor() {
      @Override
      public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
        double prob = alpha.get(xxBefore, yyBefore) *
                      probs.prob(xGram, yGram) *
                      beta.get(xxAfter, yyAfter) /
                      alphaXy;

        counts.addProb(xGram, yGram, prob);
      }
    });
  }

  private void backward(Word x, Word y, final DoubleTable beta) {
    beta.put(x.unigramCount(), y.unigramCount(), 1.0);
    walker.backward(x, y, new XyWalker.Visitor() {
      @Override
      public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
        double newBeta = probs.prob(xGram, yGram) * beta.get(xxAfter, yyAfter);
        beta.add(xxBefore, yyBefore, newBeta);
      }
    });
  }

  private void forward(Word x, Word y, final DoubleTable alpha) {
    alpha.put(0, 0, 1.0);
    walker.forward(x, y, new XyWalker.Visitor() {
      @Override
      public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
        double newAlpha = probs.prob(xGram, yGram) * alpha.get(xxBefore, yyBefore);
        alpha.add(xxAfter, yyAfter, newAlpha);
      }
    });
  }

  private double maximization() {
    ProbTable.Marginals marginals = counts.calculateMarginals();
    double totalChange = 0;
    double unsuperFactor = (1.0 - trainOpts.semiSupervisedFactor);
    double superFactor = trainOpts.semiSupervisedFactor;

    for (Pair<String, String> xy : ProbTable.unionOfAllCells(counts, labelledProbs)) {
      String x = xy.getLeft();
      String y = xy.getRight();
      double countExp = counts.prob(x, y);
      double unsupervised = trainOpts.maximizer.maximize(immutableCell(x, y, countExp), marginals);
      double supervised = labelledProbs.prob(x, y);
      double update = (unsuperFactor * unsupervised) + (superFactor * supervised);
      assertProb(update);

      double current = probs.prob(x, y);
      totalChange += Math.abs(current - update);
      probs.setProb(x, y, update);
    }

    counts.clear();
    return trainOpts.maximizer.normalize(totalChange, marginals);
  }

  private void initCounts(List<InputRecord> records) {
    for (InputRecord record : records) {
      walker.forward(record.getLeft(), record.getRight(), new XyWalker.Visitor() {
        @Override
        public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
          counts.addProb(xGram, yGram, 1.0);
        }
      });
    }
  }

  public static void main(String[] args) {
    try {
      trainAndSave(args);
    } catch (Exception e) {
      log.error("Problem training ", e);
    }
  }

  public static AlignModel trainAndSave(String[] args) throws CmdLineException, IOException {
    TrainOptions opts = parseArgs(args);
    AlignerTrainer trainer = new AlignerTrainer(opts);

    log.info("Reading input training records...");
    InputReader reader = opts.makeReader();
    List<InputRecord> inputRecords = reader.readFromFile(opts.trainingFile);

    log.info("Training the probabilistic model...");
    AlignModel model = trainer.train(inputRecords);

    log.info("Writing model to " + opts.outputFile + "...");
    ReadWrite.writeTo(model, opts.outputFile);

    log.info("Training complete!");
    return model;
  }

  private static TrainOptions parseArgs(String[] args) throws CmdLineException {
    TrainOptions opts = new TrainOptions();
    CmdLineParser parser = new CmdLineParser(opts);
    parser.parseArgument(args);
    opts.afterParametersSet();
    return opts;
  }
}
