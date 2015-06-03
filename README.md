Master Branch: [![Travis CI](https://travis-ci.org/caofangkun/storm-cli.svg?branch=master)](https://travis-ci.org/caofangkun/storm-cli)

# storm-cli
cli for Apache Storm



  word count example
```
set topology.name=test_topology;
set storm.jar=./jstorm-example-0.9.0.jar;
add jar storm-example-0.9.0.jar;
set topology.workers=3;
REGISTER spout=SPOUT(2, "storm.starter.spout.RandomSentenceSpout");
REGISTER split=BOLT(2, "storm.starter.WordCountTopology$SplitSentence").SHUFFLE("spout");
REGISTER count=BOLT(1, "storm.starter.WordCountTopology$WordCount").FIELDS("split", "word");
submit;
```
