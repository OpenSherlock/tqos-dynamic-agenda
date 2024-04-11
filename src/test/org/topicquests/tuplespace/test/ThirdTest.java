/**
 * 
 */
package org.topicquests.tuplespace.test;

import java.util.Iterator;
import java.util.Map;

import org.topicquests.tuplespace.api.ITemplate;
import org.topicquests.tuplespace.api.ITuple;

/**
 * 
 */
public class ThirdTest extends BaseTest {

	/**
	 * 
	 */
	public ThirdTest() {
		populate();
		System.out.println("BEFORE");
		Iterator<ITuple> tups = agenda.listTuples(name);
		while (tups.hasNext()) 
			System.out.println(tups.next().toString());

		Map<String, Object> p = TupleUtil.newProperties();
		p.put(FLD_1, VAL_1);
		p.put(FLD_2, VAL_2);
		p.put(FLD_3, VAL_3);
		// this tuple has a score of 20 and highest is 30
		// let's jump over it.
		ITemplate tx = TupleUtil.createTemplate(p);
		tx.compile();
		agenda.addValue(name, tx, 15);
		System.out.println("AFTER");
		tups = agenda.listTuples(name);
		while (tups.hasNext()) 
			System.out.println(tups.next().toString());
		

	}

	void populate() {
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
	}
}
/*
INSERTING <tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bar</value>
  </field>
  <priority>10</priority>
</tuple>

INSERTING <tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bah</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bar</value>
  </field>
  <priority>20</priority>
</tuple>

INSERTING <tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bar</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bah</value>
  </field>
  <priority>30</priority>
</tuple>

BEFORE
<tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bar</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bah</value>
  </field>
  <priority>30</priority>
</tuple>

<tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bah</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bar</value>
  </field>
  <priority>20</priority>
</tuple>

<tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bar</value>
  </field>
  <priority>10</priority>
</tuple>

MATCHING <tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bar</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bah</value>
  </field>
  <priority>30</priority>
</tuple>

MATCHING To <tuple>
  <group>template</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bah</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bar</value>
  </field>
</tuple>

StartingMatch org.topicquests.tuplespace.impl.LogicElementImpl@6d06d69c
MATCHING <tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bah</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bar</value>
  </field>
  <priority>20</priority>
</tuple>

MATCHING To <tuple>
  <group>template</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bah</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bar</value>
  </field>
</tuple>

StartingMatch org.topicquests.tuplespace.impl.LogicElementImpl@6d06d69c
MATCHING GOT MATCH
Pbefore true20
Pafter 35
INSERTING <tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bah</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bar</value>
  </field>
  <priority>35</priority>
</tuple>

AFTER
<tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bah</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bar</value>
  </field>
  <priority>35</priority>
</tuple>

<tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldC</name>
    <value>bar</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bah</value>
  </field>
  <priority>30</priority>
</tuple>

<tuple>
  <group>TestChannel</group>
  <field>
    <name>fieldA</name>
    <value>foo</value>
  </field>
  <field>
    <name>fieldB</name>
    <value>bar</value>
  </field>
  <priority>10</priority>
</tuple>


*/