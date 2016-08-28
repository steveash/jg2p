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

package com.github.steveash.jg2p.wfst;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.primitives.Doubles;

import com.github.steveash.jopenfst.ImmutableFst;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.WriteableSymbolTable;
import com.github.steveash.jopenfst.operations.ArcSort;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;
import com.github.steveash.kylm.model.ngram.NgramLM;
import com.github.steveash.kylm.model.ngram.NgramWalker;
import com.github.steveash.kylm.model.ngram.WalkerVisitor;
import com.github.steveash.kylm.model.ngram.reader.ArpaNgramReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static com.github.steveash.jopenfst.Fst.EPS;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.last;

/**
 * Converts a lang model to an FST
 *
 * @author Steve Ash
 */
public class LangModelToFst {

  public static final double REALLY_HIGH = 999.0;
  public static final double PRETTY_HIGH = 99.0;

  private final Splitter graphoneSplitter = Splitter.on(SeqTransducer.GRAPHONE_DELIM).trimResults().limit(2);
  private final Joiner commaJoin = Joiner.on(',');

  private MutableFst fst;
  private int maxOrder;

  public SeqTransducer fromArpa(File arpaFile) {
    ArpaNgramReader reader = new ArpaNgramReader();
    try (BufferedReader br = Files.newBufferedReader(arpaFile.toPath(), Charsets.UTF_8)) {
      NgramLM model = reader.read(br);
      return fromModel(model);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public SeqTransducer fromModel(NgramLM model) {
    Preconditions.checkNotNull(model.getStartSymbol(), "must use start symbol");
    Preconditions.checkNotNull(model.getTerminalSymbol(), "must use terminal symbol");
    Preconditions.checkArgument(model.getStartSymbol().equals(SeqTransducer.START), "Only using start %s", SeqTransducer.START);
    Preconditions.checkArgument(model.getTerminalSymbol().equals(SeqTransducer.END), "Only using end %s", SeqTransducer.END);

    this.maxOrder = model.getN();
    this.fst = new MutableFst(TropicalSemiring.INSTANCE);
    fst.useStateSymbols();
    fst.newStartState(SeqTransducer.START_STATE);
    fst.newState(SeqTransducer.END).setFinalWeight(TropicalSemiring.INSTANCE.one());

    for (String sym : SeqTransducer.ALL_SKIP_STRINGS) {
      fst.getInputSymbols().getOrAdd(sym);
      fst.getOutputSymbols().getOrAdd(sym);
    }

    addArc(SeqTransducer.START_STATE, SeqTransducer.START, SeqTransducer.START, SeqTransducer.START, 0.0);
    // add stuff for each order counts
    Preconditions.checkState(maxOrder > 1, "cant work with a unigram model");
    new NgramWalker(model).walk(new WalkerVisitor() {
      @Override
      public void visit(int order, List<String> toks, float score, float backoffScore, boolean hasChildren,
                        boolean isLastOrder) {
        Preconditions.checkState(order == toks.size());
        if (order == 1) {
          String s = toks.get(0);
          if (s.equalsIgnoreCase(SeqTransducer.START)) {
            addArc(SeqTransducer.START, EPS, EPS, EPS, backoffScore);
          } else if (s.equalsIgnoreCase(SeqTransducer.END)) {
            addArc(EPS, SeqTransducer.END, SeqTransducer.END, SeqTransducer.END, score);
          } else {
            addArc(s, EPS, EPS, EPS, backoffScore);
            addArc(EPS, s, s, s, score);
          }
          return;
        }
        String last = last(toks);
        if (last.equalsIgnoreCase(SeqTransducer.END)) {
          // last in a sentence just ends in the terminal
          addArc(commaJoin.join(toks.subList(0, toks.size() - 1)),
                 last, last, last, score);
          return;
        }
        if (isLastOrder) {
          // we're moving one step
          addArc(commaJoin.join(toks.subList(0, toks.size() - 1)),
                 commaJoin.join(toks.subList(1, toks.size())),
                 last,
                 last,
                 score
          );
          return;
        }
        // we are in the middle so emit an arc for the backoff and an arc for the score
        addArc(commaJoin.join(toks),
               commaJoin.join(toks.subList(1, toks.size())),
               EPS,
               EPS,
               backoffScore
        );
        addArc(commaJoin.join(toks.subList(0, toks.size()-1)),
               commaJoin.join(toks),
               last,
               last,
               score
        );
      }
    });

    patchSymbols(fst.getInputSymbols(), true);
    patchSymbols(fst.getOutputSymbols(), false);

    ArcSort.sortByInput(fst);
    fst.dropStateSymbols(); // we dont need these for test time
    return new SeqTransducer(new ImmutableFst(fst), this.maxOrder);
  }

  // psaurus does this...but for the O-labels i dont really get this ...
  private void patchSymbols(WriteableSymbolTable symbols, boolean isInput) {
    for (int i = 0; i < symbols.size(); i++) {
      if (!symbols.invert().containsKey(i)) {
        continue;
      }
      String symbol = symbols.invert().keyForId(i);
      if (symbol.contains(SeqTransducer.GRAPHONE_DELIM)) {
        for (String unigram : graphoneSplitter.split(symbol)) {
          if (symbols.contains(unigram)) {
            continue;
          }
          // this unigram doesn't exist so add a backoff edge to the start
          if (isInput) {
            fst.addArc(SeqTransducer.START, unigram, SeqTransducer.SKIP, SeqTransducer.START_STATE, PRETTY_HIGH);
          } else {
            fst.addArc(SeqTransducer.START, SeqTransducer.SKIP, unigram, SeqTransducer.START_STATE, PRETTY_HIGH);
          }
        }
      }
    }
  }

  private void addArc(String thisStateSymbol, String nextStateSymbol, String inLabel, String outLabel, double weight) {
    weight = tropicalWeight(weight);
    List<String> result = graphoneSplitter.splitToList(inLabel);
    String correctedIn = inLabel.trim();
    String correctedOut = outLabel.trim();
    if (result.size() > 1) {
      Preconditions.checkState(result.size() == 2, "we only support X:Y split or X:Y in input");
      correctedIn = result.get(0);
      correctedOut = result.get(1);
    }
    if (isBlank(correctedIn)) {
      correctedIn = EPS;
    }
    if (isBlank(correctedOut)) {
      correctedOut = EPS;
    }
    fst.addArc(thisStateSymbol, correctedIn, correctedOut, nextStateSymbol, weight);
  }

  private static double tropicalWeight(double inWeight) {
    double val = Math.log(10.0) * inWeight * -1.0;
    if (!Doubles.isFinite(val)) {
      val = REALLY_HIGH;
    }
    return val;
  }
}
