/**
 * 
 */
package org.topicquests.tuplespace.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.topicquests.tuplespace.api.ITemplate;
import org.topicquests.tuplespace.api.ITuple;
import org.topicquests.tuplespace.impl.TemplateImpl;
import org.topicquests.tuplespace.impl.TupleImpl;

/**
 * 
 */
public class TupleUtil {

	/**
	 * 
	 */
	public TupleUtil() {
	}

	public static ITuple createTuple(String channelName, int priority, Map<String,Object> properties) {
		ITuple result = new TupleImpl(channelName, properties);
		result.setPriority(priority);
		return result;
	}
	
	public static ITemplate createTemplate(Map<String,Object> properties) {
		return new TemplateImpl(properties);
	}
	
	public static Map<String, Object> newProperties() {
		return new ConcurrentHashMap<String, Object>();
	}
}
