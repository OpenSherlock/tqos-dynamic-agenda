/*
 * Copyright 2024 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.tuplespace.api;

/**
 * @author jackpark
 */
public interface ITemplate extends ITuple {

	/**
	 * Get ILogicElement for matching
	 * @return IConstraint -- a class containing FOL for matching
	 */
	ILogicElement getConstraint();
	
	/**
	 * Compile an ILogicElement from the tuple's properties
	 */
	void compile();
 
}
