package org.apache.storm.ql.parse;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.storm.ql.session.SessionState;
import org.apache.storm.ql.session.SessionState.LogHelper;
import org.apache.storm.ql.utils.ParseUtils;

import backtype.storm.spout.ISpout;
import backtype.storm.topology.IBasicBolt;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import backtype.storm.utils.Utils;

public class RegisterSemanticAnalyzer extends BaseSemanticAnalyzer {
	private final LogHelper console;

	public RegisterSemanticAnalyzer(TopologyBuilder builder) {
		super(builder);
		Log LOG = LogFactory.getLog("RegisterSemanticAnalyzer");
		console = new LogHelper(LOG);
	}

	@Override
	public void analyzeInternal(ASTNode ast, Context context) throws Exception {
		switch (ast.getToken().getType()) {
		case StormParser.TOK_REGISTER:
			ASTNode pro = (ASTNode) ast.getChild(0);
			if (pro.getToken().getType() == StormParser.TOK_PROPERTY) {
				ASTNode cnAst = (ASTNode) pro.getChild(0);
				String alias = cnAst.getText(); // word
				ASTNode gpAst = (ASTNode) pro.getChild(1);
				if (gpAst.getToken().getType() == StormParser.TOK_BOLT) {
					if (gpAst.getChildren().size() != 3) {
						throw new RuntimeException("For Bolt size should be 3");
					}
					String boltClassName = (String) gpAst.getChild(0).getText();
					// TODO
					if (boltClassName.startsWith("\"")) {
						boltClassName = boltClassName.substring(1,
								boltClassName.lastIndexOf("\""));
					}
					String boltParallelismHint = (String) gpAst.getChild(1)
							.getText();
					ASTNode fnAst = (ASTNode) gpAst.getChild(2);
					String groupingType = null;
					String componentId = null;
					List<String> fields = new ArrayList<String>();
					if (fnAst.getToken().getType() == StormParser.TOK_DOT) {
						int dotAstSize = fnAst.getChildren().size();
						groupingType = fnAst.getChild(0).getText();
						componentId = fnAst.getChild(1).getText();
						if (componentId.startsWith("\"")) {
							componentId = componentId.substring(1,
									componentId.lastIndexOf("\""));
						}
						if (dotAstSize >= 3) {
							int cnt = 2;
							while (cnt < dotAstSize) {
								String field = fnAst.getChild(cnt).getText();
								if (field.startsWith("\"")) {
									field = field.substring(1,
											field.lastIndexOf("\""));
								}
								fields.add(field);
								cnt++;
							}
						}
					}
					Object bolt = null;
					try {
						bolt = ParseUtils.newInstance(boltClassName,
								SessionState.get().getConf().getClassLoader());
					} catch (ClassNotFoundException e) {
						console.printError("ClassNotFound: " + boltClassName);
					} catch (Exception e) {
						throw new Exception(e);
					}
					if (bolt instanceof IBasicBolt) {
						context.getBoltDescs().put(
								alias,
								new BoltDesc((IBasicBolt) bolt, Utils.getInt(
										boltParallelismHint, 1), groupingType,
										componentId, new Fields(fields)));
					} else if (bolt instanceof IRichBolt) {
						bolt = (IRichBolt) bolt;
						context.getBoltDescs().put(
								alias,
								new BoltDesc((IRichBolt) bolt, Utils.getInt(
										boltParallelismHint, 1), groupingType,
										componentId, new Fields(fields)));
					}

				} else if (gpAst.getToken().getType() == StormParser.TOK_SPOUT) {
					if (gpAst.getChildren().size() != 2) {
						throw new RuntimeException("For Spout size should be 2");
					}
					String spoutClassName = (String) gpAst.getChild(0)
							.getText();
					// TODO
					if (spoutClassName.startsWith("\"")) {
						spoutClassName = spoutClassName.substring(1,
								spoutClassName.lastIndexOf("\""));
					}
					String spoutParallelismHint = (String) gpAst.getChild(1)
							.getText();
					Object spout = ParseUtils.newInstance(spoutClassName,
							SessionState.get().getConf().getClassLoader());
					if (spout instanceof IRichSpout) {
						context.getSpoutDescs().put(
								alias,
								new SpoutDesc((IRichSpout) spout, Utils
										.getInt(spoutParallelismHint, 1)));
					} else if (spout instanceof ISpout) {
						context.getSpoutDescs().put(
								alias,
								new SpoutDesc((ISpout) spout, Utils.getInt(
										spoutParallelismHint, 1)));
					}
				}
			}
		default:
		}
	}
}
