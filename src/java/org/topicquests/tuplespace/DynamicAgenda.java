/*
 * Copyright 2024 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.tuplespace;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.topicquests.tuplespace.api.IDynamicAgenda;
import org.topicquests.tuplespace.api.ITemplate;
import org.topicquests.tuplespace.api.ITuple;
import org.topicquests.tuplespace.api.ITupleSpace;
import org.topicquests.tuplespace.impl.TemplateImpl;
import org.topicquests.tuplespace.impl.TupleImpl;
import org.topicquests.tuplespace.impl.TupleSpaceImpl;

import java.util.concurrent.ConcurrentHashMap;

public class DynamicAgenda implements IDynamicAgenda{
	private Map<String, ITupleSpace> channels;
	
	
	public DynamicAgenda() {
		channels = new ConcurrentHashMap<String, ITupleSpace>();
	}

	@Override
	public void createChannel(String name) {
		channels.put(name, new TupleSpaceImpl(name) );
	}

	@Override
	public ITupleSpace getChannel(String name) {
		return channels.get(name);
	}

	@Override
	public ITuple newTuple(String channelName) {
		return new TupleImpl(channelName);
	}

	@Override
	public ITuple newTuple(String channelName, Map<String, Object> properties) {
		return new TupleImpl(channelName, properties);
	}

	@Override
	public void put(String channelName, ITuple tuple) {
		ITupleSpace c = getChannel(channelName);
		c.insert(tuple);
	}

	@Override
	public ITuple read(String channelName, ITemplate template, long waitTime) {
		ITupleSpace c = getChannel(channelName);
		return c.read(template, waitTime);
	}

	@Override
	public ITuple take(String channelName, ITemplate template, long waitTime) {
		ITupleSpace c = getChannel(channelName);
		return c.take(template, waitTime);
	}

	@Override
	public Iterator<ITuple> listTuples(String channelName) {
		ITupleSpace c = getChannel(channelName);
		return c.tuples();
	}

	@Override
	public ITemplate newTemplate() {
		return new TemplateImpl();
	}

	@Override
	public void decayAll(int howMuch) {
		ITupleSpace ts;
		ITuple t;
		Iterator<ITupleSpace> itr = channels.values().iterator();
		Iterator<ITuple> itx;
		while (itr.hasNext()) {
			ts = itr.next();
			itx =ts.tuples();
			while (itx.hasNext()) {
				t = itx.next();
				int p = t.getPriority() ;
				p += howMuch;
				t.setPriority(p);
			}
		}
	}

	@Override
	public void addValue(String channelName, ITemplate template, int howMuch) {
		ITupleSpace c = getChannel(channelName);
		ITuple t = c.read(template, 10000);
		if (t != null) {
			int p = t.getPriority() ;
			p += howMuch;
			t.setPriority(p);
		}
		else throw new RuntimeException("Add Value missing tuple match "+template.toString());
	}

}
