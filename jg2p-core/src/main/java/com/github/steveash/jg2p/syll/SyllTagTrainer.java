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

package com.github.steveash.jg2p.syll;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Sets;

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.phoseq.Graphemes;
import com.github.steveash.jg2p.seq.LeadingTrailingFeature;
import com.github.steveash.jg2p.seq.NeighborShapeFeature;
import com.github.steveash.jg2p.seq.NeighborTokenFeature;
import com.github.steveash.jg2p.seq.StringListToTokenSequence;
import com.github.steveash.jg2p.seq.SurroundingTokenFeature;
import com.github.steveash.jg2p.seq.TokenSequenceToFeature;
import com.github.steveash.jg2p.seq.TokenWindow;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.TokenAccuracyEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2LabelSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Sequence;

/**
 * Trains a CRF to predict synonym structure from aligned.  We have two kinds of structure we are modelling here -- the
 * sonority of the syllable as S = {Onset, Nucleus, Coda} and the boundaries for where graphemes should be split to map
 * into phoneme substrings as T = {B, X} (b is begin, x is continuation -- like the BIO scheme) Note that the Y side is
 * not necessarily individual phonemes -- but the phoneme substrings that the aligner identified.
 *
 * Thus we are learning the labels S x T and we are going to disallow illegal transitions to enforce the semantics of
 * each
 *
 * @author Steve Ash
 */
public class SyllTagTrainer {

  private static final Logger log = LoggerFactory.getLogger(SyllTagTrainer.class);

  // syllable only roles
  public static final String Onset = "O";
  public static final char OnsetChar = Onset.charAt(0);
  public static final String Nucleus = "N";
  public static final char NucleusChar = Nucleus.charAt(0);
  public static final String Coda = "C";
  public static final char CodaChar = Coda.charAt(0);

  // alignment structure
  public static final String AlignBegin = "B";
  public static final String AlignCont = "X";
  private static final char AlignBeginChar = AlignBegin.charAt(0);
  private static final char AlignContChar = AlignCont.charAt(0);

  // syll chain structure
  public static final String SyllCont = "Y";
  public static final String SyllEnd = "Z";
  public static final char SyllContChar = SyllCont.charAt(0);
  public static final char SyllEndChar = SyllEnd.charAt(0);

  // these mark the _start_ of a phoneme alignemnt
  public static final String OnsetStart = Onset + AlignBegin;
  public static final String NucleusStart = Nucleus + AlignBegin;
  public static final String CodaStart = Coda + AlignBegin;

  // these mark the _continuation of the phoneme alignment (i.e. until the next _start_)
  public static final String OnsetCont = Onset + AlignCont;
  public static final String NucleusCont = Nucleus + AlignCont;
  public static final String CodaCont = Coda + AlignCont;

  public static String onlySyllStructreFromTag(String tag) {
    Preconditions.checkArgument(tag.length() == 2, "didn't pass a tag", tag);
    char c = tag.charAt(0);
    if (c == OnsetChar) {
      return Onset;
    }
    if (c == NucleusChar) {
      return Nucleus;
    }
    if (c == CodaChar) {
      return Coda;
    }
    throw new IllegalStateException("unknown code symbol " + tag);
  }

  public static String onlyAlignStructureFromTag(String tag) {
    Preconditions.checkArgument(tag.length() == 2, "didn't pass a tag", tag);
    char c = tag.charAt(1);
    if (c == AlignBeginChar) {
      return AlignBegin;
    }
    if (c == AlignContChar) {
      return AlignCont;
    }
    throw new IllegalStateException("unknown code symbol " + tag);
  }

  public static boolean isAlignBegin(String tag) {
    if (tag.length() == 2) {
      char c = tag.charAt(1);
      if (c == AlignBeginChar) {
        return true;
      }
      if (c == AlignContChar) {
        return false;
      }
    }
    throw new IllegalStateException("unknown code symbol " + tag);
  }

  // takes a training time alignment and produces the joint (S x T) labels; cardinality will be |X|
  public static List<String> makeSyllMarksFor(Alignment align) {
    Preconditions.checkArgument(align.getSyllWord() != null, "must pass alignment with syll word");
    SWord phoneSyll = align.getSyllWord();
    List<Boolean> starts = align.getXStartMarks();
    List<String> sylls = Lists.newArrayListWithCapacity(align.getWordUnigrams().size());
    int syllState = 0; // 0 onset, 1 nucleus, 2 coda
    int x = 0;
    int y = 0;

    for (Pair<List<String>, List<String>> graphone : align.getGraphonesSplit()) {
      // deal with phonemes, if leading is syllable boundary then great, also assert no breaks

      for (int i = 0; i < graphone.getRight().size(); i++) {
        if (graphone.getRight().get(i).equals(Grams.EPSILON)) {
          continue;
        }

        if (i == 0) {
          if (phoneSyll.isStartOfSyllable(y)) {
            syllState = 0; // back to onset
          }
        } else {
          if (phoneSyll.isStartOfSyllable(y)) {
//            if (log.isDebugEnabled()) {
//              log.debug("Malformed alignment, cant start syllable in middle " + phoneSyll +
//                        " with graphones " + align.toString() + " x = " + x + ", y = " + y);
//            }
            syllState = 0;
          }
        }
        y += 1;
      }
      // we've updated our syll state so just emit symbols now
      for (int i = 0; i < graphone.getLeft().size(); i++) {
        String graph = graphone.getLeft().get(i);
        // skip epsilons
        if (graph.equals(Grams.EPSILON)) {
          continue;
        }

        boolean isVowel = Graphemes.isVowel(graph);
        if (syllState == 0) {
          if (isVowel) {
            syllState = 1; // this is the nucleus
          }
        } else if (syllState == 1) {
          if (!isVowel) {
            syllState = 2; // this is the coda
          }
        } else {
          Preconditions.checkState(syllState == 2, "should always be coda");
//          if (isVowel && !knownIssue(graph, align.getWordUnigrams(), x)) {
//            if (log.isDebugEnabled()) {
//              log.debug("Malformed syllable, vowels in coda " + phoneSyll +
//                        " with graphones " + align.toString() + " x = " + x + ", y = " + y);
//            }
//          }
        }
        String coded = getCodeFor(syllState, starts.get(x));
        sylls.add(coded);
        x += 1;
      }
    }
    Preconditions
        .checkState(x == align.getWordUnigrams().size(), "ended up with diff graphone graph count ", align, phoneSyll);
    Preconditions.checkState(y == phoneSyll.unigramCount(), "ended up with different phone count ", align, phoneSyll);
    return sylls;
  }

  public static List<String> makeOncForGraphemes(Alignment align) {
    List<String> graphMarks = makeSyllMarksFor(align);
    List<String> oncMarks = Lists.newArrayListWithCapacity(graphMarks.size());
    for (String graphMark : graphMarks) {
      oncMarks.add(SyllTagTrainer.onlySyllStructreFromTag(graphMark));
    }
    return oncMarks;
  }

  // gets the ONC coding as graphone grams from a training time alignment, output cardinality |XY graphones|
  public static List<String> makeSyllGramsFromMarks(Alignment align) {
    return makeSyllGramsFromMarks(makeSyllMarksFor(align));
  }

  // gets the ONC coding as graphone grams from a joint (T x S) labelling (that would be produced by
  // the joint syll test time tagger (not the syll chain one). Input cardinality |X|, output |XY graphones|
  public static List<String> makeSyllGramsFromMarks(List<String> marks) {
    List<String> outGrams = Lists.newArrayList();
    StringBuilder sb = new StringBuilder();
    for (String mark : marks) {
      if (isAlignBegin(mark)) {
        if (sb.length() > 0) {
          outGrams.add(sb.toString().trim());
          sb.delete(0, sb.length());
        }
      }
      sb.append(onlySyllStructreFromTag(mark)).append(' ');
    }
    if (sb.length() > 0) {
      outGrams.add(sb.toString().trim());
    }
    return outGrams;
  }

  public static List<String> makeSyllablesFor(Alignment align) {
    StringBuilder sb = new StringBuilder();
    List<String> outs = Lists.newArrayList();
    SyllCounter counter = new SyllCounter();
    List<Pair<String, String>> graphones = align.getGraphones();
    List<String> syllGrams = align.getGraphoneSyllableGrams();
    Preconditions.checkNotNull(syllGrams);
    Preconditions.checkState(syllGrams.size() == graphones.size());
    int syllIndex = 0;
    for (int i = 0; i < graphones.size(); i++) {
      counter.onNextGram(syllGrams.get(i));
      if (syllIndex != counter.currentSyllable()) {
        // we just crossed a syllable boundary
        if (sb.length() == 0) {
          throw new IllegalStateException("Not sure what happened with this " + align + " at " + i +
                                          " the counter says " + counter.currentSyllable() + " i have " + syllIndex +
                                          " after feeding " + syllGrams.get(i));
        }
        outs.add(sb.toString());
        syllIndex = counter.currentSyllable();
        sb.delete(0, sb.length());
      }
      String gram = graphones.get(i).getLeft();
      appendGramNoSpaces(sb, gram);
    }
    if (sb.length() > 0) {
      outs.add(sb.toString());
    }
    return outs;
  }

  // produces Y/Z tags on graphemes where Z indicates the grapemes that are the last in a
  // syllable
  public static List<String> makeSyllableGraphEndMarksFor(Alignment align) {
    List<String> endings = Lists.newArrayList();
    SyllCounter counter = new SyllCounter();
    List<String> syllGrams = align.getGraphoneSyllableGrams();
    List<String> codes = Grams.flattenGrams(syllGrams);
    PeekingIterator<String> iter = Iterators.peekingIterator(codes.iterator());
    Preconditions.checkState(codes.size() == align.getWordUnigrams().size());
    int syllIndex = 0;
    if (!iter.hasNext()) {
      return ImmutableList.of(); // empty
    }
    counter.onNextGram(iter.next());
    while (iter.hasNext()) {
      counter.onNextGram(iter.next());
      endings.add(syllIndex == counter.currentSyllable() ? SyllCont : SyllEnd);
      syllIndex = counter.currentSyllable();
    }
    endings.add(SyllEnd); // last one is always a boundary
    Preconditions.checkState(codes.size() == endings.size());
    return endings;
  }

  // takes a sequence of Y/Z tags indicating the _last grapheme_ in a syll and produces the set of grapheme
  // indexes that start syllables
  public static Set<Integer> startsFromSyllGraphMarks(Sequence<?> outSeq) {
    Set<Integer> starts = Sets.newHashSet();
    starts.add(0);
    for (int i = 1; i < outSeq.size(); i++) {
      if (outSeq.get(i - 1).toString().equalsIgnoreCase(SyllEnd)) {
        starts.add(i);
      }
    }
    return starts;
  }

  // takes a test time alignment and the set of grapheme syll start indexes ( from a syllchain eval) and
  // produce the joint (T x S) labels in graphone grams (to send to the pronouncer)
  public static List<String> makeSyllMarksFor(Alignment align, Set<Integer> graphSyllStarts) {
    List<Boolean> starts = align.getXStartMarks();
    List<String> sylls = Lists.newArrayListWithCapacity(align.getWordUnigrams().size());
    int syllState = 0; // 0 onset, 1 nucleus, 2 coda
    int x = 0;

    for (Pair<List<String>, List<String>> graphone : align.getGraphonesSplit()) {
      // deal with phonemes, if leading is syllable boundary then great, also assert no breaks

      StringBuilder sb = new StringBuilder();
      // we've updated our syll state so just emit symbols now
      for (int i = 0; i < graphone.getLeft().size(); i++) {
        String graph = graphone.getLeft().get(i);
        // skip epsilons
        if (graph.equals(Grams.EPSILON)) {
          continue;
        }

        if (graphSyllStarts.contains(x)) {
          // reset the syll state to onset
          syllState = 0;
        }

        boolean isVowel = Graphemes.isVowel(graph);
        if (syllState == 0) {
          if (isVowel) {
            syllState = 1; // this is the nucleus
          }
        } else if (syllState == 1) {
          if (!isVowel) {
            syllState = 2; // this is the coda
          }
        } else {
          Preconditions.checkState(syllState == 2, "should always be coda");
          //          if (isVowel && !knownIssue(graph, align.getWordUnigrams(), x)) {
          //            if (log.isDebugEnabled()) {
          //              log.debug("Malformed syllable, vowels in coda " + phoneSyll +
          //                        " with graphones " + align.toString() + " x = " + x + ", y = " + y);
          //            }
          //          }
        }
        String coded = getSyllCodeFor(syllState);
        sb.append(coded).append(" ");
        x += 1;
      }
      sylls.add(sb.toString().trim());
      sb.delete(0, sb.length());
    }
    Preconditions
        .checkState(x == align.getWordUnigrams().size(), "ended up with diff graphone graph count ", align);
    Preconditions.checkState(sylls.size() == align.getGraphones().size());
    return sylls;
  }

  private static StringBuilder appendGramNoSpaces(StringBuilder sb, String gram) {
    for (String letter : Grams.iterateSymbols(gram)) {
      sb.append(letter);
    }
    return sb;
  }

  private static String getCodeFor(int state, boolean isStart) {
    if (state == 0) {
      if (isStart) {
        return OnsetStart;
      } else {
        return OnsetCont;
      }
    } else if (state == 1) {
      if (isStart) {
        return NucleusStart;
      } else {
        return NucleusCont;
      }
    } else {
      Preconditions.checkState(state == 2, "invalid state");
      if (isStart) {
        return CodaStart;
      } else {
        return CodaCont;
      }
    }
  }

  private static String getSyllCodeFor(int state) {
    if (state == 0) {
      return Onset;
    } else if (state == 1) {
      return Nucleus;
    } else {
      Preconditions.checkState(state == 2, "invalid state");
      return Coda;
    }
  }

  private CRF initFrom = null;

  public void setInitFrom(SyllTagModel initFrom) {
    this.initFrom = initFrom.getCrf();
  }

  public SyllTagModel train(Collection<Alignment> trainInputs) {
    return train(trainInputs, null, false);
  }

  public SyllTagModel train(Collection<Alignment> trainInputs, Collection<Alignment> testInputs, boolean eval) {
    Pipe pipe = makePipe();
    InstanceList trainExamples = makeExamplesFromAlignsWithPipe(trainInputs, pipe);
    InstanceList testExamples = null;
    if (testInputs != null) {
      testExamples = makeExamplesFromAlignsWithPipe(testInputs, pipe);
    }

    log.info("Training test-time syll aligner on whole data...");
    TransducerTrainer trainer = trainOnce(pipe, trainExamples);

    if (eval) {
      TokenAccuracyEvaluator evaler = new TokenAccuracyEvaluator(trainExamples, "traindata");
      evaler.evaluate(trainer);
      double trainAcc = evaler.getAccuracy("traindata");
      double testAcc = 0.0;
      if (testExamples != null) {
        TokenAccuracyEvaluator evaler2 = new TokenAccuracyEvaluator(testExamples, "testdata");
        evaler2.evaluate(trainer);
        testAcc = evaler2.getAccuracy("testdata");
      }
      log.info("Train data accuracy = " + trainAcc + ", test data accuracy = " + testAcc);
    }

    return new SyllTagModel((CRF) trainer.getTransducer());
  }

  private TransducerTrainer trainOnce(Pipe pipe, InstanceList trainData) {
    Stopwatch watch = Stopwatch.createStarted();

    CRF crf = new CRF(pipe, null);
    crf.addOrderNStates(trainData, new int[]{1}, null, null, null, null, false);
    crf.addStartState();
    crf.setWeightsDimensionAsIn(trainData, false);
    if (initFrom != null) {
      crf.initializeApplicableParametersFrom(initFrom);
    }

    log.info("Starting alignTag training...");
    CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(crf, 8);
    trainer.setGaussianPriorVariance(2);
    trainer.setAddNoFactors(true);
    trainer.setUseSomeUnsupportedTrick(false);
    trainer.train(trainData);
    trainer.shutdown();
    watch.stop();

    log.info("Syll align Tag CRF Training took " + watch.toString());
    crf.getInputAlphabet().stopGrowth();
    crf.getOutputAlphabet().stopGrowth();
    return trainer;
  }

  private InstanceList makeExamplesFromAligns(Iterable<Alignment> alignsToTrain) {
    Pipe pipe = makePipe();
    return makeExamplesFromAlignsWithPipe(alignsToTrain, pipe);
  }

  private InstanceList makeExamplesFromAlignsWithPipe(Iterable<Alignment> alignsToTrain, Pipe pipe) {
    int count = 0;
    InstanceList instances = new InstanceList(pipe);
    for (Alignment align : alignsToTrain) {

      Word orig = align.getInputWord();
      Word marks = Word.fromGrams(makeSyllMarksFor(align));
      Preconditions.checkState(orig.unigramCount() == marks.unigramCount());

      Instance ii = new Instance(orig.getValue(), marks.getValue(), align.getInputWord().getAsNoSpaceString(), null);
      instances.addThruPipe(ii);
      count += 1;

    }
    log.info("Read {} instances of training data for align syll tag", count);
    return instances;
  }

  private Pipe makePipe() {
    Alphabet alpha = new Alphabet();
    Target2LabelSequence labelPipe = new Target2LabelSequence();
    LabelAlphabet labelAlpha = (LabelAlphabet) labelPipe.getTargetAlphabet();

    return new SerialPipes(ImmutableList.of(
        new StringListToTokenSequence(alpha, labelAlpha),   // convert to token sequence
        new TokenSequenceLowercase(),                       // make all lowercase
        new NeighborTokenFeature(true, makeNeighbors()),         // grab neighboring graphemes
        new SurroundingTokenFeature(false),
//        new SurroundingTokenFeature(true),
        new NeighborShapeFeature(true, makeShapeNeighs()),
        new LeadingTrailingFeature(),
        new TokenSequenceToFeature(),                       // convert the strings in the text to features
        new TokenSequence2FeatureVectorSequence(alpha, true, true),
        labelPipe
    ));
  }

  private static List<TokenWindow> makeShapeNeighs() {
    return ImmutableList.of(
        //        new TokenWindow(-5, 5),
        new TokenWindow(-4, 4),
        new TokenWindow(-3, 3),
        new TokenWindow(-2, 2),
        new TokenWindow(-1, 1),
        new TokenWindow(1, 1),
        new TokenWindow(1, 2),
        new TokenWindow(1, 3),
        new TokenWindow(1, 4)
        //        new TokenWindow(1, 5)
    );
  }

  private List<TokenWindow> makeNeighbors() {
    return ImmutableList.of(
        new TokenWindow(1, 1),
//        new TokenWindow(1, 2),
        new TokenWindow(2, 1),
        new TokenWindow(3, 1),
        new TokenWindow(4, 1),
        new TokenWindow(-1, 1),
        new TokenWindow(-2, 1),
        new TokenWindow(-3, 1),
        new TokenWindow(-4, 1)
    );
  }
}
