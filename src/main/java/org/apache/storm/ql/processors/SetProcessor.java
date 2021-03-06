package org.apache.storm.ql.processors;

import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.storm.ql.parse.Context;
import org.apache.storm.ql.parse.VariableSubstitution;
import org.apache.storm.ql.session.SessionState;

public class SetProcessor implements CommandProcessor {

  private static String prefix = "set: ";
  public static final String ENV_PREFIX = "env:";
  public static final String SYSTEM_PREFIX = "system:";
  public static final String HIVECONF_PREFIX = "hiveconf:";
  public static final String HIVEVAR_PREFIX = "hivevar:";
  public static final String SET_COLUMN_NAME = "set";

  public static boolean getBoolean(String value) {
    if (value.equals("on") || value.equals("true")) {
      return true;
    }
    if (value.equals("off") || value.equals("false")) {
      return false;
    }
    throw new IllegalArgumentException(prefix + "'" + value
        + "' is not a boolean");
  }

  private void dumpOptions(Properties p) {
    SessionState ss = SessionState.get();
    SortedMap<String, String> sortedMap = new TreeMap<String, String>();
    sortedMap.put("silent", (ss.getIsSilent() ? "on" : "off"));
    for (Object one : p.keySet()) {
      String oneProp = (String) one;
      String oneValue = p.getProperty(oneProp);
      sortedMap.put(oneProp, oneValue);
    }

    for (String s : ss.getStormVariables().keySet()) {
      sortedMap.put(SetProcessor.HIVEVAR_PREFIX + s, ss.getStormVariables()
          .get(s));
    }

    for (Map.Entry<String, String> entries : sortedMap.entrySet()) {
      ss.out.println(entries.getKey() + "=" + entries.getValue());
    }

    for (Map.Entry<String, String> entry : mapToSortedMap(System.getenv())
        .entrySet()) {
      ss.out.println(ENV_PREFIX + entry.getKey() + "=" + entry.getValue());
    }

    for (Map.Entry<String, String> entry : propertiesToSortedMap(
        System.getProperties()).entrySet()) {
      ss.out.println(SYSTEM_PREFIX + entry.getKey() + "=" + entry.getValue());
    }

  }

  private void dumpOption(String s) {
    SessionState ss = SessionState.get();

    if (ss.getConf().get(s) != null) {
      ss.out.println(s + "=" + ss.getConf().get(s));
    } else if (ss.getStormVariables().containsKey(s)) {
      ss.out.println(s + "=" + ss.getStormVariables().get(s));
    } else {
      ss.out.println(s + " is undefined");
    }
  }

  @Override
  public void init() {
  }

  private CommandProcessorResponse setVariable(String varname, String varvalue) {
    SessionState ss = SessionState.get();
    if (varvalue.contains("\n")) {
      ss.err.println("Warning: Value had a \\n character in it.");
    }
    if (varname.startsWith(SetProcessor.ENV_PREFIX)) {
      ss.err.println("env:* variables can not be set.");
      return new CommandProcessorResponse(1);
    } else if (varname.startsWith(SetProcessor.SYSTEM_PREFIX)) {
      String propName = varname.substring(SetProcessor.SYSTEM_PREFIX.length());
      System.getProperties().setProperty(propName,
          new VariableSubstitution().substitute(ss.getConf(), varvalue));
      return new CommandProcessorResponse(0);
    } else if (varname.startsWith(SetProcessor.HIVECONF_PREFIX)) {
      String propName =
          varname.substring(SetProcessor.HIVECONF_PREFIX.length());
      try {
        setConf(varname, propName, varvalue, false);
        return new CommandProcessorResponse(0);
      } catch (IllegalArgumentException e) {
        return new CommandProcessorResponse(1, e.getMessage(), "42000");
      }
    } else if (varname.startsWith(SetProcessor.HIVEVAR_PREFIX)) {
      String propName = varname.substring(SetProcessor.HIVEVAR_PREFIX.length());
      ss.getStormVariables().put(propName,
          new VariableSubstitution().substitute(ss.getConf(), varvalue));
      return new CommandProcessorResponse(0);
    } else {
      try {
        setConf(varname, varname, varvalue, true);
        return new CommandProcessorResponse(0);
      } catch (IllegalArgumentException e) {
        return new CommandProcessorResponse(1, e.getMessage(), "42000");
      }
    }
  }

  // returns non-null string for validation fail
  private void setConf(String varname, String key, String varvalue,
      boolean register) throws IllegalArgumentException {
    Map conf = SessionState.get().getConf();
    String value = new VariableSubstitution().substitute(conf, varvalue);
    conf.put(key, value);
    // conf.verifyAndSet(key, value);
    if (register) {
      SessionState.get().getOverriddenConfigurations().put(key, value);
    }
  }

  private SortedMap<String, String> propertiesToSortedMap(Properties p) {
    SortedMap<String, String> sortedPropMap = new TreeMap<String, String>();
    for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
      sortedPropMap.put((String) entry.getKey(), (String) entry.getValue());
    }
    return sortedPropMap;
  }

  private SortedMap<String, String> mapToSortedMap(Map<String, String> data) {
    SortedMap<String, String> sortedEnvMap = new TreeMap<String, String>();
    sortedEnvMap.putAll(data);
    return sortedEnvMap;
  }

  private CommandProcessorResponse getVariable(String varname) {
    SessionState ss = SessionState.get();
    if (varname.equals("silent")) {
      ss.out.println("silent" + "=" + ss.getIsSilent());
      return createProcessorSuccessResponse();
    }
    if (varname.startsWith(SetProcessor.SYSTEM_PREFIX)) {
      String propName = varname.substring(SetProcessor.SYSTEM_PREFIX.length());
      String result = System.getProperty(propName);
      if (result != null) {
        ss.out.println(SetProcessor.SYSTEM_PREFIX + propName + "=" + result);
        return createProcessorSuccessResponse();
      } else {
        ss.out.println(propName + " is undefined as a system property");
        return new CommandProcessorResponse(1);
      }
    } else if (varname.indexOf(SetProcessor.ENV_PREFIX) == 0) {
      String var = varname.substring(ENV_PREFIX.length());
      if (System.getenv(var) != null) {
        ss.out
            .println(SetProcessor.ENV_PREFIX + var + "=" + System.getenv(var));
        return createProcessorSuccessResponse();
      } else {
        ss.out.println(varname + " is undefined as an environmental variable");
        return new CommandProcessorResponse(1);
      }
    } else if (varname.indexOf(SetProcessor.HIVECONF_PREFIX) == 0) {
      String var = varname.substring(SetProcessor.HIVECONF_PREFIX.length());
      if (ss.getConf().get(var) != null) {
        ss.out.println(SetProcessor.HIVECONF_PREFIX + var + "="
            + ss.getConf().get(var));
        return createProcessorSuccessResponse();
      } else {
        ss.out.println(varname
            + " is undefined as a hive configuration variable");
        return new CommandProcessorResponse(1);
      }
    } else if (varname.indexOf(SetProcessor.HIVEVAR_PREFIX) == 0) {
      String var = varname.substring(SetProcessor.HIVEVAR_PREFIX.length());
      if (ss.getStormVariables().get(var) != null) {
        ss.out.println(SetProcessor.HIVEVAR_PREFIX + var + "="
            + ss.getStormVariables().get(var));
        return createProcessorSuccessResponse();
      } else {
        ss.out.println(varname + " is undefined as a hive variable");
        return new CommandProcessorResponse(1);
      }
    } else {
      dumpOption(varname);
      return createProcessorSuccessResponse();
    }
  }

  private CommandProcessorResponse createProcessorSuccessResponse() {
    return new CommandProcessorResponse(0, null, null);
  }

  @Override
  public CommandProcessorResponse run(String command, Context context) {
    SessionState ss = SessionState.get();

    String nwcmd = command.trim();
    if (nwcmd.equals("")) {
      // dumpOptions(ss.getConf().getChangedProperties());
      return createProcessorSuccessResponse();
    }

    if (nwcmd.equals("-v")) {
      // dumpOptions(ss.getConf().getAllProperties());
      return createProcessorSuccessResponse();
    }

    String[] part = new String[2];
    int eqIndex = nwcmd.indexOf('=');

    if (nwcmd.contains("=")) {
      if (eqIndex == nwcmd.length() - 1) { // x=
        part[0] = nwcmd.substring(0, nwcmd.length() - 1);
        part[1] = "";
      } else { // x=y
        part[0] = nwcmd.substring(0, eqIndex).trim();
        part[1] = nwcmd.substring(eqIndex + 1).trim();
      }
      if (part[0].equals("silent")) {
        ss.setIsSilent(getBoolean(part[1]));
        return new CommandProcessorResponse(0);
      }
      return setVariable(part[0], part[1]);
    } else {
      return getVariable(nwcmd);
    }

  }

}
