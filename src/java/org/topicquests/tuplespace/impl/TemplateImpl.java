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

	public TemplateImpl(Map<String,Object> properties) {
		super("template", properties);
	}


	@Override
	public ILogicElement getConstraint() {
		return constraint;
	}

	@Override
	public void compile() {
		ILogicElement root = new LogicElementImpl();
		root.isAndType();
		Map<String,Object> data = this.getFields();
		String key;
		Object val;
		ILogicElement x;
		Iterator<String> itr = data.keySet().iterator();
		while (itr.hasNext()) {
			key = itr.next();
			val = data.get(key);
			x = new LogicElementImpl();
			x.isEqualsType();
			x.setFieldName(key);
			x.setLiteral(val);
			root.addElement(x);
		}


	}

}
