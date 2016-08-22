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

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

import com.github.steveash.jg2p.util.ListOrdering;
import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.SymbolTable;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Decodes the WFST lattice and produces path objets with total costs; assumes wfst has already had shortest path
 * executed
 * @author Steve Ash
 */
public class PathDecoder {

  private static final TropicalSemiring RING = TropicalSemiring.INSTANCE;

  public static class CandidatePath implements Comparable<CandidatePath> {

    private final ImmutableList<String> pathStates;
    private final double cost;

    public CandidatePath(List<String> pathStates, double cost) {
      this.pathStates = ImmutableList.copyOf(pathStates);
      this.cost = cost;
    }

    public ImmutableList<String> getPathStates() {
      return pathStates;
    }

    public double getCost() {
      return cost;
    }

    @Override
    public int compareTo(CandidatePath o) {
      return ComparisonChain.start()
          .compare(this.cost, o.cost)
          // shorter paths first, if same then do element wise comparison
          .compare(this.pathStates.size(), o.pathStates.size())
          .compare(this.pathStates, o.pathStates, ListOrdering.getInstance(String.class))
          .result();
    }
  }

  private final Set<String> skipLabels;
  private final ArrayList<CandidatePath> outputs = Lists.newArrayList();
  private SymbolTable.InvertedSymbolTable inputLabels;

  public PathDecoder(Set<String> skipLabels) {
    this.skipLabels = skipLabels;
  }

  public List<CandidatePath> decodeBest(Fst lattice) {
    this.outputs.clear();
    this.inputLabels = lattice.getInputSymbols().invert();
    decodeStep(lattice.getStartState(), new LinkedList<String>(), RING.one());
    this.inputLabels = null;
    return outputs;
  }

  private void decodeStep(State from, Deque<String> path, double cost) {
    if (isFinalStep(from)) {
      double finalCost = RING.times(cost, from.getFinalWeight());
      Preconditions.checkState(from.getArcCount() == 0);
      ImmutableList<String> candidate = ImmutableList.copyOf(path);
      if (RING.isMember(finalCost) && !isDuplicatePath(candidate, outputs)) {
        outputs.add(new CandidatePath(candidate, finalCost));
      }
      return; // nothing more to recurse
    }
    for (Arc arc : from.getArcs()) {
      String ilabel = inputLabels.keyForId(arc.getIlabel());
      boolean push = !skipLabels.contains(ilabel);
      if (push) {
        path.addLast(ilabel);
      }
      decodeStep(arc.getNextState(), path, RING.times(cost, arc.getWeight()));
      if (push) {
        path.removeLast();
      }
    }
  }

  private boolean isDuplicatePath(ImmutableList<String> candidate, List<CandidatePath> outputs) {
    for (CandidatePath output : outputs) {
      if (output.getPathStates().equals(candidate)) {
        return true;
      }
    }
    return false;
  }

  private boolean isFinalStep(State from) {
    if (Double.isNaN(from.getFinalWeight()) ||
        RING.zero() == from.getFinalWeight() ||
        Double.isInfinite(from.getFinalWeight())) {
      return false;
    }
    Preconditions.checkArgument(Doubles.isFinite(from.getFinalWeight()));
    return true;
  }
}
