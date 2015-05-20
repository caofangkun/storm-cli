set topology.workers=3;
set topology.name="ExclamationTopology"
set storm.jar=./jstorm-example-0.9.0.jar;
add jar jstorm-example-0.9.0.jar;
-- SPOUT(parami_hint, spout_classname, spout_class_args)
REGISTER word=SPOUT(10, "storm.starter.spout.TestWordSpout", 20, 30);
-- BOLT(parami_hint, bolt_classname, bolt_class_args).[GROUPING TYPE](head_node_alias_name)
REGISTER exclaim1=BOLT(3, "storm.starter.ExclamationBolt").SHUFFLE("word");
REGISTER exclaim2=BOLT(2, "storm.starter.ExclamationBolt").SHUFFLE("exclaim1");
submit;
