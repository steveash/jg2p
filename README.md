jg2p
====

Java implementation of a general grapheme to phoneme toolkit using a pipeline of CRFs, a log-loss re-ranker, and a 
joint "graphone" language model. 

Out of the box you get:
* Phonetic Encoder for English words (trained using CMUDict 0.7b)
* Syllabifier to split English words into syllables (trained using CMUDict 0.7b)
* Syllabifier to split phoneme strings into syllables (trained using CMUDict 0.7b)
* CMUDict 0.7b with syllable boundaries for the words and the phonemes (tagged using the above syllabifiers; see jg2p-core/src/test/resources/cmu7b.test|train)
* Toolkit for building your own CRF Pipeline based transducers
* Port of the Phonetasaurus WFST implementation of G2P transduction in Java (see SeqTransducer)

If you just want to use these without worrying about building your own models, see the Quick Start
section below. If you want to train your own models, then see the Training Your Own Models section

## Quick Start

Pre-requisites
* Java 7
* Maven 3+
* [git-lfs](https://git-lfs.github.com/) installed - there are a number of large files and GitHub doesn't allow a lot of large files so a separate plugin, git-lfs, solves this. Install this before cloning the repo or if you forget and install this after the fact, then run `git lfs pull` to update your local workspace with real (large) files.

### Phonetic Encoder

To use the phonetic encoder out of the box, you need to add the maven dependency:
```xml
<dependency>
    <groupId>com.github.steveash.jg2p</groupId>
    <artifactId>jg2p-pipe-cmu</artifactId>
    <version>1.1.0</version>
</dependency>
```

This will include the jg2p core, its dependencies, and the pre-trained phonetic encoder model
on the full CMU 0.7b dataset (which is ~35MB compressed).  Here is a code example showing usage:

```java
import com.github.steveash.jg2p.model.CmuEncoderFactory;
import com.github.steveash.jg2p.SimpleEncoder;

// ...

SimpleEncoder encoder = CmuEncoderFactory.createSimple();

// returns a space separated arpabet encoding of the given word
String result = encoder.encodeBestAsSpaceString("stephen");
// result is S T IY V AH N

// returns a list of the top-3 best encodings of the word
List<String> pumps = encoder.encodeAsSpaceString("pumpernickel", 3);
// returns list of [P AH M P ER N IH K AH L, P AH M ER N IH K AH L, ...]
```

The `SimpleEncoder` is thread-safe. Also, it's fairly large in memory and slow to load so you only 
want to call `createSimple()` once and hold on to the encoder result as a singleton somewhere in 
your application.

There is also a more complex interface `Encoder` that gives you a richer result data structure 
containing all of the scores for various pipes and alignments. You can instantiate this version
by `CmuEncoderFactory.create()`.

### Syllabifier

To use the syllabifier out of the box, you need to add the maven dependency:
```xml
<dependency>
    <groupId>com.github.steveash.jg2p</groupId>
    <artifactId>jg2p-syllg-cmu</artifactId>
    <version>1.1.0</version>
</dependency>
```

This will include the jg2p core, its dependencies, and the pre-trained syllabifier model 
on the full CMU 0.7b dataset (which is ~7MB compressed).  Here is a code example showing usage:

```java
import com.github.steveash.jg2p.model.CmuSyllabifierFactory;
import com.github.steveash.jg2p.syllchain.Syllabifier

// ...

Syllabifier syllabifier = CmuSyllabifierFactory.create();

// splits the word into a list of the word's syllables
List<String> karoneous = syllabifier.splitIntoSyllables("karoneous");
// result is [kar, o, ne, ous]

// returns the number of syllables in the given word
int count = syllabifier.syllableCount("stephen");
// count is 2
```

The syllabifier is thread-safe, and it should be used as a singleton in your program. Don't call 
`create()` over and over everytime you want to syllabify something.

## Training your own models
The pipeline is described in two academic papers that are in press right now. I will update this 
with links after they are published. In the meantime, you can build your own models by running
the `PipelineTrain.groovy` script in `jg2p-core/src/test/groovy`. This script shows the 
options that I used to build the final models. It also includes the option to specify the 
test dataset and do a validation immediately after training.  

There are many options, but the most important are the max G and max P substrings to use 
in the m-n aligner. For my models I used max=4 graphemes and max=3 phonemes.

I do all of the training on gcloud using 32 core, 15GB memory boxes. You can look at 
[this script](https://github.com/steveash/devops/blob/master/gcloud_jg2p.sh) to see how 
I bootstrap the environment, but it doesn't really need anything special.  Once bootstrapped
then you can use the `mrun` bash script in `jg2p-core/src/test/groovy` to actually
run the script (letting maven setup the classpath for you):

```
./mrun PipelineTrain.groovy
```

### Other implementation notes

Unfortunately, for the moment this requires forks of two other libraries: kylm and Mallet, because I made changes to 
both that haven't made their way back to pull requests (yet). But both are available on Maven Central so it will
probably be unnoticeable to you.

My fork of KYLM rewrites all of the runtime data structures that you would need to test a trained language model.
The original version was unable to be run by multiple threads at the same time. My fork allows you to train on 
mutable, non-thread safe version of the models, and then convert them to serializable, immutable, thread safe
versions.
```xml
<dependency>
    <groupId>com.github.steveash.kylm</groupId>
    <artifactId>kylm</artifactId>
    <version>1.1.4</version>
</dependency>
```

In my fork of Mallet I added two features: (1) the ability to use trained CRF models with sparse vector representations
(by default it uses a kinda-sparse implementation that just doesn't scale with a lot of parameters and blows up your
memory requirements very quickly; (2) the ability to _bootstrap_ CRF training by initializing the parameter values 
from another already trained CRF (by matching state + feature function value). This short cuts CRF training time 
substantially by shaving off hundreds of LBFGS iterations.
```xml
<dependency>
    <groupId>com.github.steveash.mallet</groupId>
    <artifactId>mallet</artifactId>
    <version>2.0.11</version>
</dependency>
```

Both of these are transitive dependencies of jg2p-core so you don't have to worry about including 
them explicitly.
