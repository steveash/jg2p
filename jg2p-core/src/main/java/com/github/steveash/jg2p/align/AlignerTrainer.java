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

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.github.steveash.jg2p.util.Assert.assertProb;
import static com.google.common.collect.Tables.immutableCell;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newCachedThreadPool;

/**
 * Owns the training algorithms for an Aligner
 *
 * @author Steve Ash
 */
public class AlignerTrainer {

  private static final Logger log = LoggerFactory.getLogger(AlignerTrainer.class);

  private final ProbTable counts = new ProbTable();
  private final ProbTable probs = new ProbTable();
  private final ProbTable originalCounts = new ProbTable();
  private final TrainOptions trainOpts;
  private final GramOptions gramOpts;
  private final XyWalker walker;
  private ProbTable labelledProbs;
  private final Set<Pair<String, String>> allowed;
  private final Set<Pair<String,String>> blocked;
  private final Penalizer penalizer;
  private ProbTable initFrom = null;

  public AlignerTrainer(TrainOptions trainOpts) {
    this(trainOpts, null);
  }

  public AlignerTrainer(TrainOptions trainOpts, XyWalker overrideWalker) {
    this.trainOpts = trainOpts;
    this.gramOpts = trainOpts.makeGramOptions();
    XyWalker w;
    if (overrideWalker == null) {
      if (trainOpts.useWindowWalker) {
        w = new WindowXyWalker(gramOpts);
      } else {
        w = new FullXyWalker(gramOpts);
      }
//      if (trainOpts.useSyllableTagger) {
//        w = new SyllPreserving(w);
//      }
    } else {
      w = overrideWalker;
    }
    if (trainOpts.alignAllowedFile != null) {
      try {
        this.allowed = FilterWalkerDecorator.readFromFile(trainOpts.alignAllowedFile);
        this.blocked = Sets.newHashSet();
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    } else {
      this.allowed = null;
      this.blocked = null;
    }
    this.walker = w;
    this.penalizer = gramOpts.makePenalizer();
  }

  public void setInitFrom(ProbTable initFrom) {
    this.initFrom = initFrom;
  }

  //  private static XyWalker decorateForAllowed(TrainOptions trainOpts, XyWalker w) {
//    try {
//      Set<Pair<String, String>> allowed = FilterWalkerDecorator.readFromFile(trainOpts.alignAllowedFile);
//      return new FilterWalkerDecorator(w, allowed);
//    } catch (IOException e) {
//      throw Throwables.propagate(e);
//    }
//  }

  public AlignModel train(List<InputRecord> records) {
    return train(records, new ProbTable());
  }

  public AlignModel train(List<InputRecord> records, ProbTable labelledExamples) {
    ListeningExecutorService service = listeningDecorator(newCachedThreadPool());
    try {
      this.labelledProbs = labelledExamples.makeNormalizedCopy();
      initCounts(records);
      maximization(); // this just initializes the probabilities for the first time

      int iteration = 0;
      boolean keepTraining = true;
      log.info("Starting EM rounds...");
      while (keepTraining) {
        iteration += 1;

        expectation(records, service);
        double thisChange = maximization();

        keepTraining = !hasConverged(thisChange, iteration);
        log.info("Completed EM round " + iteration + " mass delta " + String.format("%.15f", thisChange));
      }
      log.info("Training complete in " + iteration + " rounds!");
      return new AlignModel(gramOpts, probs);
    } finally {
      MoreExecutors.shutdownAndAwaitTermination(service, 60, TimeUnit.SECONDS);
    }
  }

  private boolean hasConverged(double thisChange, int iteration) {
    if (thisChange < trainOpts.probDeltaConvergenceThreshold) {
      log.info("EM only had a mass shift by " + thisChange + " training is complete.");
      return true;
    }
    if (iteration >= trainOpts.trainingAlignerMaxIterations) {
      return true;
    }
    return false;
  }

  private void expectation(List<InputRecord> records, ListeningExecutorService service) {
    int workerCount = Runtime.getRuntime().availableProcessors();
    List<ListenableFuture<ProbTable>> consumers = Lists.newArrayList();
    for (List<InputRecord> partition : Lists.partition(records, workerCount)) {
      consumers.add(service.submit(makeConsumer(partition)));
    }
    try {
      List<ProbTable> results = Futures.allAsList(consumers).get();
      ProbTable.mergeAll(results, counts);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private Callable<ProbTable> makeConsumer(final List<InputRecord> partition) {
    return new Callable<ProbTable>() {
      @Override
      public ProbTable call() throws Exception {
        ProbTable counts = new ProbTable();
        for (InputRecord inputRecord : partition) {
          expectationForRecord(inputRecord, counts);
        }
        return counts;
      }
    };
  }

  private void expectationForRecord(InputRecord record, final ProbTable outCounts) {
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
                      penalize(xGram, yGram, probs.prob(xGram, yGram)) *
                      beta.get(xxAfter, yyAfter) /
                      alphaXy;

        outCounts.addProb(xGram, yGram, prob);
      }
    });
  }

  private void backward(Word x, Word y, final DoubleTable beta) {
    beta.put(x.unigramCount(), y.unigramCount(), 1.0);
    walker.backward(x, y, new XyWalker.Visitor() {
      @Override
      public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
        double newBeta = penalize(xGram, yGram, probs.prob(xGram, yGram)) * beta.get(xxAfter, yyAfter);
        beta.add(xxBefore, yyBefore, newBeta);
      }
    });
  }

  private void forward(Word x, Word y, final DoubleTable alpha) {
    alpha.put(0, 0, 1.0);
    walker.forward(x, y, new XyWalker.Visitor() {
      @Override
      public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {
        double newAlpha = penalize(xGram, yGram, probs.prob(xGram, yGram)) * alpha.get(xxBefore, yyBefore);
        alpha.add(xxAfter, yyAfter, newAlpha);
      }
    });
  }

  private double penalize(String xGram, String yGram, double prob) {
    return penalizer.penalize(xGram, yGram, prob);
  }

  private double maximization() {
    smoothCounts();
    ProbTable.Marginals marginals = counts.calculateMarginals();
    double totalChange = 0;
    double unsuperFactor = (1.0 - trainOpts.semiSupervisedFactor);
    double superFactor = trainOpts.semiSupervisedFactor;

    for (Pair<String, String> xy : ProbTable.unionOfAllCells(counts, labelledProbs)) {
      String x = xy.getLeft();
      String y = xy.getRight();
      double countExp = counts.prob(x, y);
      double unsupervised = trainOpts.trainingAlignerMaximizer.maximize(immutableCell(x, y, countExp), marginals);
      double supervised = labelledProbs.prob(x, y);
      double update = (unsuperFactor * unsupervised) + (superFactor * supervised);
      assertProb(update);

      double current = probs.prob(x, y);
      totalChange += Math.abs(current - update);
      probs.setProb(x, y, update);
    }

    counts.clear();
    return trainOpts.trainingAlignerMaximizer.normalize(totalChange, marginals);
  }

  private void smoothCounts() {
    if (allowed == null) return;

    // do some kind of discounted smoothing where we add 0.5 * c / k * smallest entry) to every entry in the counts
    // where c is the count of good transitions and k is the total count of transitions.  And we're just going to
    // take half of that (arbitrarily)
    double c = allowed.size();
    double k = blocked.size();
    double discount = 2.0d * c / k;
    double toAdd = minAllowedCount() * discount;
    for (Pair<String, String> xy : allowed) {
      counts.addProb(xy.getLeft(), xy.getRight(), toAdd);
    }
    for (Pair<String, String> xy : blocked) {
      // we're forcing the blocked ones to be this small mass, whereas we're just adding the extra to the good xy
      counts.setProb(xy.getLeft(), xy.getRight(), toAdd);
    }
  }

  private double minAllowedCount() {
    double min = Double.POSITIVE_INFINITY;
    for (Pair<String, String> xy : allowed) {
      double p = counts.prob(xy.getLeft(), xy.getRight());
      if (p > 0 && p < min) {
        min = p;
      }
    }
    return min;
  }

  private void initCounts(List<InputRecord> records) {
    // we init counts for any allowed transitions and collect all of the transitions that we block
    counts.clear();
    originalCounts.clear();
    for (InputRecord record : records) {
      walker.forward(record.getLeft(), record.getRight(), new XyWalker.Visitor() {
        @Override
        public void visit(int xxBefore, int xxAfter, String xGram, int yyBefore, int yyAfter, String yGram) {

          double initValue = 1.0;
          if (initFrom != null) {
            double maybeInitFrom = initFrom.prob(xGram, yGram);
            if (maybeInitFrom > 0) {
              initValue = maybeInitFrom;
            }
          }
          originalCounts.addProb(xGram, yGram, initValue);
          if (allowed == null) {
            counts.addProb(xGram, yGram, initValue);
            return;
          }
          // use allowed file to constrain the joint distribution
          if (allowed.contains(Pair.of(xGram, yGram))) {
            counts.addProb(xGram, yGram, initValue);
          } else {
            blocked.add(Pair.of(xGram, yGram));
          }
        }
      });
    }
  }

  public int numberOfLowSupportAlignments(Alignment align, int lowSupport) {
    int count = 0;
    for (Pair<String, String> pair : align.getGraphones()) {
      double result = originalCounts.prob(pair.getLeft(), pair.getRight());
      if (result > 0 && result <= lowSupport) {
        count += 1;
      }
    }
    return count;
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
