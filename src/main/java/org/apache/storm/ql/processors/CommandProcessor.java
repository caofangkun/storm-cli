package org.apache.storm.ql.processors;

import org.apache.storm.ql.CommandNeedRetryException;
import org.apache.storm.ql.parse.Context;

public interface CommandProcessor {
  public void init();

  public CommandProcessorResponse run(String command, Context context)
      throws CommandNeedRetryException, Exception;
}
