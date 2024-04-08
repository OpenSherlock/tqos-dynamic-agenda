/*
 * ============================================================================
 *
 *  File:     TupleCollectionById.java
 *----------------------------------------------------------------------------
 *
 * Copyright 2008 Erlend Nossum
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 *  Description:  See javadoc below
 *
 *  Created:      May 2, 2008
 * ============================================================================ 
 */

package org.semispace;

import org.semispace.api.ISemiSpaceTuple;
import org.semispace.exception.SemiSpaceInternalException;
import org.topicquests.util.LoggingPlatform;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Erlend Nossum
 * @author park -- changes to some code
 * </p>Collects {@link ISemiSpaceTuple} objects by <code>id</code></p>
 */
public class TupleCollectionById implements Iterable<ISemiSpaceTuple>{
	private LoggingPlatform log = LoggingPlatform.getLiveInstance();
    private Map<Long, ISemiSpaceTuple> elements = new ConcurrentHashMap<Long, ISemiSpaceTuple>();
    private boolean waiting;
    
    public synchronized int size() {
        return elements.size();
    }

    public static synchronized TupleCollectionById createNewCollection(Tuple holder) {
        TupleCollectionById hc = new TupleCollectionById();
        hc.addHolder(holder);
        return hc;
    }

    public synchronized ISemiSpaceTuple removeHolderById( long id ) {
        ISemiSpaceTuple found = elements.remove(Long.valueOf(id));
        return found;
    }

    /**
     * Searching for holder elements with given ID
     */
    public synchronized ISemiSpaceTuple findById(long id) {
        ISemiSpaceTuple found = elements.get(Long.valueOf(id));
        return found;
    }

    public synchronized void addHolder(ISemiSpaceTuple add ) {
        ISemiSpaceTuple old = elements.put( Long.valueOf(add.getId()), add);
        String j = add.getJSON();
//        System.out.println("TupleCollectionById "+j);
        if ( old != null ) {
            throw new SemiSpaceInternalException("Unexpected duplication id IDs. Found twice: "+old.getId());
        }
//        System.out.println("TupleCollectionById-2 "+elements);
        notifyAll();
    }

    public synchronized Tuple[] toArray() {
        return elements.values().toArray( new Tuple[0]);
    }

    @Override
    public synchronized Iterator<ISemiSpaceTuple> iterator() {
//    	System.out.println("TupleCollcetionById "+elements);
        // TODO Will this be thread safe?
        /*
        List<Tuple> defensive = new ArrayList();
        defensive.addAll(elements.values());
        return defensive.iterator();
        */
        return elements.values().iterator();
    }

    public synchronized  void waitHolder(long timeout) {
        if (elements.isEmpty()) {
    		try {
    		    waiting = true;
    			wait(timeout);
    		} catch (InterruptedException ex) {
                log.logDebug("InterruptedException ignored: "+ex);
    		} finally {
    		    waiting = false;
    		}
    		
    	}
    }
    
    public boolean isWaiting() {
        return waiting;
    }
}
