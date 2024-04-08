/*
 * ============================================================================
 *
 *  File:     TupleCollection.java
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
 *  Created:      Apr 27, 2008
 * ============================================================================ 
 */

package org.semispace;

import org.semispace.api.ISemiSpaceTuple;
import org.semispace.api.ITupleFields;
import org.semispace.exception.SemiSpaceObjectException;
import org.semispace.exception.SemiSpaceUsageException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Container for holder elements.
 */
public class TupleCollection {
    private AtomicLong idseq = new AtomicLong();

    private Map<String, TupleCollectionById> heads = null;

    /**
     * Read / write lock
     */
    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private static TupleCollection instance = new TupleCollection();
    
    private TupleCollection() {
        heads = new ConcurrentHashMap<String, TupleCollectionById>();
    }

    public static synchronized TupleCollection retrieveContainer() {
        return instance;
    }
    
    public TupleCollectionById next(String tag) {
        rwl.writeLock().lock();
        try {
            return heads.get(tag);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    public ISemiSpaceTuple removeHolderById(long id, String className) {
        ISemiSpaceTuple toReturn = null;
        rwl.writeLock().lock();
        try {
            TupleCollectionById head = heads.get(className);
            if ( head == null ) {
                return null;
            }
            toReturn = head.removeHolderById(id);
            if ( (idseq.longValue() % 5000) == 0 && head.size() < 1 ) {
                // It may not be deterministic when this actually occurs, but that does not matter. 
                removeEmptyHeads();
            }

        } finally {
            rwl.writeLock().unlock();
        }
        return toReturn;
    }

    /**
     * Instance need to have been locked in beforehand.
     * Intended to be used occasionally in order to remove empty heads.
     */
    private void removeEmptyHeads() {
        List<String> toPurge = new ArrayList<String>();
        for ( String name : heads.keySet()) {
            TupleCollectionById head = heads.get( name);
            if ( !head.isWaiting() && head.size() < 1 ) {
                toPurge.add(name);
            }
        }
        for ( String name : toPurge ) {
            heads.remove(name);
        }
    }

    public ISemiSpaceTuple findById(long id, String className) {
        rwl.readLock().lock();

        try {
            TupleCollectionById n = heads.get(className);
            if ( n == null ) {
                return null;
            }
            return n.findById(id);
        } finally {
            rwl.readLock().unlock();
        }
    }

    /**
     * Protected for the benefit of junit tests.
     */
    protected void addHolder(Tuple add) {
        rwl.writeLock().lock();
        try {
            if (add == null) {
                throw new SemiSpaceUsageException("Illegal to add null");
            }
            if ( add.getTag() == null ) {
                throw new SemiSpaceObjectException("Need classname in holder with contents "+add.getJSON());
            }
            TupleCollectionById head = heads.get( add.getTag() );
            if (head == null) {
                head = TupleCollectionById.createNewCollection(add);
                heads.put( add.getTag(), head);
            } else {
                head.addHolder(add);
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Method presumed called on first object, which is the holder object. Returning count, excluding holder.
     */
    public int size() {
        rwl.readLock().lock();
        try {
            if (heads == null) {
                return 0;
            }
            int size = 0;
            
            for ( TupleCollectionById head : heads.values() ) {
                size += head.size();
            }
            return size;
        } finally {
            rwl.readLock().unlock();
        }

    }
    
    public String[] retrieveGroupNames() {
        rwl.readLock().lock();
        String[] result = null;
        try {
            result = heads.keySet().toArray(new String[0]);
        } finally {
            rwl.readLock().unlock();
        }
        return result;
    }

    public ISemiSpaceTuple readHolderWithId(long id) {
        String[] cnames = retrieveClassNames();
        for (String lookup : cnames ) {
            TupleCollectionById next = next(lookup);
            ISemiSpaceTuple toReturn = next.findById(id);
            if ( toReturn != null ) {
                return toReturn;
            }
        }
        return null;
    }
    
    /**
     * Return all ids present. Notice that this method will
     * be rather network expensive, and is only intended to 
     * be used for persistence purposes.
     */
    public Long[] findAllHolderIds() {
        List<Long> allIds = new ArrayList<Long>();
        String[] cnames = retrieveClassNames();
        for (String lookup : cnames ) {
            TupleCollectionById next = next(lookup);
            rwl.readLock().lock();
            try {
                for ( Tuple elem : next.toArray()) {
                    allIds.add(Long.valueOf( elem.getId() ));
                }

            } finally {
                rwl.readLock().unlock();
            }
        }
        return allIds.toArray(new Long[0]);
    }
    
    private String[] retrieveClassNames() {
        rwl.readLock().lock();
        String[] cnames = null;
        try {
            cnames = heads.keySet().toArray(new String[0]);
        } finally {
            rwl.readLock().unlock();
        }

        return cnames;
    }

    public Tuple addHolder(String json, long liveUntil, String entryClassName, Map<String, Object> searchMap) {
        // Methods used herein are thread safe, and therefore no reason to lock at this point.
        long holderId = incrementReturnNextId(); 
        if (searchMap != null) {
        	Long id = (Long)searchMap.get(ITupleFields.ID);
        	if (id != null)
        		holderId = id.longValue();
        }
        Tuple holder = new Tuple(json, liveUntil, entryClassName, holderId, searchMap);
        addHolder(holder);
        return holder;
    }

    public long incrementReturnNextId() {
        return idseq.incrementAndGet();
    }

    public void waitHolder(String className, long timeout) {
        TupleCollectionById e = null;
        rwl.writeLock().lock();
        try {
            e = heads.get(className);
            if (e == null) {
                e = new TupleCollectionById();
                heads.put(className, e);
            }
        } finally {
            rwl.writeLock().unlock();
        }
        e.waitHolder(timeout);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for ( TupleCollectionById head : heads.values() ) {
            sb.append("[");
            for (ISemiSpaceTuple next : head) {
                sb.append(next.getTag()).append(":");
                sb.append(next.getJSON());
                sb.append("  ");
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
