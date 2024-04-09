package org.topicquests.tuplespace.api;

import java.util.Map;

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
 * $Id: ITuple.java,v 1.4 2001/07/08 16:13:39 vwilliams Exp $
 */


//import java.util.Iterator;
import java.util.Set;
import java.sql.Timestamp;
/* to compile for JDK 1.1 uncomment and link to Collections 1.1 lib
import com.sun.java.util.collections.Iterator;
*/

/**
* Interface for the basic ITuple data structure.
* Includes methods for copying Tuples, adding fields,
* accessing fields, and testing for tuple/template matches.
*
* @author Vanessa Williams
* @version $Revision: 1.4 $, $Date: 2001/07/08 16:13:39 $
*
*/
/**
 * @author Jack Park
 *
 *	FIXME:
 *		To Do:
 *			add direct support for priority -- done
 *			add support for fol match -- fieldNames() does this
 */
public interface ITuple extends Comparable {

    /**
     * Features added to let ITuple know it's command -- put, take, read
     */
    String getCommand();
    void setCommand(String cmd);
    /**
     * Features added to let ITuple know it's ITupleSpace name
     */
    String getSpace();
    void setSpace(String space);
	/**
	* Returns a copy of the ITuple. Essentially the same
	* as clone(), but I needed to specify it in the interface.
	* @return an exact replica of this ITuple
	*/
	ITuple copy();

    /**
     * Timestamp support
     */
    Timestamp getCreated();
    void setCreated(Timestamp ts);

    /**
     * Transaction support <requestId>
     */
    String getRequestId();
    void setRequestId(String id);
    
	/**
	* Gets the tag associated with this ITuple.
	* @return a String
	*/
	String getTag();

	/**
	 * Set the tag associated with this ITuple -- to allow recycling
	 */
	void setTag(String newTag);

	/**
	* Sets the value of a named field.
	* @param String field name
	* @param Object any Object. If this tuple is a template(anti-tuple)
	*		  		the value may be null
	*/
	void set(String name, Object f);

	/**
	* Get the value of the field with the given name.
	* @param index an index into the ordered list
	* @return an Object
	*/
	Object get(String name);

	/**
	* Return a hash value for this ITuple such that all Tuples with the
	* same "signature" (same tag, same number and type of fields)
	* hash to the same value. Distinct from hashCode(), which would
	* generate a unique hash for every unique ITuple. Ideally tuples/templates
	* with different signatures should hash to different values to optimize
	* storage in collections, but it isn't required.
	* @return an Object, most likely a String or an Integer
        *
        * In the current version, hash == tag, which is the "group" to which
        * the ITuple is assigned, which defaults to "*"
        * hash is always a String
	**/
	Object hash();

	/**
	* Get the number of fields in this ITuple.
	* @return int
	*/
	int numFields();

	/**
	* Return a set of all the field names in this ITuple
	*/
	Set<String> fieldNames();

	Map<String,Object> getFields();

	/**
	* Determine whether this tuple is a match for the given templat
	* (template).
	* @param template a ITuple to be used as a template
	* @return true if the ITuple is a match for the template, false otherwise.
	*/
	boolean matches(final ITemplate template);

    /**
     * Identity for each ITuple
     */
    void setID(String id);
    String getID();

    /**
     * Allow for partial matching
     * If true, only the fields in the AntiTuple need match
     * no matter how many other fields are present
     */
    void setAllowPartialMatch(boolean tf);
    boolean getAllowPartialMatch();
	/**
	 * Set priority value
	 * @param int priority
	 */
	void setPriority(int priorityValue);
	/**
	 * Get priority value
	 * @return int priority value
	 */
	int getPriority();
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
    String toString();
    /**
     * Speedup:
     * @param ITuple that matches
     */
    void setMatch(ITuple match);
    /**
     *
     * Speedup:
     * @return matching ITuple
     */
    ITuple getMatch();
}
