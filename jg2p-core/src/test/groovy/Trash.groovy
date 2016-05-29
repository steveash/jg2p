import com.github.steveash.jg2p.align.Alignment
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.syll.SWord
import com.github.steveash.jg2p.syllchain.SyllChainTrainer
import com.github.steveash.jg2p.train.PipelineTrainer
import com.github.steveash.jg2p.util.ModelReadWrite
import com.github.steveash.jg2p.util.Percent

def inputFile = "cmu7b.train"
def testFile = "cmu7b.test"
def inputs = InputReader.makePSaurusReader().readFromClasspath(inputFile)
inputs = inputs.findAll { PipelineTrainer.keepTrainable.apply(it)}
//inputs = inputs.findAll{it.left.asNoSpaceString.equalsIgnoreCase("huguenots")}
def testInputs = InputReader.makePSaurusReader().readFromClasspath(testFile)
println "reading model..."
def aligner1 = ModelReadWrite.readTrainAlignerFrom("../resources/pipe_43sy_cmu7_orig_1.dat")
//def aligner2 = ModelReadWrite.readTrainAlignerFrom("../resources/syllchainAlignConstrained.dat")

int total = 0
int exactMatch = 0
int split1 = 0
int syllCountMatch = 0
//int split2 = 0
println "evaluating aligns"
inputs.each { rec ->
  def sword = rec.right as SWord
  def res1 = aligner1.align(rec.left, rec.right, 1)
//  def res2 = aligner2.align(rec.left, rec.right, 1)
  if (res1.empty) {
    return
  }
  res1 = res1.first()
  total += 1
  if (total % 10000 == 0) {
    println "just did $total"
  }
  boolean shouldPrint = false
  if (rec.left.asNoSpaceString.equalsIgnoreCase("huguenots")) {
    shouldPrint = true
  }
  def splits = isSplit(res1)
  if (!splits.empty) {
//    println "$split1 - splits $splits for ${rec.left.asNoSpaceString} = ${sword.spaceWordWithSylls} = $res1"
    split1 += 1
    shouldPrint = true
  }
  def graphStarts = SyllChainTrainer.splitGraphsByPhoneSylls(res1)
  if (sword.syllCount() == graphStarts.size()) {
    syllCountMatch += 1
  } else {
    shouldPrint = true
  }
  if (shouldPrint) {
    println "predicted ${graphStarts.sort()} - ${rec.left.splitBy(graphStarts)} = ${sword.spaceWordWithSylls} = $res1"
  }
}
println "Done $exactMatch of $total exact match " + Percent.print(exactMatch, total)
println "Unconstrained aligner splits $split1 " + Percent.print(split1, total)
println "Graph coding mismatches ${total - syllCountMatch} " + Percent.print(syllCountMatch, total)

def isSplit(Alignment ali) {
  int yy = 0
  def sword = ali.syllWord
  def splits = []
  ali.graphonesSplit.each { graphone ->
    def graphs = graphone.left
    def phones = graphone.right
    for (int i = 1; i < phones.size(); i++) {
      if (sword.isStartOfSyllable(yy + i)) {
        if (graphs == ["X"] && phones == ["K","S"]) {
          // skip this known case
        } else {
          splits << (yy + i)
        }
      }
    }
    yy += phones.size()
  }
  return splits
}