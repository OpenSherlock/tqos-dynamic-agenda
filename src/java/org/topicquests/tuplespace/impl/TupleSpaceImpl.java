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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

import org.topicquests.tuplespace.api.ITuple;
import org.topicquests.tuplespace.api.ITupleSpace;
//import org.topicquests.tuplespace.api.ITupleSpaceListener;

/* to compile for JDK 1.x, uncomment and link to Collections 1.1 lib
import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.Collections;
import com.sun.java.util.collections.HashMap;
import com.sun.java.util.collections.ListIterator;
*/


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

	/**
	* A Map for holding Lists of Tuples.
	*/
	private Map tupleMap = new HashMap();

	/**
	* A Map for holding Lists of Templates waiting to be matched.
	**/
	private Map templateMap = new HashMap();

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
	}

    /**
     * @return list of Tuple IDs
     */
    public Iterator tuples() {
      if (tupleMap==null) return null;
      Set keys = tupleMap.keySet();
      return keys.iterator();
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


		store(insertTup, tupleMap);
System.out.println("INSERT starting to check for match");

		// search the template map. If a match is found, notify() the
		// waiting thread(s).
		Object thash = insertTup.hash();
		synchronized(this.templateMap) {
                    // the meaning of thash in search is the same as "any" or
                    // all possible template groups
			if (templateMap.containsKey(thash)) {
				List templateList = (List)templateMap.get(thash);
				ListIterator templates = templateList.listIterator();
				ITuple curTemplate;
				while (templates.hasNext()) {
					curTemplate = (ITuple)templates.next();
      System.out.println("Insert matching "+curTemplate.toString());
					if (insertTup.matches(curTemplate)) {
      System.out.println("Insert found match");
						synchronized (curTemplate) {
                          // tell curTemplate (antiTuple)
                          // it found a match
                            insertTup.setRequestId(curTemplate.getRequestId());
                            curTemplate.setMatch(insertTup);
                            curTemplate.notify();
						}
					}
				}
			} else if (thash.equals("*")) {
                          // must check all possible templates
       System.out.println("Insert matching against all templates");
                        }

		}

	}

	public ITuple extract(final ITuple antiTup, long blockFor) {

		// make a defensive copy of the template ITuple
		ITuple template = antiTup.copy();

		ITuple match = null;
		boolean firstTry = true;
		synchronized (template) {
			while ((match = getMatch(template, true)) == null) {
				if (firstTry) {
					store(template, templateMap);
					firstTry = false;
				}
				try {
					if (blockFor == Long.MAX_VALUE) {
						template.wait(); 	// wait as long as required...
					}
					else {
						template.wait(blockFor);
						return getMatch(template, true); // we only get one try
					}
				} catch (Exception e) { }
			}
		}
		return match;
	}
	/**
	 * Extract a Tuple matching the template.
	 * This method "subscribes" a TupleSpaceListener
	 */
	public ITuple take(final ITuple antiTup, long listener) {
		// make a defensive copy of the template tuple
		ITuple template = antiTup.copy();

		ITuple match = null;
		boolean firstTry = true;
		synchronized (template) {
			while ((match = getMatch(template, true)) == null) {
				if (firstTry) {
					store(template, templateMap);
					firstTry = false;
				}
				try {
						template.wait(); 	// wait as long as required...
				} catch (Exception e) { }
			}
		}
		return match;
	}

	public ITuple read(final ITuple antiTup, long blockFor) {

		// make a defensive copy of the template ITuple
		ITuple template = antiTup.copy();

		ITuple match = null;
		boolean firstTry = true;
		synchronized (template) {
			while ((match = getMatch(template, false)) == null) {
				if (firstTry) {
					store(template, templateMap);
					firstTry = false;
				}
				try {
					if (blockFor == Long.MAX_VALUE) {
						template.wait(); 	// wait as long as required...
					}
					else {
						template.wait(blockFor);
						return getMatch(template, false); // we only get one try
					}
				} catch (Exception e) { }
			}
		}
		return match;
	}

    /**
     * @param template ITuple
     * @return List of matching Tuples or empty list
     */
    public List<ITuple> collect(ITuple antiTup) {
	// make a defensive copy of the template ITuple
	ITuple template = antiTup.copy();
            return getMatches(template);
    }

    /**
     * @param antiTuple to match
     * @return ITuple or null
     */
	public ITuple noWaitRead(final ITuple antiTup) {

		// make a defensive copy of the template ITuple
		ITuple template = antiTup.copy();
System.out.println("READING "+antiTup.toString());
		return getMatch(template, false);
	}



	/************************* Private methods *************************/

	private void store(ITuple tup, Map store) {

		/*
		Use the tuple type hash to access the List
		to contain this tuple. If the appropriate List
		doesn't exist, create it.
		*/
		Object thash = tup.hash();
		List tupleList;
		synchronized (store) {
			if (store.containsKey(thash)) {
				tupleList = (List)store.get(thash);
				tupleList.add(tup);
			}
			else {
				tupleList = new ArrayList();
				tupleList.add(tup);
				store.put(thash, tupleList);
			}
		}
	}

	private ITuple getMatch(ITuple template, boolean destroy) {

		Object thash = template.hash();
		synchronized(this.tupleMap) {
			if (tupleMap.containsKey(thash)) {
				List tupleList = (List)tupleMap.get(thash);
      System.out.println("MATCHING WITH "+tupleList.size());
				ListIterator tuples = tupleList.listIterator();
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
			// no such tuple exists
			return null;
		}
	}
	private List getMatches(ITuple template) {
          List result = new ArrayList();
		Object thash = template.hash();
		synchronized(this.tupleMap) {
			if (tupleMap.containsKey(thash)) {
				List tupleList = (List)tupleMap.get(thash);
      System.out.println("MATCHING WITH "+tupleList.size());
				ListIterator tuples = tupleList.listIterator();
				ITuple curTuple = null;
				while (tuples.hasNext()) {
					curTuple = (ITuple)tuples.next();
        System.out.println("MATCHING "+curTuple.toString());
        System.out.println("MATCHING To "+template.toString());
					if (curTuple.matches(template)) {
        System.out.println("MATCHING GOT MATCH");
						curTuple.setRequestId(template.getRequestId());
                                                result.add(curTuple.copy());
					}
				}
			}
		}
          return result;
	}

}