import com.github.steveash.jg2p.abb.Abbrev
import com.github.steveash.jg2p.abb.KnownPattern
import com.github.steveash.jg2p.align.Alignment
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.phoseq.Graphemes
import com.github.steveash.jg2p.util.ModelReadWrite
import com.google.common.collect.HashMultiset
import com.google.common.collect.Lists

def file = "../resources/pipe_42sy_F10_7.dat"
def tra = ModelReadWrite.readTrainAlignerFrom(file)
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
List<Alignment> examples = Lists.newArrayListWithCapacity(inputs.size());
for(InputRecord input : inputs) {
//def input = inputs.find {it.xWord.asNoSpaceString == "AAA"}
  List<Alignment> best = tra.align(input.xWord, input.yWord, 1);
  if (best.isEmpty()) {
    println "Got nothing for " + input
  }
}
//  for (Alignment pairs : best) {
//    examples.add(pairs);
//    println pairs

//  }
//}
//def phones = Word.fromSpaceSeparated("AH G R AE W AH L")
//def aligns = tra.align(grams, phones, 5)
//def aligns = tea.inferAlignments(grams, 5)
//def aligns = pe.encode(grams)
println "done"