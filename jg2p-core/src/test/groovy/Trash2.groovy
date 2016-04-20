import com.github.steveash.jg2p.PipelineEncoder
import com.github.steveash.jg2p.PipelineModel
import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.abb.PatternFacade
import com.github.steveash.jg2p.align.AlignerTrainer
import com.github.steveash.jg2p.align.Alignment
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.train.PipelineTrainer
import com.github.steveash.jg2p.util.ModelReadWrite
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.collect.HashBasedTable
import com.google.common.collect.HashMultimap
import com.google.common.collect.HashMultiset
import com.google.common.collect.Lists
import com.google.common.collect.Multisets

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
opts.maxXGram = 4
opts.maxYGram = 3
opts.onlyOneGrams = false
opts.useCityBlockPenalty = true
opts.useWindowWalker = true
opts.useSyllableTagger = true

def att = new AlignerTrainer(opts)
def tra = att.train(inputs)

inputs = inputs.findAll{ PipelineTrainer.keepTrainable.apply(it)}
inputs = inputs.collect {PipelineTrainer.trainingXforms.apply(it)}
def shapeCounts = HashMultiset.create()
def yexs = HashMultimap.create()
def ycounts = HashMultiset.create()
def ys = HashMultiset.create()
for (InputRecord input : inputs) {
  List<Alignment> best = tra.align(input.xWord, input.yWord, 1);
  for (Alignment pairs : best) {
    pairs.graphonesSplit.each {
      def shape = it.left.size() + "x" + it.right.size()
      shapeCounts.add(shape)
      ycounts.add(it.right)
      if (yexs.get(it.right).size() < 4) {
        yexs.put(it.right, pairs)
      }
      if (it.right.size() > 1) {
        ys.add(it.right)
      }
      if (it.left.size() >= 5 || it.right.size() >= 3) {
        println "Example of $shape " + pairs
      }
    }
  }
}

println "---- shape counts ----"
Multisets.copyHighestCountFirst(shapeCounts).entrySet().each {
  println it.element + " = " + it.count
}
println "---- target counts ----"
Multisets.copyHighestCountFirst(ycounts).entrySet().each {
  println it.element.join(' ') + " = " + it.count + " times:"
  yexs.get(it.element).each {
    println "   " + it
  }
}
println "---- Y > 1 distinct " + ys.entrySet().size()
Multisets.copyHighestCountFirst(ys).entrySet().each {
  println it.element.join(' ') + " = " + it.count
}

//def phones = Word.fromSpaceSeparated("AH G R AE W AH L")
//def aligns = tra.align(grams, phones, 5)
//def aligns = tea.inferAlignments(grams, 5)
//def aligns = pe.encode(grams)
println "done"