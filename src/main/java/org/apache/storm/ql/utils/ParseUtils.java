package org.apache.storm.ql.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.storm.ql.parse.ASTNode;

public class ParseUtils {

	/**
	 * Performs a descent of the leftmost branch of a tree, stopping when either
	 * a node with a non-null token is found or the leaf level is encountered.
	 * 
	 * @param tree
	 *            candidate node from which to start searching
	 * 
	 * @return node at which descent stopped
	 */
	public static ASTNode findRootNonNullToken(ASTNode tree) {
		while ((tree.getToken() == null) && (tree.getChildCount() > 0)) {
			tree = (ASTNode) tree.getChild(0);
		}
		return tree;
	}

	public static String parseString(Object o, String defaultValue) {
		if (o == null) {
			return defaultValue;
		}
		return String.valueOf(o);
	}

	public static String stringifyError(Throwable error) {
		StringWriter result = new StringWriter();
		PrintWriter printer = new PrintWriter(result);
		error.printStackTrace(printer);
		printer.close();
		return result.toString();
	}
	
	  private static final Class<?>[] EMPTY_ARRAY = new Class[] {};

	  private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE =
	      new ConcurrentHashMap<Class<?>, Constructor<?>>();

	  public static <T> T newInstance(String classStr)
	      throws ClassNotFoundException {
	    T result;

	    Class<T> theClass = (Class<T>) Class.forName(classStr);
	    try {
	      Constructor<T> meth = (Constructor<T>) CONSTRUCTOR_CACHE.get(theClass);
	      if (meth == null) {
	        meth = theClass.getDeclaredConstructor(EMPTY_ARRAY);
	        meth.setAccessible(true);
	        CONSTRUCTOR_CACHE.put(theClass, meth);
	      }
	      result = meth.newInstance();
	    } catch (Exception e) {
	      throw new RuntimeException(e);
	    }
	    return result;
	  }

	  public static <T> T newInstance(String classStr, ClassLoader classLoader)
	      throws ClassNotFoundException {
	    T result;

	    @SuppressWarnings("unchecked")
	    Class<T> theClass = (Class<T>) classLoader.loadClass(classStr);
	    // Class<T> theClass = (Class<T>) Class.forName(classStr, false,
	    // classLoader);
	    try {
	      Constructor<T> meth = (Constructor<T>) CONSTRUCTOR_CACHE.get(theClass);
	      if (meth == null) {
	        meth = theClass.getDeclaredConstructor(EMPTY_ARRAY);
	        meth.setAccessible(true);
	        CONSTRUCTOR_CACHE.put(theClass, meth);
	      }
	      result = meth.newInstance();
	    } catch (Exception e) {
	      throw new RuntimeException(e);
	    }
	    return result;
	  }

}
