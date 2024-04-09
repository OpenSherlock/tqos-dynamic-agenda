package org.topicquests.tuplespace.impl;

/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: TupleSpaceImpl.java,v 1.2 2001/07/07 15:44:25 vwilliams Exp $
 *  @author Jack Park -- modifications
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

import org.topicquests.tuplespace.api.ITemplate;
import org.topicquests.tuplespace.api.ITuple;
import org.topicquests.tuplespace.api.ITupleSpace;
import java.util.SortedSet;
import java.util.TreeSet;

/**
* This class implements a TupleSpace. This is the default
* implementation used by TupleSpaceFactory to create a space.
*
* TO DO:
*	- override equals(), toString(), and hashCode()
*
* @author Vanessa Williams
* @version $Revision: 1.2 $, $Date: 2001/07/07 15:44:25 $
*
*/
/**
 * @author Jack Park
 *
 *	Major revision to handle two types of spaces:
 *		classical
 *		prioritized (sorted by priority)
 *	Major revision to handle IConstraint (First Order Logic) matching
 *
 *	note: prioritized space uses only Tuple.tag for matching
 */
public final class TupleSpaceImpl implements ITupleSpace {

	/**
	* The space name. Required to look up a reference to a particular space.
	*/
	private String spaceName;

	
	private SortedSet<ITuple> myTuples;

    /**
     * Shutdown for all threads
     */
    boolean isRunning = true;

	/**
	* Constructor. Normally never called directly.
	* Use TupleSpaceFactory.getSpace() instead.
	*/
	public TupleSpaceImpl(String name) {
		this.spaceName = name;
		this.myTuples = new TreeSet<ITuple>();
	}

    /**
     * @return list of Tuple IDs
     */
    public Iterator tuples() {
      if (myTuples==null) return null;
      return myTuples.iterator();
    }

    /**
     * @param Tuple to insert into this TupleSpace
     * Tuple's tag (group) could be a "*" which would then
     * become a valid group "*" -- a don't care
     * Note: "*" is the default tag (group) for a Tuple
     *
     * Tuples now have a Timestamp
     * They should be sorted latest last
     * Actually, they are added to the end of the list so they are
     * already sorted!
     * Read/Take should take from the front of the List
     *
     */
	public void insert(final ITuple tup) {
System.out.println("INSERTING "+tup.toString());
		// make a defensive copy of the template tuple
		ITuple insertTup = tup.copy();


System.out.println("INSERT starting to check for match");

		synchronized(this.myTuples) {
			myTuples.add(tup);
			myTuples.notify();
		}
	}


	/**
	 * Extract a Tuple matching the template.
	 * This method "subscribes" a TupleSpaceListener
	 */
	public ITuple take(final ITemplate template, long t) {
		// make a defensive copy of the template tuple
		//ITuple template = antiTup.copy();

		ITuple match = null;
		boolean firstTry = true;
		synchronized (myTuples) {
			while ((match = getMatch(template, true)) == null) {
				if (firstTry) {
					firstTry = false;
					try {
						template.wait(t); 	// wait as long as required...
					} catch (Exception e) { }
				} else
					return match;
			}
		}
		return match;
	}

	public ITuple read(final ITemplate template, long t) {

		// make a defensive copy of the template ITuple
		//ITuple template = antiTup.copy();

		ITuple match = null;
		boolean firstTry = true;
		synchronized (template) {
			while ((match = getMatch(template, false)) == null) {
				if (firstTry) {
					//store(template, templateMap);
					firstTry = false;
				}
				try {
						template.wait(t);
						return getMatch(template, false); // we only get one try
				} catch (Exception e) { }
			}
		}
		return match;
	}

    /**
     * @param template ITuple
     * @return List of matching Tuples or empty list
     */
    public List<ITuple> collect(final ITemplate template) {
	// make a defensive copy of the template ITuple
	//ITuple template = antiTup.copy();
           return getMatches(template);
    }

    /**
     * @param antiTuple to match
     * @return ITuple or null
     */
	public ITuple noWaitRead(final ITemplate template) {

		// make a defensive copy of the template ITuple
		//ITuple template = antiTup.copy();
		return getMatch(template, false);
	}



	/************************* Private methods *************************/


	private ITuple getMatch(ITemplate template, boolean destroy) {

		synchronized(myTuples) {
			Iterator tuples = myTuples.iterator();
			ITuple curTuple = null;
			while (tuples.hasNext()) {
				curTuple = (ITuple)tuples.next();
    System.out.println("MATCHING "+curTuple.toString());
    System.out.println("MATCHING To "+template.toString());
				if (curTuple.matches(template)) {
    System.out.println("MATCHING GOT MATCH");
					if (destroy)
						tuples.remove(); // extract the tuple if appropriate
					curTuple.setRequestId(template.getRequestId());
                    return curTuple.copy();
				}
			}
			// no match found
    System.out.println("MATCHING NO MATCH");
			return null;
		}
	}
	private List<ITuple> getMatches(final ITemplate template) {
          List<ITuple> result = new ArrayList<ITuple>();
		synchronized(myTuples) {
			Iterator<ITuple> tuples = myTuples.iterator();
			ITuple curTuple = null;
			while (tuples.hasNext()) {
				curTuple = tuples.next();
    System.out.println("MATCHING "+curTuple.toString());
    System.out.println("MATCHING To "+template.toString());
				if (curTuple.matches(template)) {
    System.out.println("MATCHING GOT MATCH");
					curTuple.setRequestId(template.getRequestId());
                    result.add(curTuple.copy());
				}
			}	
		}
        return result;
	}

}