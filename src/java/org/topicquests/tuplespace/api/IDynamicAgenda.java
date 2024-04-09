/*
 * Copyright 2024 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.tuplespace.api;

import java.util.Iterator;
import java.util.Map;

/**
 * @author jackpark
 */
public interface IDynamicAgenda {

	//////////////////////
	// Channels
	//////////////////////
	/**
	 * Creates a new Channel which is held internally
	 * @param name
	 */
	void createChannel(String name);
	
	ITupleSpace getChannel(String name);
	
	
	//////////////////////
	// Tuples
	//////////////////////
	
	/**
	 * Returns empty tuple
	 * @param channelName
	 * @return
	 */
	ITuple newTuple(String channelName);
	
	/**
	 * 
	 * @return
	 */
	ITemplate newTemplate();
	
	/**
	 * 
	 * @param channelName
	 * @param properties instance of ConcurrentHashMap
	 * @return
	 */
	ITuple newTuple(String channelName, Map<String, Object> properties);
	
	void put(String channelNamee, ITuple tuple);
	
	/**
	 * 
	 * @param template
	 * @param waitTime
	 * @return can return {@code null}
	 */
	ITuple read(ITuple template, long waitTime);
	
	/**
	 * 
	 * @param template
	 * @param waitTime
	 * @return can return {@code null}
	 */
	ITuple take(ITuple template, long waitTime);
	
	Iterator<ITuple> listTuples(String channelName);

}
