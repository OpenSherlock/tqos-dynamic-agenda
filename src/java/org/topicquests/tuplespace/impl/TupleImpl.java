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
 * $Id: TupleImpl.java,v 1.3 2001/07/08 16:16:08 vwilliams Exp $
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.topicquests.tuplespace.api.IConstants;
import org.topicquests.tuplespace.api.ILogicElement;
import org.topicquests.tuplespace.api.ITemplate;
import org.topicquests.tuplespace.api.ITuple;

import java.sql.Timestamp;

/* to compile for JDK 1.1 uncomment and link to Collections 1.1 lib
import com.sun.java.util.collections.HashMap;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Set;
*/


/**
* Default, minimalist implementation of a Tuple. Uses a List to
* store the tuple's fields.
*
* TO DO:
*	- improve hash() so we can be sure we aren't comparing apples to oranges
*	- override equals(), hashCode() and toString()
*
* @author Vanessa Williams
* @version $Revision: 1.3 $, $Date: 2001/07/08 16:16:08 $
*
*/
/**
 * @author Jack Park
 *	added priority
 */

public class TupleImpl implements ITuple, IConstants {

	/**
	 * priority support
	 */
	private int priority = -1;

    /**
     * Transaction support
     */
    private String requestId = "";
	/**
	 * IConstraint support
	 */
//	private IConstraint matchConstraint = null;
        /**
         * Tag is used for the <group> xml value
         * This allows grouping Tuples
         * Default value is wildcard
         */
	private String tag = "*";

    /**
     * Identity
     */
    private String id = null;

    /**
     * Internal store for all fields
     * except tag (group) and priority
     */
	private Map<String, Object> tupleFields = null;

	private int fieldCount = 0;

    protected boolean allowPartialMatch = false;

    public Timestamp created = null;

    /**
     * Speedup: let TupleSpace.insert() tell this Tuple (acting as a template)
     * which Tuple matched it
     */
    private ITuple matchTuple = null;
    /**
     * Features added to let Tuple know it's command -- put, take, read
     */
    private String command = null;
    public String getCommand() {
      return command;
    }
    public void setCommand(String cmd) {
      command = cmd;
    }
    /**
     * Features added to let Tuple know it's TupleSpace name
     */
    private String mySpace = null;

    public String getSpace() {
      return mySpace;
    }
    public void setSpace(String space) {
      mySpace = space;
    }
    /**
     * Identity for each Tuple
     */
    public void setID(String id) {
      this.id = id;
    }
    public String getID() {
      return this.id;
    }
    /**
     * Timestamp support
     */
    public Timestamp getCreated() {
      return this.created;
    }
    public void setCreated(Timestamp ts) {
      this.created = ts;
    }
    /**
     * Transaction support <requestId>
     */
    public String getRequestId() {
      return this.requestId;
    }
    public void setRequestId(String id) {
      this.requestId = id;
    }

    /**
     * Allow for partial matching
     * If true, only the fields in the AntiTuple need match
     * no matter how many other fields are present
     */
    public void setAllowPartialMatch(boolean tf) {
      allowPartialMatch = tf;
    }
    public boolean getAllowPartialMatch() {
      return allowPartialMatch;
    }

	/**
	* This constructor is normally not called by applications.
	* Use TupleFactory.create() instead.
	*/
	public TupleImpl(String tag) {
		this.tag = tag;
		this.tupleFields = new ConcurrentHashMap<String, Object>();
	}

	public TupleImpl(String tag, Map<String, Object> fields) {
		this.tag = tag;
		this.tupleFields = fields;
	}

	public ITuple copy() {

		TupleImpl newTup = new TupleImpl(this.tag);
		newTup.tupleFields = new HashMap(this.tupleFields);
		newTup.fieldCount = newTup.tupleFields.size();
		newTup.priority = this.priority;
                newTup.requestId = this.requestId;
                newTup.tag = this.tag;
                newTup.id = this.id;
                newTup.mySpace = this.mySpace;
                newTup.allowPartialMatch = this.allowPartialMatch;
		return (ITuple)newTup;
	}

	public String getTag() {
		return this.tag;
	}

	public void set(String name, Object f) {
		synchronized(tupleFields) {
			tupleFields.put(name, f);
		}
		this.fieldCount++;
	}

	public Object get(String name) {
		return this.tupleFields.get(name);
	}

	/**
	* Templates and tuples of the same type
	*  must return the same hash value.
	*/
	public Object hash() {
		// brain-dead, totally non-optimized hash
		// This definitely needs replacing.
		return this.getTag();
	}

	public int numFields() {
		return this.fieldCount;
	}

	public Set<String> fieldNames() {
		return this.tupleFields.keySet();
	}

	public boolean matches(final ITemplate template){
		// if this is a constraint-based match, return that
        boolean isPartialMatch = template.getAllowPartialMatch();
		ILogicElement tupleConstraint = template.getConstraint();
		if (tupleConstraint != null) {
			return tupleConstraint.eval(this);
		}
System.out.println("Tuple Match 1 "+isPartialMatch);
                String g = template.getTag();
                // if either is a wild card, then keep going
           //     if (!(g.equals("*") || this.getTag().equals("*")))
	//	  if (g != this.getTag()) return false;
System.out.println("Tuple Match 2 ");
                if (!isPartialMatch) {
System.out.println("Tuple Match 2a ");
		  if (template.numFields() != this.numFields()) return false;
                }
System.out.println("Tuple Match 3");
		Set tKeys = this.fieldNames();
//System.out.println("SET "+tKeys);
		Set aKeys = template.fieldNames();
//System.out.println("SET "+aKeys);
                if (!isPartialMatch) {
		  if (!aKeys.containsAll(tKeys)) return false;
                }
System.out.println("Tuple Match 4");

		// iterate through the fields in the antiTuple
		// the tuple matches if: all fields have the same names and types;
		// all non-null fields in the antituple have the same values
		// in the tuple

		Iterator<String> antiNames = aKeys.iterator();
//		Iterator<String> tupNames = tKeys.iterator();
		String fieldName;
		Object aValue;
		Object tValue;
		while (antiNames.hasNext()) {
			fieldName = antiNames.next();
			aValue = template.get(fieldName);
      System.out.println("Tuple looking for "+allowPartialMatch+" // "+fieldName+" // "+aValue);
                  /**
                        if (fieldName.equals("*") && aValue.equals("*")) {
                          // we found a wild card field which ends the search
                          return true;
                        }
                  */
                        ///////////////////////////////////
                        // FIXME: need to deal with wildcard fieldName or aValue
                        ///////////////////////////////////
			if (aValue == null) continue;
			tValue = this.get(fieldName);
			if (tValue == null)
				return false; // should never get here because we compared the key sets above
			// are the fields instances of the same class?
                        // all values are Strings
		//	if (tValue.getClass().getName() == aValue.getClass().getName()) {
				if (!(aValue.equals(tValue))) return false;
		//	}
		//	else {
		//		return false;
		//	}
		}
		// if you made it this far, it's a match
		return true;

	}

	/**
	 * Priority support
	 */
	public void setPriority(int newPriority) {
		this.priority = newPriority;
	}
	public int getPriority() {
		return this.priority;
	}
	/**
	 * Recycle support
	 */
	public void clearFields() {
		//FIXME
	}
	public void setTag(String newTag) {
		this.tag = newTag;
	}


    /**
     * @return tuple encoded as a string:
     *  <tuple>
     *    <field>
     *      <name>...</name>
     *      <value>...</value>
     *    </field>
     *    <priority>...</priority>
     *   </tuple>
     */
    public String toString() {
      StringBuffer buf = new StringBuffer("<"+TUPLE+">\n");
      if (id != null)
        buf.append("  <"+ID+">"+id+"</"+ID+">\n");
      if (mySpace != null)
        buf.append("  <"+SPACE+">"+mySpace+"</"+SPACE+">\n");
      buf.append("  <"+GROUP+">"+tag+"</"+GROUP+">\n");
      if (command != null)
        buf.append("  <"+DO+">"+command+"</"+DO+">\n");
      buf.append("  <"+REQUEST_ID+">"+requestId+"</"+REQUEST_ID+">\n");
      if (allowPartialMatch)
        buf.append("  <"+PARTIAL_MATCH+"/>\n");
      Set keys = tupleFields.keySet();
      Iterator itr = keys.iterator();
      String n = null;
      while (itr.hasNext()) {
        n = (String)itr.next();
        buf.append("  <"+FIELD+">\n");
        buf.append("    <"+NAME+">"+n+"</"+NAME+">\n");
        buf.append("    <"+VALUE+">"+(String)tupleFields.get(n)+"</"+VALUE+">\n");
        buf.append("  </"+FIELD+">\n");
      }
      if (priority > -1)
        buf.append("  <"+PRIORITY+">"+Integer.toString(priority)+"</"+PRIORITY+">\n");
      buf.append("</"+TUPLE+">\n");
      return buf.toString();
    }
        /**
     * Speedup:
     * @param ITuple that matches
     */
    public void setMatch(ITuple match) {
      this.matchTuple = match;
    }
    /**
     *
     * Speedup:
     * @return matching Tuple
     */
    public ITuple getMatch() {
      return this.matchTuple;
    }
	@Override
	public int compareTo(Object o) {
		return this.getPriority()-((ITuple)o).getPriority();
	}
	@Override
	public Map<String, Object> getFields() {
		return tupleFields;
	}

}