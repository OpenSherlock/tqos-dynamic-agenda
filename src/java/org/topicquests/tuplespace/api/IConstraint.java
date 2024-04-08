/*
 * Copyright 2024 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.tuplespace.api;

/**
 *  IConstraint.java
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 * @author Jack Park
 */
 
import java.util.ArrayList;

public interface IConstraint {

	/**
	 * Clear all constraint fields for recycling
	 */
	void clearConstraints();
	/**
	 * Set the constraint sentence
	 * @param ArrayList -- a list that contains a fol sentence
	 *		to be interpreted
	 */
	void setConstraints(ArrayList sentence);
	/**
	 * Set the compiled IConstraint
	 * @param ILogicElement build by caller
	 */
	void setConstraint(ILogicElement newConstraint);
	/**
	 * The interpreter
	 * @return boolean
	 */
	boolean eval(ITuple inTuple);
	
}