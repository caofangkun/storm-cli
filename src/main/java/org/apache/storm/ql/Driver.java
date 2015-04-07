package org.apache.storm.ql;

import org.apache.storm.ql.parse.ASTNode;
import org.apache.storm.ql.parse.BaseSemanticAnalyzer;
import org.apache.storm.ql.parse.Context;
import org.apache.storm.ql.parse.ParseDriver;
import org.apache.storm.ql.parse.SemanticAnalyzerFactory;
import org.apache.storm.ql.processors.CommandProcessor;
import org.apache.storm.ql.processors.CommandProcessorResponse;
import org.apache.storm.ql.utils.ParseUtils;

public class Driver implements CommandProcessor {

  public void setTryCount(int tryCount) {
    // TODO Auto-generated method stub

  }

  public int close() {
    return 0;
    // TODO Auto-generated method stub

  }

  public CommandProcessorResponse run(String command, Context context)
      throws Exception {
    return run(command, false, context);
  }

  private CommandProcessorResponse run(String command, boolean alreadyCompiled,
      Context context) throws Exception {
    CommandProcessorResponse cpr =
        runInternal(command, alreadyCompiled, context);
    return cpr;
  }

  private CommandProcessorResponse runInternal(String command,
      boolean alreadyCompiled, Context context) throws Exception {
    int ret = compile(command, context);
    return new CommandProcessorResponse(ret);
  }

  public int compile(String command, Context context) throws Exception {
    ParseDriver pd = new ParseDriver();
    ASTNode tree = pd.parse(command, false);
    tree = ParseUtils.findRootNonNullToken(tree);
    BaseSemanticAnalyzer sem = SemanticAnalyzerFactory.get(null, tree);
    sem.analyze(tree, context);

    return 0;

  }

  @Override
  public void init() {
    // TODO Auto-generated method stub

  }

  public void destroy() {
    // TODO Auto-generated method stub

  }

  public static void main(String[] args) throws Exception {
    Driver d = new Driver();
    String cmd =
        "REGISTER spout=SPOUT(\"storm.starter.spout.RandomSentenceSpout\")";
    d.compile(cmd, new Context());
  }
}
