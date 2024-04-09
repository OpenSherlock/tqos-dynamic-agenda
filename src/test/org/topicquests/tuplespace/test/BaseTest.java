/**
 * 
 */
package org.topicquests.tuplespace.test;

import org.topicquests.tuplespace.DynamicAgenda;
import org.topicquests.tuplespace.api.IDynamicAgenda;
import org.topicquests.tuplespace.api.ITupleSpace;

/**
 * 
 */
public class BaseTest {
	protected IDynamicAgenda agenda = new DynamicAgenda();
	protected final String name = "TestChannel";
	protected ITupleSpace channel;
	protected final String
		FLD_1	= "fieldA",
		FLD_2	= "fieldB",
		FLD_3	= "fieldC",
		VAL_1	= "foo",
		VAL_2	= "bar",
		VAL_3 	= "bah";

	/**
	 * 
	 */
	public BaseTest() {
		agenda.createChannel(name);
		channel = agenda.getChannel(name);
	}

}
