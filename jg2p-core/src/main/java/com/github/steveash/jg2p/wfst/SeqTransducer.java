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

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import com.github.steveash.jg2p.Word;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.ImmutableFst;
import com.github.steveash.jopenfst.ImmutableSymbolTable;
import com.github.steveash.jopenfst.MutableArc;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.io.Convert;
import com.github.steveash.jopenfst.operations.ArcSort;
import com.github.steveash.jopenfst.operations.Compose;
import com.github.steveash.jopenfst.operations.NShortestPaths;
import com.github.steveash.jopenfst.operations.Project;
import com.github.steveash.jopenfst.operations.ProjectType;
import com.github.steveash.jopenfst.operations.RemoveEpsilon;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A WFST based transducer to do structured coding from X to Y where the input
 * symbols might have a M:N alignment (like in G2P)
 * @author Steve Ash
 */
public class SeqTransducer implements Serializable {

  public static final String SEP = "|";
  public static final String START_STATE = "<start>";
  public static final String START = "<s>";
  public static final String END = "</s>";
  public static final String SKIP = "_";
  public static final String GRAPHONE_DELIM = "}";
  public static final ImmutableSet<String> ALL_SKIP_STRINGS = ImmutableSet.of(SEP, START, END, SKIP);

  private static final double precision = 0.85;
  private static final double ratio = 0.72;
  private static final int beamWidth = 1500;

  private final ImmutableFst fst;
  private final ImmutableBiMap<String, Integer> skipInputIndexes;
  private final ImmutableFst epsMapper;
  private final EntryFstMaker entryMaker;
  private final int order;

  public SeqTransducer(ImmutableFst fst, int order) {
    this.fst = fst;
    this.order = order;
    ImmutableSymbolTable isyms = this.fst.getInputSymbols();
    ImmutableSymbolTable osyms = this.fst.getOutputSymbols();
    ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
    for (String skipString : ALL_SKIP_STRINGS) {
      builder.put(skipString, isyms.get(skipString));
    }
    skipInputIndexes = builder.build();
    epsMapper = makeEpsMapper(osyms, skipInputIndexes.keySet());
    entryMaker = new EntryFstMaker(fst.getInputSymbols().symbols());
  }

  private static ImmutableFst makeEpsMapper(ImmutableSymbolTable osyms, ImmutableSet<String> skipLabels) {
    MutableFst epsMapper = new MutableFst();
    epsMapper.setInputSymbolsAsCopy(osyms);
    epsMapper.setOutputSymbolsAsCopy(osyms);
    MutableState start = epsMapper.newStartState();
    int oeps = osyms.get(Fst.EPS);
    for (ObjectIntCursor<String> cursor : osyms) {
      int olabel = cursor.value;
      if (skipLabels.contains(cursor.key)) {
        olabel = oeps;
      }
      start.addArc(new MutableArc(cursor.value, olabel, 0, start));
    }
    start.setFinalWeight(0.0);
    ArcSort.sortByInput(epsMapper);
    return new ImmutableFst(epsMapper);
  }

  public ImmutableFst getFst() {
    return fst;
  }

  public int getOrder() {
    return order;
  }

  public List<WordResult> translate(Word inputWord, int topKResults) {
    throwIfInvalidInput(inputWord);
    MutableFst efst = entryMaker.inputToFst(inputWord, fst.getInputSymbols());
//    double[] thetas = computeThetas(inputWord.unigramCount());
//    int n = Math.min(inputWord.unigramCount() + 1, order);
//    MutableFst allFst = MutableFst.copyFrom(this.fst);
    MutableFst composed = Compose.compose(efst, MutableFst.copyFrom(this.fst), TropicalSemiring.INSTANCE);
//    Convert.export(composed, "composed.fst");
    Project.apply(composed, ProjectType.OUTPUT);

    MutableFst shortestPaths;
    if (topKResults > 1) {
      shortestPaths = NShortestPaths.apply(composed, beamWidth);
    } else {
      shortestPaths = NShortestPaths.apply(composed, 1);
    }
//    Convert.export(shortestPaths, "shortestpath.fst");
    MutableFst finalLattice = RemoveEpsilon.remove(shortestPaths);
//    Convert.export(finalLattice, "finallattice.fst");
    List<PathDecoder.CandidatePath> bestPaths =
        new PathDecoder(skipInputIndexes.keySet()).decodeBest(finalLattice);
    List<PathDecoder.CandidatePath> sortedBest = Ordering.natural().sortedCopy(bestPaths);
    return convertResults(sortedBest.subList(0, Math.min(topKResults, sortedBest.size())));
  }

  private List<WordResult> convertResults(List<PathDecoder.CandidatePath> bestPaths) {
    ArrayList<WordResult> results = Lists.newArrayListWithCapacity(bestPaths.size());
    for (PathDecoder.CandidatePath path : bestPaths) {
      results.add(new WordResult(Word.fromGrams(path.getPathStates()), path.getCost()));
    }
    return results;
  }


  private double[] computeThetas(int count) {
    /*
        Theta values are computed on a per-word basis
        We scale the maximum order by the length of the input word.
        Higher MBR N-gram orders favor longer pronunciation hypotheses.
        Thus a high N-gram order coupled with a short word will
        favor longer pronunciations with more insertions.

          p=.63, r=.48
          p=.85, r=.72
        .918
        Compute the N-gram Theta factors for the
        model.  These are a function of,
          N:  The maximum N-gram order
          T:  The total number of 1-gram tokens
          p:  The 1-gram precision
          r:  A constant ratio

        1) T may be selected arbitrarily.
        2) Default values are selected from Tromble 2008
      */
    int n = Math.min(count + 1, order);
    double t = 10.0;
    double[] thetas = new double[order + 1];
    thetas[0] = -1.0 / t;
    for (int i = 1; i <= order; i++) {
      thetas[i] = 1.0 / ((n * t * precision) * (Math.pow(ratio, (i - 1))));
    }
    return thetas;
  }

  private void throwIfInvalidInput(Word word) {
    word.throwIfNotUnigram();
    for (String gram : word.getValue()) {
      if (!fst.getInputSymbols().contains(gram)) {
        throw new IllegalArgumentException("Input word " + word.getAsSpaceString() + " contains gram " + gram +
                                           " that isn't in the symbol table for the transducer");
      }
    }
  }

  private Object writeReplace() {
    return new SeqTransducerProxy(this);
  }

  private void readObject(ObjectInputStream stream) throws InvalidObjectException {
    throw new InvalidObjectException("Proxy required");
  }
}
