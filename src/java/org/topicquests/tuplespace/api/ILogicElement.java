/*
 * Copyright 2024 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.tuplespace.api;

/**
 * ILogicElement.java
 *
 *	Interface for a variety of Logic elements
 *		e.g. AndElement, OrElement, EqualsElement
 *
 *
 *	LogicElements are combined (nested) from a root element
 *	to form prefix sentences that self-interpret
 *
 *	Interpretation occurs by comparison against a given ITuple
 *	If the given ITuple can satisfy the sentence
 *	Then a match occurs and the sentence returns 'true'
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 * @author Jack Park
 */

public interface ILogicElement {
	/**
	 *  Local IConstants for Element type
	 */
	public static final int AND = 0;
	public static final int OR = 1;
	public static final int NOT = 2;
	public static final int EQUALS = 3;
	public static final int LITERAL = 4;
	public static final int FETCH = 5;
	
	/**
	 * Type Setters
	 */
	void isOrType();
	void isAndType();
	void isNotType();
	void isEqualsType();
	void isLiteralType();
	void isFetchType();
	/**
	 * The interpreter
	 */
	boolean eval(ITuple inTuple);
	
	/**
	 * Add element to operand list
	 * @param Object is ILogicElement
	 */
	void addElement(Object operand);
	
	/**
	 * Set a literal value
	 */
	void setLiteral(Object inObject);
	/**
	 * Fetch a literal value
	 * @return java.lang.Object, a literal value
	 */
	Object getLiteral();
	
	/**
	 * Return true if this element is a literal object
	 * @return boolean
	 */
	boolean isLiteral();
	/**
	 * Set a field name for later fetching from ITuple
	 * @param String literal name--a field name in a ITuple
	 */
	void setFieldName(String fieldName);
	/**
	 * Get a field name to fetch from a ITuple
	 * @return String field name from a ITuple
	 */
	String getFieldName();
}