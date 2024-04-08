/**
 * 
 */
package org.topicquests.tuplespace.test;

import org.topicquests.tuplespace.api.IDynamicAgenda;
import org.topicquests.tuplespace.api.ITupleSpace;

/**
 * 
 */
public class FirstTest {
	private final String name = "TestChannel";
	/**
	 * 
	 */
	public FirstTest(IDynamicAgenda agenda) {
		
		agenda.createChannel(name);
		
		ITupleSpace t = agenda.getChannel(name);
		
		System.out.println("DID "+ t);
	}

}
