/**
 * 
 */
package org.topicquests.tuplespace.test;
import org.topicquests.tuplespace.DynamicAgenda;
import org.topicquests.tuplespace.api.IDynamicAgenda;
/**
 * 
 */
public class TestMain {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IDynamicAgenda agenda = new DynamicAgenda();
		
		new FirstTest(agenda);
	}

}
