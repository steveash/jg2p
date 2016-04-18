import com.github.steveash.jg2p.abb.Abbrev
import com.github.steveash.jg2p.abb.KnownPattern
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.phoseq.Graphemes
import com.google.common.collect.HashMultiset

//def file = "../resources/pipe_42sy_F10_7.dat"
//def tra = ModelReadWrite.readTrainAlignerFrom(file)
//def tea = ModelReadWrite.readTestAlignerFrom(file)
//def model = ReadWrite.readFromFile(PipelineModel, new File(file))
//model.makeSparse()
//def pe = new PipelineEncoder(model)
//def grams = Word.fromNormalString("AUGE")
//def result = pe.encode(grams)
//result.take(10).each {println it}

def inputFile = "g014b2b.train.syll"
def inputs = InputReader.makePSaurusReader().readFromClasspath(inputFile)
opts = new TrainOptions()
opts.maxXGram = 6
opts.maxYGram = 2
opts.onlyOneGrams = false
opts.useCityBlockPenalty = true
opts.useWindowWalker = true
opts.useSyllableTagger = true

//att = new AlignerTrainer(opts)
//model = att.train(inputs)
//List<Alignment> examples = Lists.newArrayListWithCapacity(inputs.size());
def counts = HashMultiset.create()
for (InputRecord input : inputs) {
  def xw = input.xWord
  if (xw.unigramCount() <= 3 && xw.value.every { !Graphemes.isVowel(it) }) {
    counts.add("ROUGH_MATCH")
    if (Abbrev.isAcronym(xw)) {
      if (Abbrev.transcribeAcronym(xw).equals(input.yWord.asSpaceString)) {
        counts.add("ABBREV_GOODPHONES")
      } else {
        counts.add("ABBREV_BADPHONES")
      }
    } else if (KnownPattern.matches(xw)) {
      if (KnownPattern.transcribePattern(xw).equals(input.yWord.asSpaceString)) {
        counts.add("KP_GOODPHONES")
      } else {
        counts.add("KP_BADPHONES")
      }
    } else {
      counts.add("ROUGH_NOT_COVERED")
      println "not covered: $input"
    }
  } else {
    if (Graphemes.isAllVowelsOrConsonants(xw)) {
      def mtc = Abbrev.transcribeAcronym(xw).equals(input.yWord.asSpaceString)
      if (mtc) {
        counts.add("NO_ABBREV_BUT_MATCHED_PHONES")
        println "matched phones: " + input
      }
      if (xw.unigramCount() >= 2 && xw.unigramCount() <= 3 && Graphemes.isAllVowels(xw)) {
        if (mtc) {
          counts.add("NEWRULE_GOOD")
        } else {
          counts.add("NEWRULE_BAD")
          println "NEWRULE BAD " + xw
        }
      }
    }
  }
}
counts.entrySet().sort {it.element}.each {println it}
//  List<Alignment> best = tra.align(input.xWord, input.yWord, 1);
//  for (Alignment pairs : best) {
//    examples.add(pairs);
//  }
//}
//def counts = HashMultiset.create()
//for (Alignment ex : examples) {
//  for(String gram : ex.allXTokensAsList) {
//    if (Grams.countInGram(gram) >= 3) {
//      counts.add(gram)
//    }
//  }
//}
//Multisets.copyHighestCountFirst(counts).entrySet().each {
//  println it
//}

//def phones = Word.fromSpaceSeparated("AH G R AE W AH L")
//def aligns = tra.align(grams, phones, 5)
//def aligns = tea.inferAlignments(grams, 5)
//def aligns = pe.encode(grams)
println "done"