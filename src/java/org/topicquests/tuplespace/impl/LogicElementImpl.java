/*
 * Copyright 2024 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.tuplespace.impl;

/**
 * LogicElementImpl.java
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 * @author Jack Park
 */

import java.util.ArrayList;

import org.topicquests.tuplespace.api.ILogicElement;
import org.topicquests.tuplespace.api.ITuple;

public class LogicElementImpl implements ILogicElement {
	/**
	 * elements to be interpreted.
	 */
	private ArrayList<Object> elements = new ArrayList<Object>();
	/**
	 * literal object
	 */
	private Object literal = null;
	
	private String fieldKey = null;
	/**
	 * What type of ILogicElement am I?
	 */
	private int myLogicType = -1;
	/**
	 * Constructor. 
	 */
	public LogicElementImpl() {}
	/**
	 * Type Setters
	 */
	public void isOrType() { this.myLogicType=OR; }
	public void isAndType() { this.myLogicType=AND; }
	public void isNotType() { this.myLogicType=NOT; }
	public void isEqualsType() { this.myLogicType=EQUALS; }
	public void isLiteralType() { this.myLogicType=LITERAL; }
	public void isFetchType() { this.myLogicType=FETCH; }

	//////////////////////////
	// Interpreters
	/////////////////////////
	/**
	 * eval
	 * @return boolean true if match occurs
	 */
	public boolean eval(ITuple inTuple) {
		if (myLogicType==OR)
			return evalOR(inTuple);
		else if (myLogicType==AND)
			return evalAND(inTuple);
		else if (myLogicType==NOT)
			return evalNOT(inTuple);
		else if (myLogicType==EQUALS)
			return evalEQUALS(inTuple);
		//FIXME: need some error message if we fall out here
		return false;
	}

	/**
	 * The OR interpreter
	 *	This will fail if any element is a literal or fetchliteral
	 * @return boolean true if match occurs
	 */
	boolean evalOR(ITuple inTuple) {
		boolean result = false;
		int elementLength = elements.size();
		ILogicElement op1 = null;
		for (int i = 0; i < elementLength; i++) {
			// setup second op
			op1 = (ILogicElement)elements.get(i);
			result = op1.eval(inTuple);
			if (result)
				return true;
		}
		return result;
	}
	/**
	 * The AND interpreter
	 *	This will fail if any element is a literal or fetchliteral
	 * @return boolean true if match occurs
	 */
	boolean evalAND(ITuple inTuple) {
		boolean result = true;
		int elementLength = elements.size();
		ILogicElement op1 = null;
		for (int i = 0; i < elementLength; i++) {
			// setup second op
			op1 = (ILogicElement)elements.get(i);
			result = op1.eval(inTuple);
			if (!result)
				return false;
		}
		return result;
	}
	/**
	 * The EQUALS interpreter
	 * Equals wants to compare a list of literals
	 * Design rule:
	 *	all elements must be literals or fetchliterals
	 *	eval fails if any element is not a literal or fetchliteral
	 *	eval fails if any element not equal
	 * @return boolean true if match occurs
	 */
	public boolean evalEQUALS(ITuple inTuple) {
		Object obj2 = null;
		String fieldKey = null;
		// setup first field
		fieldKey = this.getFieldName(); //.getFieldName();
		obj2 = inTuple.get(fieldKey);
		return (obj2.equals(this.getLiteral()));
	}
	/**
	 * The NOT interpreter
	 *	This will fail if any element is a literal or fetchliteral
	 * @return boolean true if match occurs
	 */
	public boolean evalNOT(ITuple inTuple) {
		boolean result = false;
		int elementLength = elements.size();
		ILogicElement op1 = null;
		for (int i = 0; i < elementLength; i++) {
			// setup second op
			op1 = (ILogicElement)elements.get(i);
			result = op1.eval(inTuple);
			if (!result)
				return true;
		}
		return result;
	}
	//////////////
	// support methods
	//////////////
	/**
	 * Add element to operand list
	 * @param Object is LogicElement
	 */
	public void addElement(Object operand) {
		this.elements.add(operand);
	}

	/**
	 * Clear elements -- for recycling
	 */
	public void clearElements() {
		this.elements.clear();
		this.literal = null;
	}

	/**
	 * Set a literal value
	 */
	public void setLiteral(Object inObject) {
		this.literal = inObject;
	}
	/**
	 * Fetch a literal value
	 * @return java.lang.Object, a literal value
	 */
	public Object getLiteral() {
		return this.literal;
	}
	/**
	 * Return true if this element is a literal object
	 * @return boolean
	 */
	public boolean isLiteral() { return (myLogicType==LITERAL);}
	/**
	 * Set a field name for later fetching from Tuple
	 * @param String literal name--a field name in a Tuple
	 */
	public void setFieldName(String fieldName) {
		this.fieldKey=fieldName;
	}
	/**
	 * Get a field name to fetch from a Tuple
	 * @return String field name from a Tuple
	 */
	public String getFieldName() {
		return this.fieldKey;
	}

}