/*
 * ============================================================================
 *
 *  File:     SemiSpace.java
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
 *  Created:      23. des.. 2007
 * ============================================================================ 
 */

package org.semispace;

import org.json.simple.JSONObject;

import org.semispace.admin.SemiSpaceAdmin;
import org.semispace.api.ISemiSpace;
import org.semispace.api.ISemiSpaceAdmin;
import org.semispace.api.ISemiEventListener;
import org.semispace.api.ISemiSpaceTuple;
import org.semispace.api.ITupleFields;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.event.SemiExpirationEvent;
import org.semispace.event.SemiRenewalEvent;
import org.semispace.event.SemiTakenEvent;
import org.semispace.exception.SemiSpaceInternalException;
import org.semispace.exception.SemiSpaceObjectException;
import org.semispace.exception.SemiSpaceUsageException;
import org.topicquests.util.LoggingPlatform;
import org.topicquests.util.Tracer;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * A tuple space implementation which can be distributed with terracotta. This is
 * the main class from which the SemiSpace interface is obtained.
 */
public class SemiSpace implements ISemiSpace {
	private LoggingPlatform log = LoggingPlatform.getLiveInstance();
    private static final String ADMIN_GROUP_IS_FLAGGED = "adminGroupIsFlagged";

    public static final long ONE_DAY = 86400 * 1000;

    private static SemiSpace instance = null;

    private long listenerId = 0;

    private TupleCollection elements = null;

    private transient Map<Long, ListenerHolder> listeners;

    private transient ISemiSpaceAdmin admin;

    private transient Map<String, Field[]> classFieldMap = new WeakHashMap<String, Field[]>();

    private SemiSpaceStatistics statistics;

    
    private EventDistributor eventDistributor = EventDistributor.getInstance();


    /**
     * Tuple for sanity check of stored class. It should not be an inner class.
     */
    private Set<String> checkedClassSet = new HashSet<String>();

    /**
     * 
     * @param spaceName
     */
    public SemiSpace(String spaceName) {
 
        elements = TupleCollection.retrieveContainer();
        listeners = new ConcurrentHashMap<Long, ListenerHolder>();
        statistics = new SemiSpaceStatistics();
        setAdmin(new SemiSpaceAdmin(this));
        if (admin.hasBeenInitialized()) {
            admin.performInitialization();
         }
        instance = this;
    }


    public static synchronized ISemiSpace retrieveSpace() {
    	return instance;
    }
    /**
     * None of the parameters can be null
     * 
     * @return Returning null if something went wrong or was wrong, a registration object otherwise.
     * @see org.semispace.api.ISemiSpace#notify(ISemiSpaceTuple, ISemiEventListener, long)  
     */
    @Override
    public SemiEventRegistration notify(ISemiSpaceTuple tmpl, ISemiEventListener listener, long duration) {
        if (tmpl == null) {
            log.logDebug("Not registering notification on null object.");
            return null;
        }
//        Map<String, String> searchProps = retrievePropertiesFromObject(tmpl);
//        SemiEventRegistration registration = notify(searchProps, listener, duration);
        SemiEventRegistration registration = notify(tmpl, listener, duration);
        return registration;
    }

    /**
     * Basically the same as the notify method demanded by the interface, except that it accepts search properties
     * directly. Used from the web services class. None of the parameters can be null
     * 
     * @return Returning null if something went wrong or was wrong, a registration object otherwise.
     * /
    public SemiEventRegistration notify(Map<String, String> searchProps, ISemiEventListener listener, long duration) {
        if (listener == null) {
            log.logDebug("Not allowing listener to be null.");
            return null;
        }
        if (searchProps == null) {
            log.logDebug("Not allowing search props to be null");
            return null;
        }
        if (duration <= 0) {
            log.logDebug("Not registering notification when duration is <= 0. It was " + duration);
            return null;
        }

        ListenerHolder holder = null;
        listenerId++;
        holder = new ListenerHolder(listenerId, listener, duration + admin.calculateTime(),
                searchProps);
        if ( listeners.put(Long.valueOf(holder.getId()), holder) != null ) {
            throw new SemiSpaceInternalException("Internal assertion error. Listener map already had element with id "+holder.getId());
        }
        statistics.increaseNumberOfListeners();
        SemiLease lease = new ListenerLease(holder, this);
        SemiEventRegistration eventRegistration = new SemiEventRegistration(holder.getId(), lease);
        return eventRegistration;
    }

    /**
     * Distributed notification method.
     */
    public void notifyListeners(DistributedEvent distributedEvent) {
        final List<ISemiEventListener> toNotify = new ArrayList<ISemiEventListener>();
        ListenerHolder[] listenerArray = listeners.values().toArray(new ListenerHolder[0]);
        Arrays.sort( listenerArray, new ShortestTtlComparator());
        for (ListenerHolder listener : listenerArray) {
            if (listener.getLiveUntil() < admin.calculateTime()) {

                cancelListener( listener );

            } else if (hasSubSet(distributedEvent.getEntrySet(), listener.getSearchMap(), false, null, null)) {
                ISemiEventListener notifyMe = listener.getListener();
                toNotify.add(notifyMe);
            }
        }
        final SemiEvent event = distributedEvent.getEvent();
        for (ISemiEventListener notify : toNotify) {
                try {
                    notify.notify(event);
                } catch (ClassCastException ignored ) {
                    // Sadly enough, I need to ignore this due to type erasure.
                }
        }

        
        admin.notifyAboutEvent(distributedEvent);
    }

    /**
     * Notice that the lease time is the time in milliseconds the element is wants to live, <b>not</b> the system time
     * plus the time to live.
     * 
     * @return Either the resulting lease or null if an error
     */
    @Override
    public SemiLease write(final ISemiSpaceTuple entry, final long leaseTimeMs) {
        if (entry == null) {
            return null;
        }
//    	log.logDebug( "WRITE: "+entry.getJSON());

//System.out.println("SemiSpace.write-1 "+entry);
        WrappedInternalWriter write = new WrappedInternalWriter(entry, leaseTimeMs);
        
        ExecutorService thd = admin.getThreadPool();
//System.out.println("SemiSpace.write-2 "+thd.isShutdown()+" "+thd.isTerminated());
        
        Future<?> future = thd.submit(write);
        Exception exception = null;
        try {
            future.get();
        } catch ( CancellationException e ) {
            log.logError("Got exception", e);
            exception = e;            
        } catch (InterruptedException e) {
            log.logError("Got exception", e);
            exception = e;
        } catch (ExecutionException e) {
            log.logError("Got exception", e);
            exception = e;
        } 
//System.out.println("SemiSpace.write-3 "+write.getException());
        
        if (write.getException() != null || exception != null) {
            String error = " Writing object (of type " + entry.getClass().getName()
                    + ") to space gave exception. XML version: " /*+ objectToXml(entry)*/;
            if ( write.getException() != null ) {
                exception = write.getException();
            }
            throw new SemiSpaceObjectException(error, exception);
        }
//System.out.println("SemiSpace.write-4 "+write.getLease());
        return write.getLease();
    }

    private SemiLease writeInternally(ISemiSpaceTuple entry, long leaseTimeMs) {
        String tag = entry.getTag();
         Map<String, Object> searchMap = entry.getSearchMap();
//System.out.println("WriteInternally "+tag+" "+searchMap);
        return writeToElements(tag, leaseTimeMs, searchMap);
    }

    /**
     * This method is public for the benefit of the web services, which shortcuts the writing process.
     * All values are expected to be non-null and valid upon entry.
     */
    public SemiLease writeToElements(String tag, long leaseTimeMs, Map<String, Object> searchMap) {
        Tuple holder = null;
        if ( !checkedClassSet.contains( tag )) {
            checkedClassSet.add(tag);
    //        if ( json.contains("<outer-class>")) {
    //            log.logDebug("It seems that "+tag+" is an inner class. This is DISCOURAGED as it WILL serialize the outer " +
     //                   "class as well. If you did not intend this, note that what you store MAY be significantly larger than you " +
     //                   "expected. This warning is printed once for each class type.");
     //       }
        }
        // Need to add holder within lock. This indicates that TupleCollection has some thread safety issues
        holder = elements.addHolder(null, admin.calculateTime() + leaseTimeMs, tag, searchMap);
//System.out.println("writeToElements-1 "+holder.getId());
//System.out.println("writeToElements-2 "+holder.getSearchMap());
        SemiLease lease = new ElementLease(holder, this);
        statistics.increaseWrite();
        
        SemiAvailabilityEvent semiEvent = new SemiAvailabilityEvent(holder.getId());
        distributeEvent(new DistributedEvent(holder.getTag(), semiEvent,
                holder.getSearchMap()));

        return lease;
    }
    
    private void distributeEvent(final DistributedEvent distributedEvent) {
        final Runnable distRunnable = new Runnable() {
            @Override
            public void run() {
                eventDistributor.distributeEvent(distributedEvent);
            }
        };
        if (!getAdmin().getThreadPool().isShutdown()) {
            try {
                admin.getThreadPool().execute(distRunnable);
            } catch ( RejectedExecutionException e ) {
                log.logError("Could not schedule notification",e);
            }
        } else {
            log.logDebug("Thread pool is shut down, not relaying event");
        }
    }


    @Override
    public ISemiSpaceTuple read(ISemiSpaceTuple tmpl, long timeout) {
//    	log.logDebug( "READ-: "+tmpl.getJSON());
        ISemiSpaceTuple found = null;
        if (tmpl != null) {
            found = findOrWaitLeaseForTemplate(getPropertiesForObject(tmpl), timeout, false);
        }
        if (found != null) {
 //       	System.out.println("READ Found "+found.getId());
           	log.logDebug( "READ+: "+found);
        } else {
 //       	System.out.println("READ Found "+found);
           	log.logDebug( "READ+: empty");
        }
        return found; //xmlToObject(found);
    }

    /**
     * Public for the benefit of the webservices interface.
     *
     * @param timeout how long to wait in milliseconds. If timeout is zero or negative, query once. 
     * @param isToTakeTheLease true if the element shall be marked as taken.
     * @return ISemiSpaceTuple version of data, if found, or null
     */
    public ISemiSpaceTuple findOrWaitLeaseForTemplate(Map<String, Object> templateSet, long timeout, boolean isToTakeTheLease) {
        final long until = admin.calculateTime() + timeout;
        long systime = admin.calculateTime();
        String tag = (String)templateSet.get("tag");
//        log.logDebug("findOrWaitLeaseForTemplate "+templateSet+" "+timeout);
        ISemiSpaceTuple found = null;
        long subtract = 0;
        do {
            final long duration = timeout - subtract;
            if (isToTakeTheLease) {
                statistics.increaseBlockingTake();
            } else {
                statistics.increaseBlockingRead();
            }

            found = findLeaseForTemplate(templateSet, isToTakeTheLease);

            if ( found == null && duration > 0) {
                elements.waitHolder(tag, duration);
            }
            if (isToTakeTheLease) {
                statistics.decreaseBlockingTake();
            } else {
                statistics.decreaseBlockingRead();
            }

            final long now = getAdmin().calculateTime();
			subtract += now - systime;
			systime = now;
        } while (found == null && systime < until );
        return found;
    }

    @Override
    public ISemiSpaceTuple readIfExists(ISemiSpaceTuple tmpl) {
        return read(tmpl, 0);
    }

    /**
     * 
     * @return ISemiSpaceTuple version of found object
     */
    private ISemiSpaceTuple findLeaseForTemplate(Map<String, Object> templateSet, boolean isToTakeTheLease) {
        ISemiSpaceTuple found = null;

        List<ISemiSpaceTuple> toEvict = new ArrayList<ISemiSpaceTuple>();
        String tag = (String)templateSet.get("tag");

        // Read all elements until element is found. Side effect is to generate eviction list.
        if ( tag == null ) {
            throw new SemiSpaceObjectException("Did not expect classname to be null");
        }
//        log.logDebug( "findLeaseForTemplate- "+templateSet);
        TupleCollectionById next = elements.next(tag);
        if ( next != null ) {
            Iterator<ISemiSpaceTuple> it = next.iterator();
            while ( found == null && it.hasNext()) {
                ISemiSpaceTuple elem = it.next();
 //       System.out.println("FINDING ON "+elem.getId());
 //       log.logDebug( "FINDING ON "+elem.getId());
                //see if it's running out of time
                if (elem.getLiveUntil() < admin.calculateTime()) {
                    toEvict.add(elem);
                    elem = null;
                }
                if (elem != null && hasSubSet(elem.getSearchMap().entrySet(), templateSet, isToTakeTheLease, elem.getSearchMap(), elem)) {
                    found = elem;
                }
            }
        }

        for (ISemiSpaceTuple evict : toEvict) {
            if (!cancelElement(Long.valueOf(evict.getId()), false, evict.getTag())) {
                log
                        .logDebug("Element with id "
                                + evict.getId()
                                + " should exist in most cases. This time, it is probably missing as it belongs to a timed out query.");
            }
        }
        boolean needToRetake = false;
//        log.logDebug("findLeaseForTemplate found "+found);
        if (found != null) {
            if (isToTakeTheLease && !cancelElement(Long.valueOf(found.getId()), isToTakeTheLease, found.getTag())) {
                log.logDebug("Element with id " + found.getId() + " ceased to exist during take. "
                        + "This is not an error; Just an indication of a busy space. ");
                found = null;
                needToRetake = true;
            }
        }

        if (needToRetake) {
            // As element ceased to exist during take, I need to try again. The chances of this is rather slim.
            // Nevertheless, this is needed as the query might have zero in timeout.
            return findLeaseForTemplate(templateSet, isToTakeTheLease);

        } else if (found != null) {
            if (isToTakeTheLease) {
                statistics.increaseTake();
            } else {
                statistics.increaseRead();
            }
        } else {
            if (isToTakeTheLease) {
                statistics.increaseMissedTake();
            } else {
                statistics.increaseMissedRead();
            }
        }
        if (found != null) {
            return found;
        }
        return null;
    }

    /**
     * Used for retrieving element with basis in id
     * @return Element with given holder id, or null if not found (or expired
     */
    public ISemiSpaceTuple readHolderById( long hId ) {
        ISemiSpaceTuple result = null;
        result = elements.readHolderWithId(hId);
        return result;
    }
    
    /**
     * <p>If this is a read (not a take), then we must confirm that the agent's name
     * is not already in the entry</p>
     * <p>Note that this routine uses <code>Collections.containsAll</code> which means
     * that the {@link ITuple} in question must contain all of the fields and values
     * found in <code>templateSubSet</code></p>
     * @param containerEntrySet
     * @param templateSubSet
     * @param isToTakeTheLease 
     * @param searchMap TODO
     * @param theTuple TODO
     * @return
     */
    private boolean hasSubSet(Set<Entry<String, Object>> containerEntrySet, Map<String, Object> templateSubSet, boolean isToTakeTheLease, Map<String, Object> searchMap, ISemiSpaceTuple theTuple) {
        if (templateSubSet == null) {
            throw new SemiSpaceUsageException("Did not expect template sub set to be null");
        }
        //HUGE ISSUE: as this code is written, the templates include an ID value
        // which can NEVER be the same as a given tuple.
        templateSubSet.remove(ITupleFields.ID);
        //searchMap is from the tuple itself. In theory, if the tuple is read by an agent,
        //then that agent's name should be in it on the second pass.
//        log.logDebug("HASSUBSET-0 "+searchMap + " ||| "+templateSubSet);
        //NOTE: template has a String individual agentName whereas tuples
        // accumulate agentName in lists
        String agentName = (String)templateSubSet.get(ITupleFields.AGENT_NAME);
        Set<Entry<String, Object>> templateEntrySet = templateSubSet.entrySet();
//        log.logDebug("HASSUBSET-1 "+templateEntrySet+" "+agentName+" "+isToTakeTheLease);
    	boolean result = false;
        if (!isToTakeTheLease && agentName != null) {
        	//has this agent seen this tuple before?
        	Object o = searchMap.get(ITupleFields.AGENT_NAME);
 //       	log.logDebug("HASSUBSET-2 "+o);
        	if (o != null) {
        		if (o instanceof String)
        			result = ((String)o).equals(agentName);
        		else
        			result = ((List)o).contains(agentName);
        	}
        	
        }
//        log.logDebug("HASSUBSET-3 "+result);
        //note that "true" means this agent has seen this tuple before
        //so just leave with a "not found" result
        if (result)
        	return false;
        //The point here is that agentName is not a content field and, if present
        agentName = (String)templateSubSet.remove(ITupleFields.AGENT_NAME);
        //haven't seen it, is it the one?
        result = containerEntrySet.containsAll(templateEntrySet);
        //MUST PUT IT BACK IN THE TEMPLATE since the template is used in iterations
        if (agentName != null)
        	templateSubSet.put(ITupleFields.AGENT_NAME, agentName);
//        log.logDebug("HASSUBSET-4 "+result+" "+containerEntrySet+" "+templateEntrySet);
        if (result) {
        	//We found this tuple, so tell it that "agentName" was here!
        	//THIS IS COMPLEX!
        	// Just adding the agent name back here doesn't solve the problem
        	// since the next time the agent asks, the tuple will be fetched
        	// back from the queue (until it is times out or is taken) and the situation
        	// remains the same.
        	if (theTuple != null)
        		theTuple.addAgentName(agentName);
        }
        return result;
    }


    @Override
    public ISemiSpaceTuple take(ISemiSpaceTuple tmpl, long timeout) {
        ISemiSpaceTuple found = null;
        if (tmpl != null) {
            found = findOrWaitLeaseForTemplate(getPropertiesForObject(tmpl), timeout, true);
//        	log.logDebug( "TAKE-: "+tmpl.getJSON());
        }
        if (found != null)
        	log.logDebug( "TAKE+: "+found);
        return found; //xmlToObject(found);
    }

    @Override
    public ISemiSpaceTuple takeIfExists(ISemiSpaceTuple tmpl) {
        return take(tmpl, 0);
    }
/*
 * Convert some ISemiSpaceTuple to its XML string
    private String objectToXml(Object obj) {
        StringWriter writer = new StringWriter();
        xStream.marshal(obj, new CompactWriter(writer));
        return writer.toString();
    }
*/
    private ISemiSpaceTuple jsonToTuple(String json) {
    	return null; //TODO
    }
    /*
    private Object xmlToObject(String xml) {
        if (xml == null || "".equals(xml)) {
            return null;
        }
        Object result = null;
        try {
            result = xStream.fromXML(xml);
        } catch (Exception e) {
            // Not sure if masking exception is the most correct way of dealing with it.
            log.logError("Got exception unmarshalling. Not throwing the exception up, but rather returning null. "
                    + "This is as the cause may be a change in the object which is sent over. "
                    + "The XML was read as\n" + xml, e);
        }
        return result;
    }
	*/
    private static class PreprocessedTemplate implements ISemiSpaceTuple {
       private ISemiSpaceTuple object; //TODO should be ISemiSpaceTuple
       private String tag = "preprocessedtemplate";
       private Map<String, Object> cachedSet;
       private long id = 5;
       private long liveUntil = Long.MAX_VALUE; // default value
       
       public PreprocessedTemplate(ISemiSpaceTuple object, Map<String, Object> cachedSet) {
 //   	   System.out.println("PREPROCESSED TEMPLATE "+object);
          this.object = object;
          this.cachedSet = cachedSet;
           cachedSet.put("id", new Long(this.id));
       }
       
       public Map<String, Object> getCachedSet() {
          return cachedSet;
       }
       
       public void setCachedSet(Map<String, Object> cachedSet) {
          this.cachedSet = cachedSet;
       }
       
       
       public void setObject(ISemiSpaceTuple object) {
          this.object = object;
       }

		@Override
		public String getTag() {
			return tag;
		}
	
		@Override
		public Map<String, Object> getSearchMap() {
			return cachedSet;
		}
	
		@Override
		public long getLiveUntil() {
			// TODO Auto-generated method stub
			return 0;
		}
	
		@Override
		public void setTag(String newTag) {
			tag = newTag;
			cachedSet.put("tag", newTag);
		}
	
		@Override
		public void set(String name, Object f) {
			// TODO Auto-generated method stub
			
		}
	
		@Override
		public Object get(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getId() {
			return this.id;
		}

		@Override
		public String getJSON() {
			JSONObject jobj = new JSONObject(this.cachedSet);
			return jobj.toString();
		}

		@Override
		public synchronized void setLiveUntil(long liveUntil) {
			this.liveUntil = liveUntil;	
		}

		@Override
		public void setPriority(Long p) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Long getPriority() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setId(Long id) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addAgentName(String agentName) {
			// TODO Auto-generated method stub
			
		}
    }
    /**
     * Create a pre-processed template object that can be used to reduce the amount of
     * work required to match templates during a take.  Applications that take a lot of 
     * objects using the same template instance, a noticeable performance improvement
     * can be had.
     * 
     * @param template The object to preprocess
     * @return A pre-processed object that can be passed to read/take
     */
    public ISemiSpaceTuple processTemplate(ISemiSpaceTuple template) {
       PreprocessedTemplate toReturn = null;
       if (template != null) {
          toReturn = new PreprocessedTemplate(template, template.getSearchMap());
       }
       return toReturn;
    }

    private Map<String, Object> getPropertiesForObject(ISemiSpaceTuple object) {
       if (object instanceof PreprocessedTemplate) {
          return ((PreprocessedTemplate)object).getCachedSet();
       }
       return object.getSearchMap();
    }

    /**
     * Protected for the benefit of junit test(s)
     * 
     * @param examine Non-null object
     * /
    protected Map<String, Object> retrievePropertiesFromObject(ISemiSpaceTuple examine) {
        Map<String, Object> map = examine.getSearchMap();
        		fillMapWithPublicFields(examine);
        addGettersToMap(examine, map);

//        if ( examine instanceof InternalQuery ) {
//            map.put(SemiSpace.ADMIN_GROUP_IS_FLAGGED, "true");
//        }
        // Need to rename class entry in order to separate on class elements.
        String tag = (String)map.remove("tag"); //TODO ????
        map.put("tag", tag );
        return map;
    }

    /**
     * Add an objects getter names and values in a map. Note that all values are converted to strings.
     */
    private void addGettersToMap(ISemiSpaceTuple examine, Map<String, Object> map) {
        final Set<String> getters = new HashSet<String>();
        final Method[] methods = examine.getClass().getMethods();
        final Map<String, Method> keyedMethod = new HashMap<String, Method>();
        final Map<String, String> keyedMethodName = new HashMap<String, String>();
        for ( Method method : methods ) {
            final String name = method.getName();
            final int parameterLength = method.getTypeParameters().length;
            if ( parameterLength == 0 && name.startsWith("get")) {
                // Equalize key to [get][set][X]xx
                String normalized = name.substring(3,4).toLowerCase() + name.substring(4);
                getters.add(normalized);
                keyedMethod.put( name, method );
                keyedMethodName.put( normalized, name );
                //log.info("Got name "+name+" which was normalized to "+normalized);
            }
        }
        for ( String name : getters) {
            try {
                Object value = keyedMethod.get(keyedMethodName.get(name)).invoke(examine, null);
                //log.info(">> want to insert "+name+"="+value);
                if (value != null) {
                    map.put(name, "" + value);
                }
            } catch (IllegalAccessException e) {
                log.logError("Could not access method g"+name+". Got (masked exception) "+e.getMessage(),e);
            } catch (InvocationTargetException e) {
                log.logError("Could not access method g"+name+". Got (masked exception) "+e.getMessage(),e);
            }
        }
    }

    /**
     * Create a map and fill it with the public fields from the object, which
     * is the JavaSpace manner.
     */
    private Map<String, Object> fillMapWithPublicFields(ISemiSpaceTuple examine) {
        Field[] fields = classFieldMap.get(examine.getTag());
        if ( fields == null ) {
            fields = examine.getClass().getFields();
            classFieldMap.put(examine.getClass().getName(), fields);
        }
        Map<String, Object> map = new HashMap<String, Object>();
        for (Field field : fields) {
            try {
                String name = field.getName();
                Object value = field.get(examine);

                if (value != null) {
                    map.put(name, "" + value);
                }
            } catch (IllegalAccessException e) {
                log.logError("Introspection gave exception - which is not re-thrown.", e);
            }
        }
        return map;
    }

    /**
     * Preparing for future injection of admin. Note that you
     * must call initialization <b>yourself</b> after setting the
     * object
     * <p>
     * This is tested in junit test (and under terracotta).
     * </p>
     */
    public void setAdmin(ISemiSpaceAdmin admin) {
        this.admin = admin;
    }

    /**
     * Return admin element
     */
    public ISemiSpaceAdmin getAdmin() {
        return this.admin;
    }

    /**
     * Need to wrap write in own thread in order to make terracotta pick it up.
     */
    protected class WrappedInternalWriter implements Runnable {
        private ISemiSpaceTuple entry;

        private long leaseTimeMs;

        private Exception exception;

        private SemiLease lease;

        public Exception getException() {
            return this.exception;
        }

        public SemiLease getLease() {
            return lease;
        }

        protected WrappedInternalWriter(ISemiSpaceTuple entry, long leaseTimeMs) {
            this.entry = entry;
            this.leaseTimeMs = leaseTimeMs;
        }

        @Override
        @SuppressWarnings("synthetic-access")
        public void run() {
 //       	System.out.println("WrappedInternalWriter running "+entry);
            try {
                lease = writeInternally(entry, leaseTimeMs);
            } catch (Exception e) {
                log.logError("Got exception writing object.", e);
                exception = e;
            }
        }
    }

    /**
     * Harvest old elements from diverse listeners. Used from 
     * the periodic harvester and junit tests.
     */
    public void harvest() {

            for (ListenerHolder listener : listeners.values()) {
                if (listener.getLiveUntil() < admin.calculateTime()) {
                    cancelListener(listener);

                }
            }
        List<ISemiSpaceTuple> beforeEvict = new ArrayList<ISemiSpaceTuple>();

        String[] groups = elements.retrieveGroupNames();
        for ( String group : groups ) {
            int evictSize = beforeEvict.size();
            TupleCollectionById hc = elements.next(group);
            for (ISemiSpaceTuple elem : hc) {
                if (elem.getLiveUntil() < admin.calculateTime()) {
                    beforeEvict.add(elem);
                }
            }
            long afterSize = beforeEvict.size() - evictSize ;
            if ( afterSize > 0 ) {
                List<Long>ids = new ArrayList<Long>();
                for (ISemiSpaceTuple evict : beforeEvict) {
                    ids.add(Long.valueOf( evict.getId()) );
                }
                String moreInfo = "";
                if ( ids.size() < 30 ) {
                    Collections.sort(ids);
                    moreInfo = "Ids: "+ids;
                }
 //               log.logDebug("Testing group "+group+" gave "+afterSize+" element(s) to evict. "+moreInfo);
            }
        }
        for (ISemiSpaceTuple evict : beforeEvict) {
            cancelElement(Long.valueOf(evict.getId()), false, evict.getTag());
        }
    }

    /**
     * Return the number of elements in the space. Notice that this may report old elements that have not been purged
     * yet.
     */
    public int numberOfSpaceElements() {
        int size;
        size = elements.size();
        return size;
    }

    /** Need present statistics here due to spring JMX configuration. */
    public int numberOfBlockingRead() {
        return statistics.getBlockingRead();
    }

    /** Need present statistics here due to spring JMX configuration. */
    public int numberOfBlockingTake() {
        return statistics.getBlockingTake();
    }

    /** Need present statistics here due to spring JMX configuration. */
    public int numberOfMissedRead() {
        return statistics.getMissedRead();
    }

    /** Need present statistics here due to spring JMX configuration. */
    public int numberOfMissedTake() {
        return statistics.getMissedTake();
    }

    /** Need present statistics here due to spring JMX configuration. */
    public int numberOfNumberOfListeners() {
        return statistics.getNumberOfListeners();
    }

    /** Need present statistics here due to spring JMX configuration. */
    public int numberOfRead() {
        return statistics.getRead();
    }

    /** Need present statistics here due to spring JMX configuration. */
    public int numberOfTake() {
        return statistics.getTake();
    }

    /** Need present statistics here due to spring JMX configuration. */
    public int numberOfWrite() {
        return statistics.getWrite();
    }

    /**
     * For the benefit of junit test(s) - defensively copied statistics
     */
    protected SemiSpaceStatistics getStatistics() {
        SemiSpaceStatistics stats;
        // Defensive copied statistics
        stats = (SemiSpaceStatistics) null; // TODO xmlToObject(objectToXml(statistics));
        return stats;
    }

    
    protected boolean cancelListener(ListenerHolder holder) {
        boolean success = false;

        ListenerHolder listener = listeners.remove(Long.valueOf(holder.getId()));
        if (listener != null) {
            statistics.decreaseNumberOfListeners();
            success = true;
        }

        return success;
    }

    protected boolean renewListener(ListenerHolder holder, long duration) {
        boolean success = false;

        ListenerHolder listener = listeners.get(Long.valueOf(holder.getId()));
        if (listener != null) {
            listener.setLiveUntil(duration + admin.calculateTime());
            // Need to re-get due in order to be certain of liveness.
            success = listeners.get(Long.valueOf(holder.getId())) != null;
        }
        return success;
    }

    /**
     * @param isTake true if reason for the cancellation is a take.
     */
    protected boolean cancelElement(Long id, boolean isTake, String className) {
        boolean success = false;

        ISemiSpaceTuple elem = elements.removeHolderById(id.longValue(), className);
        if (elem != null) {
            if ( elem.getId() != id.longValue()) {
                throw new SemiSpaceInternalException("Sanity problem. Removed "+id.longValue()+" and got back element with id "+elem.getId());
            }
            success = true;
            SemiEvent semiEvent = null;
            if (isTake) {
                semiEvent = new SemiTakenEvent(elem.getId());
            } else {
                semiEvent = new SemiExpirationEvent(elem.getId());
            }
            //log.logDebug("Notifying about "+(isTake?"take":"expiration")+" of element with id "+semiEvent.getId());
            distributeEvent(new DistributedEvent(elem.getTag(), semiEvent,
                    elem.getSearchMap())); 

        }

        return success;
    }

    /**
     * @return true if the object actually was renewed. (I.e. it exists and got a new timeout.)
     */
    protected boolean renewElement(ISemiSpaceTuple holder, long duration) {
        boolean success = false;
        ISemiSpaceTuple elem = elements.findById(holder.getId(), holder.getTag());
        if (elem != null) {
            elem.setLiveUntil(duration + admin.calculateTime());
            success = true;
            distributeEvent(new DistributedEvent(elem.getTag(), new SemiRenewalEvent(
                    elem.getId(), elem.getLiveUntil()), elem.getSearchMap()));
        }

        return success;
    }
    
    /**
     * @see TupleCollection#findAllHolderIds
     */
    public Long[] findAllHolderIds() {
        Long[] result = null;
        result = elements.findAllHolderIds();
        return result;
    }

    /**
     * Exposing xstream instance in order to allow outside manipulation of aliases and classloader affiliation.
     * @return The xstream instance used.
     */
 //   public XStream getXStream() {
 //       return xStream;
 //   }

    private static class ShortestTtlComparator implements Comparator<ListenerHolder>, Serializable {
        @Override
        public int compare(ListenerHolder o1, ListenerHolder o2) {
            if ( o1 == null || o2 == null ) {
                throw new SemiSpaceUsageException("Did not expect any null values for listenerHolder.");
            }
            return (int) (o1.getLiveUntil() - o2.getLiveUntil());
        }
    }
}
