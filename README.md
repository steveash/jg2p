jg2p
====

Java implementation of a general grapheme to phoneme toolkit using a pipeline of CRFs, a MaxEnt classifier, and a 
joint "graphone" language model.

This is just coming out of research mode so some of the edges are a little rough. Please excuse the dust.
Unfortunately, for the moment this requires forks of two other libraries: kylm and Mallet, because I made changes to 
both that haven't made their way back to pull requests (yet). If you take a look at 
[this script](https://github.com/steveash/devops/blob/master/setup_jg2p.sh) that will show exactly what has to be 
done to a local machine to prepare it for running jg2p (because that's what I use on gcloud to prep).

## Pre-requisites
* Java 7
* Maven 3+
* My forks of KYLM and Mallet (as described above)

# Getting Started

If you want to build a G2P model from the CMUDict source, run this from the src/test/groovy folder:

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