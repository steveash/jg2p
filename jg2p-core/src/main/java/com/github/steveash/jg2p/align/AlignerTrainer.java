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

import com.google.common.collect.Table;

import com.github.steveash.jg2p.Grams;
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

/**
 * Owns the training algorithms for an Aligner
 * @author Steve Ash
 */
public class AlignerTrainer {
  private static final Logger log = LoggerFactory.getLogger(AlignerTrainer.class);

  private final ProbTable counts = new ProbTable();
  private final ProbTable probs = new ProbTable();
  private final TrainOptions trainOpts;
  private final GramOptions gramOpts;

  public AlignerTrainer(TrainOptions trainOpts) {
    this.trainOpts = trainOpts;
    this.gramOpts = trainOpts.makeGramOptions();
  }

  public AlignModel train(List<InputRecord> records) {
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
    DoubleTable alpha = new DoubleTable(xsize + 1, ysize + 1);
    DoubleTable beta = new DoubleTable(xsize + 1, ysize + 1);

    forward(x, y, alpha);
    backward(x, y, beta);

    double alphaXy = alpha.get(xsize, ysize);
    if (alphaXy == 0) {
      return;
    }

    for (int xx = 0; xx <= xsize; xx++) {
      for (int yy = 0; yy <= ysize; yy++) {

        if (xx > 0 && gramOpts.isIncludeXEpsilons()) {
          for (int i = 1; i <= gramOpts.getMaxXGram() && (xx - i) >= 0; i++) {

            String xGram = x.gram(xx - i, i);
            double prob = alpha.get(xx - i, yy) * probs.prob(xGram, Grams.EPSILON) * beta.get(xx, yy) / alphaXy;
            counts.addProb(xGram, Grams.EPSILON, prob);
          }
        }

        if (yy > 0 && gramOpts.isIncludeEpsilonYs()) {
          for (int j = 1; j <= gramOpts.getMaxYGram() && (yy - j) >= 0; j++) {

            String yGram = y.gram(yy - j, j);
            double prob = alpha.get(xx, yy - j) * probs.prob(Grams.EPSILON, yGram) * beta.get(xx, yy) / alphaXy;
            counts.addProb(Grams.EPSILON, yGram, prob);
          }
        }

        if (xx == 0 || yy == 0) {
          continue;
        }
        for (int i = 1; i <= gramOpts.getMaxXGram() && (xx - i) >= 0; i++) {
          for (int j = 1; j <= gramOpts.getMaxYGram() && (yy - j) >= 0; j++) {
            int xGramIndex = xx - i;
            int yGramIndex = yy - j;
            String xGram = x.gram(xGramIndex, i);
            String yGram = y.gram(yGramIndex, j);
            double prob = alpha.get(xGramIndex, yGramIndex) * probs.prob(xGram, yGram) * beta.get(xx, yy) / alphaXy;
            counts.addProb(xGram, yGram, prob);

          }
        }
      }
    }
  }

  private void backward(Word x, Word y, DoubleTable beta) {
    beta.put(x.unigramCount(), y.unigramCount(), 1.0);
    for (int xx = x.unigramCount(); xx >= 0; xx--) {
      for (int yy = y.unigramCount(); yy >= 0; yy--) {

        if (xx < x.unigramCount() && gramOpts.isIncludeXEpsilons()) {
          for (int i = 1; i <= gramOpts.getMaxXGram() && (xx + i <= x.unigramCount()); i++) {
            String xGram = x.gram(xx, i);
            double newBeta = probs.prob(xGram, Grams.EPSILON) * beta.get(xx + i, yy);
            beta.add(xx, yy, newBeta);
          }
        }

        if (yy < y.unigramCount() && gramOpts.isIncludeEpsilonYs()) {
          for (int j = 1; j <= gramOpts.getMaxYGram() && (yy + j <= y.unigramCount()); j++) {
            String yGram = y.gram(yy, j);
            double newBeta = probs.prob(Grams.EPSILON, yGram) * beta.get(xx, yy + j);
            beta.add(xx, yy, newBeta);
          }
        }

        if (xx == x.unigramCount() || yy == y.unigramCount()) {
          continue;
        }
        for (int i = 1; i <= gramOpts.getMaxXGram() && (xx + i <= x.unigramCount()); i++) {
          for (int j = 1; j <= gramOpts.getMaxYGram() && (yy + j <= y.unigramCount()); j++) {

            String xGram = x.gram(xx, i);
            String yGram = y.gram(yy, j);
            double newBeta = probs.prob(xGram, yGram) * beta.get(xx + i, yy + j);
            beta.add(xx, yy, newBeta);

          }
        }
      }
    }
  }

  private void forward(Word x, Word y, DoubleTable alpha) {
    alpha.put(0, 0, 1.0);
    for (int xx = 0; xx <= x.unigramCount(); xx++) {
      for (int yy = 0; yy <= y.unigramCount(); yy++) {

        // 0 is the edge of the table; so index 0 in the Word will be index 1 in the table
        if (xx > 0 && gramOpts.isIncludeXEpsilons()) {
          for (int i = 1; i <= gramOpts.getMaxXGram() && (xx - i) >= 0; i++) {
            // the current i is the # of chars to look back to build a gram which works out since xx is 1-based
            int xGramIndex = xx - i;
            String xGram = x.gram(xGramIndex, i);
            double newAlpha = probs.prob(xGram, Grams.EPSILON) * alpha.get(xGramIndex, yy);
            alpha.add(xx, yy, newAlpha);
          }
        }

        if (yy > 0 && gramOpts.isIncludeEpsilonYs()) {
          for (int j = 1; j <= gramOpts.getMaxYGram() && (yy - j) >= 0; j++) {
            // the current i is the # of chars to look back to build a gram which works out since yy is 1-based
            int yGramIndex = yy - j;
            String yGram = y.gram(yGramIndex, j);
            double newAlpha = probs.prob(Grams.EPSILON, yGram) * alpha.get(xx, yGramIndex);
            alpha.add(xx, yy, newAlpha);
          }
        }

        if (xx == 0 || yy == 0) {
          continue;
        }
        for (int i = 1; i <= gramOpts.getMaxXGram() && (xx - i) >= 0; i++) {
          for (int j = 1; j <= gramOpts.getMaxYGram() && (yy - j) >= 0; j++) {
            int xGramIndex = xx - i;
            int yGramIndex = yy - j;
            String xGram = x.gram(xGramIndex, i);
            String yGram = y.gram(yGramIndex, j);
            double newAlpha = probs.prob(xGram, yGram) * alpha.get(xGramIndex, yGramIndex);
            alpha.add(xx, yy, newAlpha);

          }
        }
      }
    }
  }

  private double maximization() {
    ProbTable.Marginals marginals = counts.calculateMarginals();
    double totalChange = 0;

    for (Table.Cell<String, String, Double> cell : counts) {
      double update = trainOpts.maximizer.maximize(cell, marginals);
      totalChange += Math.abs(probs.prob(cell.getRowKey(), cell.getColumnKey()) - update);
      probs.setProb(cell.getRowKey(), cell.getColumnKey(), update);
    }

    counts.clear();
    return trainOpts.maximizer.normalize(totalChange, marginals);
  }

  private void initCounts(List<InputRecord> records) {
    for (Pair<String, String> gram : Grams.wordPairsToAllGrams(records, gramOpts)) {
      // TODO(SA) why wouldn't I increment here?
      counts.setProb(gram.getLeft(), gram.getRight(), 1);
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
