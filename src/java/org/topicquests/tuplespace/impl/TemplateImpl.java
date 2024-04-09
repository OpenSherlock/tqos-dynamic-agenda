/*
 * Copyright 2024 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.tuplespace.impl;

import java.util.Iterator;
import java.util.Map;

import org.topicquests.tuplespace.api.ILogicElement;
import org.topicquests.tuplespace.api.ITemplate;

/**
 * @author jackpark
 */
public class TemplateImpl extends TupleImpl implements ITemplate {
	private ILogicElement constraint;
	/**
	 * 
	 */
	public TemplateImpl() {
		super("template");
	}


	@Override
	public ILogicElement getConstraint() {
		return constraint;
	}

	@Override
	public void compile() {
		Map<String,Object> data = this.getFields();
		String key;
		Iterator<String> itr = data.keySet().iterator();
		


	}

}
