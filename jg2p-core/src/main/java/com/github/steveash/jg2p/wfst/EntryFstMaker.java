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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.SymbolTable;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

import java.util.List;
import java.util.Map;

import static com.github.steveash.jg2p.wfst.SeqTransducer.ALL_SKIP_STRINGS;
import static com.github.steveash.jg2p.wfst.SeqTransducer.END;
import static com.github.steveash.jg2p.wfst.SeqTransducer.SEP;
import static com.github.steveash.jg2p.wfst.SeqTransducer.START;

/**
 * @author Steve Ash
 */
public class EntryFstMaker {

  private static final Splitter sepSplit = Splitter.on(SEP);
  private final ListAcceptor<String> clusterAcceptor;

  public EntryFstMaker(Iterable<String> symbols) {
    clusterAcceptor = new ListAcceptor<>(makeClusters(symbols));
  }

  private static ImmutableMap<ImmutableList<String>, String> makeClusters(Iterable<String> isyms) {
    ImmutableMap.Builder<ImmutableList<String>, String> builder = ImmutableMap.builder();
    for (String gram : isyms) {
      if (ALL_SKIP_STRINGS.contains(gram)) {
        continue;
      }
      List<String> grams = sepSplit.splitToList(gram);
      if (grams.size() <= 1) {
        continue;
      }
      builder.put(ImmutableList.copyOf(grams), gram);
    }
    return builder.build();
  }

  public MutableFst inputToFst(Word inputWord, SymbolTable inputSymbols) {

    MutableFst efst = new MutableFst(TropicalSemiring.INSTANCE);
    efst.setInputSymbolsAsCopy(inputSymbols);
    efst.setOutputSymbolsAsCopy(inputSymbols);
    MutableState prevState = efst.newStartState();
    MutableState sentStartState = efst.newState();
    MutableState nextState = sentStartState;
    efst.addArc(prevState, START, START, nextState, 0);
    // make single arcs for each letter in the input work
    for (String letter : inputWord) {
      prevState = nextState;
      nextState = efst.newState(); // the state ending in letter
      efst.addArc(prevState, letter, letter, nextState, 0);
    }
    // add the arc going to the </s> symbol
    MutableState endSentState = efst.newState();
    efst.addArc(nextState, END, END, endSentState, 0);
    endSentState.setFinalWeight(0);

    // go through and make a jump arc for each cluster that this input matches
    for (int i = 0; i < inputWord.unigramCount(); i++) {
      prevState = efst.getState(sentStartState.getId() + i); // this is the state before this match
      List<String> suffix = inputWord.getValue().subList(i, inputWord.unigramCount());
      List<Map.Entry<? extends List<String>, String>> matches = clusterAcceptor.accept(suffix);
      for (Map.Entry<? extends List<String>, String> match : matches) {
        // this cluster match will be a matching arc that will let us skip the unigram target states; remember
        // the unigram states for each letter are the ones with the letter consuming arc going into the state;
        // so sentState + i is the state _before_ the unigram arc that would otherwise consume that (next) letter
        // so the cluster should consume (match.size) letters and end in the state that would be the final state
        // after the equivalent number of unigram consumptions
        nextState = efst.getState(sentStartState.getId() + i + match.getKey().size());
        efst.addArc(prevState, match.getValue(), match.getValue(), nextState, 0);
      }
    }
    return efst;
  }
}
