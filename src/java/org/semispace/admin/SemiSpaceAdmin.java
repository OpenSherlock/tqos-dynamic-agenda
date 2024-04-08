/*
 * ============================================================================
 *
 *  File:     SemiSpaceAdmin.java
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
 *  Created:      16. feb.. 2008
 * ============================================================================ 
 */

package org.semispace.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.semispace.DistributedEvent;
import org.semispace.Tuple;
import org.semispace.SemiSpace;
import org.semispace.api.ISemiSpace;
import org.semispace.api.ISemiSpaceAdmin;
import org.semispace.api.ISemiSpaceTuple;
import org.semispace.event.SemiAvailabilityEvent;
import org.topicquests.util.LoggingPlatform;

public class SemiSpaceAdmin implements ISemiSpaceAdmin {
	private LoggingPlatform log = LoggingPlatform.getLiveInstance();

    private boolean master;

    private ISemiSpace space;

    private boolean beenInitialized;

    private long clockSkew;

    private int spaceId;

    private ExecutorService pool;

    private Thread shutDownHook;
    
    private PeriodicHarvest periodicHarvest;

    public SemiSpaceAdmin(ISemiSpace terraSpace) {
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(0, 5000,
                5L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(true));
        tpe.setThreadFactory(new DaemonDelegateFactory(tpe.getThreadFactory()));
        // Exchanging strategy. When thread pool is full, try to run on local thread.
        tpe.setRejectedExecutionHandler(new SemiSpaceRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()));
        tpe.allowCoreThreadTimeOut(true);
        this.pool = tpe;
        this.space = terraSpace;
        this.beenInitialized = false;
        this.clockSkew = 0;
        this.spaceId = 0;
        this.master = false;
        this.periodicHarvest = new PeriodicHarvest(this);
    }

    /**
     * Used from junit test.
     */
    protected int getSpaceId() {
        return spaceId;
    }
    
    /**
     * @return space configured for this admin. Beneficiary for subclasses. 
     */
    protected ISemiSpace getSpace() {
        return space;
    }

    @Override
    public ExecutorService getThreadPool() {
        return pool;
    }

    @Override
    public boolean hasBeenInitialized() {
        return this.beenInitialized;
    }

    @Override
    public boolean isMaster() {
        return this.master;
    }

    @Override
    public long calculateTime() {
        return System.currentTimeMillis() - clockSkew;
    }

    @Override
    public void performInitialization() {
        if (beenInitialized) {
            log.logDebug("Initialization called more than once.");
            return;
        }
        beenInitialized = true;
        
        Runnable hook = new Runnable() {
            @Override
            @SuppressWarnings("synthetic-access")
            public void run() {
                log.logDebug("Shutdown hook shutting down semispace.");
                shutdownAndAwaitTermination();
            }
        };
        shutDownHook = new Thread( hook);
        Runtime.getRuntime().addShutdownHook(shutDownHook);

        //
        // Fire up connection
        //
        long last;
        long current = SemiSpace.ONE_DAY;
        int count = 0;
        // Perform query as long as the connection is improving
        do {
            count++;
            last = current;
            current = fireUpConnection();
        } while (current < last);
        log.logDebug("Needed " + count + " iterations in order to find the best time, which was " + current + " ms.");

        //
        // Figure out the ID of this space
        //
        spaceId = figureOutSpaceId();
        log.logDebug("Space id was found to be " + spaceId);

        //
        // (Try to) find clock skew
        queryForMasterTime();
        // log.logDebug( "Calculate time, which should give an approximation of the master time, reports ["+new
        // Date(calculateTime())+"]");
        
        periodicHarvest = new PeriodicHarvest(this);
        periodicHarvest.startReaper();
    }

    private int figureOutSpaceId() {
        List<Tuple> admins = new ArrayList<Tuple>();

        Tuple masterFound = populateListOfAllSpaces(admins);

        Collections.sort(admins, new HolderComparator());
        int foundId = 1;
        if (!admins.isEmpty()) {
            // Collection is sorted, and therefore the admin should increase
            Tuple admin = admins.get(0);
            if (admin.idx != null) {
                foundId = admin.idx.intValue() + 1;
            }
        }
        if (masterFound == null) {
            log.logDebug("I am master, as no other master was identified.");
            assumeAdminResponsibility(! admins.isEmpty());
        }
        return foundId;
    }

    protected void assumeAdminResponsibility(boolean sendAdminInfoAboutSystemTime) {
        master = true;
        if (sendAdminInfoAboutSystemTime) {
            log.logDebug("Informing other masters of system time.");
            Tuple ta = new Tuple(2,"timeanswer");
            ta.masterId = getSpaceId();
            ta.timeFromMaster = Long.valueOf(System.currentTimeMillis());
            space.write(ta, 1000);
        }
    }

    /**
     * Protected as it is used every once in a while from periodic object reaper
     * @param admins List to fill with the admin processes found
     * @return List of identified SemiSpace admin classes
     */
    protected Tuple populateListOfAllSpaces(List<Tuple> admins) {
        Tuple identifyAdmin = new Tuple(3,"identifyadminquery");
        identifyAdmin.hasAnswered = Boolean.FALSE;
        space.write(identifyAdmin, SemiSpace.ONE_DAY);

        Tuple iaq = new Tuple(3,"identifyadminquery");
        iaq.hasAnswered = Boolean.TRUE;

        Tuple masterFound = null;
        Tuple answer = null;
        long waitFor = 750;
        do {
            answer = (Tuple)space.take(iaq, waitFor);
            // When the first answer has arrived, the others, if any, should come close behind.
            waitFor = 250;
            if (answer != null) {
                admins.add(answer);
                if (Boolean.TRUE.equals(answer.amIAdmin)) {
                    if (masterFound != null) {
                        log.logError("More than one admin found, both " + masterFound.idx + " and " + answer.idx, null);
                    }
                    masterFound = answer;
                }
            }
            // Looping until we do not find any more admins
        } while (answer != null);

        while ( space.takeIfExists(new Tuple(3,"identifyadminquery")) != null) { // NOSONAR
            // Remove identity query from space as we do not need it anymore. If more than one present, we have a race condition (not likely)
        }

        return masterFound;
    }

    /**
     * The very first query may take some time (when using terracotta), and it is therefore prudent to kick start the
     * connection.
     * 
     * @return Time it took in ms for an answer to be obtained.
     */
    private long fireUpConnection() {
        long bench = System.currentTimeMillis();
        Tuple nvq = new Tuple(1,"namevaluequery");
        nvq.name = "Internal admin query";
        nvq.value = "Dummy-value in order to be (quite) unique [" + bench + "]";
        space.write(nvq, SemiSpace.ONE_DAY);
        nvq = (Tuple)space.take(nvq, 1000);
        if (nvq == null) {
            throw new AssertionError("Unable to retrieve query which is designed to kickstart space.");
        }
        long timed = System.currentTimeMillis() - bench;
        return timed;
    }

    /**
     * Obtaining time by querying with internal query
     */
    private void queryForMasterTime() {
        Tuple tq = new Tuple(2,"timequery");
        tq.isFinished = Boolean.FALSE;
        // Letting the query itself exist a day. This is as skew can be large.
        space.write(tq, SemiSpace.ONE_DAY);

        space.read(new Tuple(2,"timeanswer"), 2500);
        space.takeIfExists(tq);
    }

    /**
     *
     */
    private void notifyAboutInternalQuery(InternalQuery incoming) {
        // log.logDebug("Incoming admin query for space "+getSpaceId()+" of type "+incoming.getClass().getName());
        if (incoming instanceof Tuple) {
            answerTimeQuery((Tuple) incoming);

        } else if (incoming instanceof Tuple) {
            answerIdentityQuery((Tuple) incoming);

        } else if (incoming instanceof Tuple) {
            treatIncomingTimeAnswer((Tuple) incoming);

        } else {
            log.logDebug("Unknown internal query");
        }
    }

    /**
     * A (potentially new) admin process has given time answer. Adjust time accordingly
     */
    private void treatIncomingTimeAnswer(Tuple incoming) {
        if (isMaster()) {
            if (incoming.masterId != getSpaceId()) {
                String adminfo = "Got more than one space that perceives it is admin space: " + incoming.masterId
                        + " and myself: " + getSpaceId();
                if (incoming.masterId < getSpaceId()) {
                    master = false;
                    adminfo += ". Removing this space as master.";
                } else {
                    adminfo += ". Keeping this space as master.";
                }
                log.logDebug(adminfo);
            } else {
                clockSkew = 0;
            }

        }

        // Need to test again as we may have been reset:
        if (!isMaster()) {
            long systime = System.currentTimeMillis();
            clockSkew = systime - incoming.timeFromMaster.longValue();
            log.logDebug("Master has " + " [" + new Date(incoming.timeFromMaster.longValue()) + "]" + ", whereas I have ["
                    + new Date(systime) + "]. This gives a skew of " + clockSkew + ".");
        }

    }

    private void answerIdentityQuery(Tuple identify) {
        if (spaceId < 1) {
            return;
        }
        if (identify.hasAnswered != null && identify.hasAnswered.booleanValue()) {
            return;
        }
        Tuple answer = new Tuple(3,"identifyadminquery");
        answer.amIAdmin = Boolean.valueOf(master);
        answer.hasAnswered = Boolean.TRUE;
        answer.idx = Integer.valueOf(spaceId);
        log.logDebug("Giving identity answer for space " + spaceId + ", which is" + (master ? "" : " NOT") + " master.");
        space.write(answer, SemiSpace.ONE_DAY);
    }

    private void answerTimeQuery(Tuple tq) {
        if (isMaster() && !tq.isFinished.booleanValue()) {
            Tuple answer = new Tuple(2,"timeanswer");
            answer.timeFromMaster = Long.valueOf(System.currentTimeMillis());
            answer.masterId = getSpaceId();
            space.write(answer, 1000);
            log.logDebug("Giving answer about time (which was found to be " + answer.timeFromMaster + ", which is "
                    + new Date(answer.timeFromMaster.longValue()) + ")");
        }
    }

    @Override
    public void notifyAboutEvent(DistributedEvent event) {
        if (event.getEvent() instanceof SemiAvailabilityEvent) {
            if (InternalQuery.class.getName().equals(event.getHolderClassName()) && space instanceof SemiSpace ) {
                ISemiSpaceTuple holder = ((SemiSpace)space).readHolderById(event.getEvent().getId());
                if ( holder != null ) {
                    notifyAboutInternalQuery((InternalQuery) holder);
                }
            }
        }
    }

    /**
     * The cached thread pool has a timeout of a minute, so a shutdown is not immediate. This method will try to speed
     * up the process, but it is not mandatory to use it.
     * The method is protected for the benefit of subclasses.
     */
    protected void shutdownAndAwaitTermination() {
        if ( pool.isShutdown() && periodicHarvest.isCancelled()) {
            // Already had a shutdown notification.
            return;
        }
        periodicHarvest.cancelReaper();
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.logDebug("Pool did not terminate");
                }
            }
        } catch (InterruptedException ignored) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Remove shutdown hook which otherwise is run when the space is shut down.
     * Primarily used when exchanging this admin with another.
     */
    public void removeShutDownHook() {
        periodicHarvest.cancelReaper();
        if ( shutDownHook != null ) {
            Runtime.getRuntime().removeShutdownHook(shutDownHook);            
        }
    }

    private static class HolderComparator implements Comparator<Tuple>, Serializable {
        @Override
        public int compare(Tuple a1, Tuple a2) {
            if (a1.idx == null) {
                return 1;
            } else if (a2.idx == null) {
                return -1;
            }
            return a2.idx.intValue() - a1.idx.intValue();
        }
    }
}
