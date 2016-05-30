jg2p
====

Java implementation of a general grapheme to phoneme toolkit using a pipeline of CRFs, a MaxEnt classifier, and a 
joint "graphone" language model.

This is just coming out of research mode so some of the edges are a little rough. Please excuse the dust.
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
    <version>1.1.2</version>
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
    <version>2.0.10</version>
</dependency>
```

I do all of the training on gcloud using 32 core, 15GB memory boxes. You can look at 
[this script](https://github.com/steveash/devops/blob/master/gcloud_jg2p.sh) to see how 
I bootstrap the environment, but it doesn't really need anything special except:

## Pre-requisites
* Java 7
* Maven 3+

# Getting Started

If you want to build and evaluate a G2P model from the CMUDict source, run this from the src/test/groovy folder:

```
./mrun PipelineTrain.groovy
```

You'll want to take a look at the driver groovy script to see what it's doing and where it's reading/writing files to.
 If you don't want to wait for the model to train you can use the saved model that was trained on the _training_ 
 CMUDict data set. You can run this to do an evaluation against the _test_ CMUDict data set and get overall Word 
 Error Rate (WER) and Phoneme Error Rate (PER) as shown in the paper.
 
```
./mrun PipelineEval.groovy
```