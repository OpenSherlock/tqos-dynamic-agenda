/**
 * 
 */
package org.topicquests.tuplespace.test;

import org.topicquests.tuplespace.api.ITemplate;
import org.topicquests.tuplespace.api.ITuple;

import java.util.Iterator;
import java.util.Map;
/**
 * @author jackpark
 */
public class SecondTest extends BaseTest {
	/**
	 * Make some Tuples and read thm
	 */
	public SecondTest() {
		// Create some tuples and store and examine results
		Map<String, Object> p = TupleUtil.newProperties();
		p.put(FLD_1, VAL_1);
		p.put(FLD_2, VAL_2);
		ITuple t1 = TupleUtil.createTuple(name, 10, p);
		//System.out.println(t1.toString());
		p = TupleUtil.newProperties();
		p.put(FLD_1, VAL_1);
		p.put(FLD_2, VAL_2);
		p.put(FLD_3, VAL_3);
		ITuple t2 = TupleUtil.createTuple(name, 20, p);
		//System.out.println(t2.toString());
		p = TupleUtil.newProperties();
		p.put(FLD_1, VAL_1);
		p.put(FLD_2, VAL_3);
		p.put(FLD_3, VAL_2);
		ITuple t3 = TupleUtil.createTuple(name, 30, p);
		//System.out.println(t3.toString());
		agenda.put(name, t1);
		agenda.put(name, t2);
		agenda.put(name, t3);
		System.out.println("iterating");
		Iterator<ITuple> tups = agenda.listTuples(name);
		while (tups.hasNext()) 
			System.out.println(tups.next().toString());
		System.out.println("matching");

		p = TupleUtil.newProperties();
		p.put(FLD_1, VAL_1);
		p.put(FLD_2, VAL_2);
		ITemplate tx = TupleUtil.createTemplate(p);
		tx.compile();
		ITuple mx1 = agenda.take(name, tx, 1000);
		System.out.println(mx1.toString());

	}

}
