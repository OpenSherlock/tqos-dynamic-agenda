/*
 * Copyright 2012, TopicQuests
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.semispace;

import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.semispace.api.ISemiSpaceTuple;
import org.semispace.exception.SemiSpaceInternalException;
import org.topicquests.util.LoggingPlatform;

/**
 * @author park
 * </p>Collects {@link ISemiSpaceTuple} objects by <code>priority</code></p>
 * <p>Tuples with tag = "task" are special tuples; they belong here</p>
 */
public class TupleCollectionByPriority implements Iterable<ISemiSpaceTuple> {
	private LoggingPlatform log = LoggingPlatform.getLiveInstance();
	/** Long is priority */
    private Map<Long, ISemiSpaceTuple> elements = new ConcurrentHashMap<Long, ISemiSpaceTuple>();
    private List<Long> sortedPriorities = new ArrayList<Long>();
    private boolean waiting;

	/**
	 * 
	 */
	public TupleCollectionByPriority() {
		// TODO Auto-generated constructor stub
	}
    public static synchronized TupleCollectionByPriority createNewCollection(Tuple holder) {
    	TupleCollectionByPriority hc = new TupleCollectionByPriority();
        hc.addHolder(holder);
        return hc;
    }
    
    public synchronized void addHolder(ISemiSpaceTuple add ) {
        ISemiSpaceTuple old = elements.put( Long.valueOf(add.getPriority()), add);
        notifyAll();
    }

	@Override
	public Iterator<ISemiSpaceTuple> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

}
