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

package com.github.steveash.jg2p.eval

import com.github.steveash.jg2p.DuplicateStrippingEncoder
import com.github.steveash.jg2p.Encoder
import com.github.steveash.jg2p.PhoneticEncoder
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.util.Percent
import com.google.common.base.Preconditions
import com.google.common.collect.Lists
import com.google.common.util.concurrent.RateLimiter
import groovyx.gpars.GParsPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Evaluates a bulk input (and optionally deals with classifying mulitple pronunciations
 * @author Steve Ash
 */
class BulkEval {

  private static final Logger log = LoggerFactory.getLogger(BulkEval.class);

  private final int considerTopK = 4;
  private final Encoder encoder;
  private final RateLimiter limiter = RateLimiter.create(1.0 / 2.0)

  public BulkEval(Encoder encoder) {
    this.encoder = DuplicateStrippingEncoder.decorateIfNotAlready(encoder)
  }

  public EvalStats groupAndEval(Iterable<InputRecord> ungrouped) {
    // group the inputs into Candidate instances that have all acceptable answers
    def groupedInput = ungrouped.groupBy { it.left }
    def inputGroups = Lists.newArrayListWithCapacity(groupedInput.size())
    groupedInput.values().each { grp ->
      inputGroups << new InputRecordGroup(grp[0].left, grp.collect { it.right }.toSet())
    }

    return eval(inputGroups)
  }

  public EvalStats eval(Collection<InputRecordGroup> groups) {
    log.info("Evaluating {} input word groups", groups.size())
    def stats = new EvalStats()
    def totalGroups = groups.size()
    GParsPool.withPool {
      groups.everyParallel { InputRecordGroup group ->
        stats.wordOptionsHisto.add(group.acceptableYWords.size())
        def results = encoder.encode(group.getTestWord())
        Preconditions.checkNotNull(results, "should always get results")
        stats.resultsSizeHisto.add(results.size())
        int currentTotal;
        if (results.isEmpty()) {
          currentTotal = stats.onNewResult(group, null)
        } else {
          currentTotal = stats.onNewResult(group, results[0])
          updateTopK(results, group, stats)
          updateIrs(results, group, stats)
        }

        if (limiter.tryAcquire()) {
          log.info("Completed " + currentTotal + " of " + totalGroups + "  " + Percent.print(currentTotal, totalGroups))
        }
        return true
      }
    }
    log.info("Finished evaluating all " + totalGroups)
    return stats
  }

  private def updateIrs(List<PhoneticEncoder.Encoding> encodings, InputRecordGroup group, EvalStats stats) {
    // updates the IR stats for different configurations
    def good = group.acceptableYWords.size()
    def ranks = calcRanks(encodings, group)
    for (int i = 0; i < this.considerTopK; i++) {
      int goodInResults = countRanks(ranks, Revelant.Good, 0, i)
      int badInResults = countRanks(ranks, Revelant.Bad, 0, i)
      int totalResults = goodInResults + badInResults;
      int possible = Math.min(good, (i + 1));

      if (goodInResults > possible) {
        throw new IllegalStateException("Got $possible possible but counted $goodInResults good from $ranks " +
                                        "based on good words ${group.acceptableYWords} from encodings $encodings")
      }

      stats.irConfigSetup.get("IR_ALL_TOP" + (i+1)).onNewQuery(goodInResults, totalResults, good, possible)
      if (good >= 2) {
        stats.irConfigSetup.get("IR_MULTI_TOP" + (i+1)).onNewQuery(goodInResults, totalResults, good, possible)
      }
    }
  }

  private int countRanks(Revelant[] ranks, Revelant statusToCount, int fromIncl, int toIncl) {
    int count = 0;
    for (int i = fromIncl; i <= toIncl && i < ranks.length; i++) {
      if (ranks[i] == statusToCount) {
        count += 1;
      }
    }
    return count;
  }

  private calcRanks(List<PhoneticEncoder.Encoding> encodings, InputRecordGroup group) {
    Revelant[] ranks = new Revelant[this.considerTopK];
    for (int i = 0; i < this.considerTopK; i++) {
      if (i >= encodings.size()) {
        ranks[i] = Revelant.Missing
        continue
      }
      if (group.isMatching(encodings.get(i).phones)) {
        ranks[i] = Revelant.Good
      } else {
        ranks[i] = Revelant.Bad
      }
    }
    return ranks;
  }

  private def updateTopK(List<PhoneticEncoder.Encoding> results, InputRecordGroup group, EvalStats stats) {
    // try to find what rank is the matching
    for (int i = 0; i < results.size(); i++) {
      if (group.isMatching(results[i].phones)) {
        stats.counters.add(String.format("RIGHT_TOP_%02d", i))
        return
      }
    }
    stats.counters.add("RIGHT_TOP_NONE")
  }
}
