package org.topicquests.tuplespace;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.topicquests.tuplespace.api.IDynamicAgenda;
import org.topicquests.tuplespace.api.ITuple;
import org.topicquests.tuplespace.api.ITupleSpace;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void put(String channelNamee, ITuple tuple) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ITuple read(ITuple template, long waitTime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ITuple take(ITuple template, long waitTime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<ITuple> listTuples(String channelName) {
		// TODO Auto-generated method stub
		return null;
	}

}
