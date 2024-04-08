package org.topicquests.tuplespace.api;

/**
 * ConstraintFactory.java
 *
 *	A class that carries 'first order logic' constraints
 *	for ITuple matching
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * @author Jack Park
 */

//import org.nex.tuplespace.impl.ConstraintImpl;
import java.util.Stack;

public final class ConstraintFactory {
	/**
	 * IConstraint Pool to reduce number of creates
	 */
	private static Stack constraintPool = new Stack();


	/**
	* Creates an empty IConstraint
	* @return an instance of a IConstraint
	* /
	public static IConstraint create() {
		IConstraint result = null;
		if (constraintPool.empty()) {
			result = new ConstraintImpl();
		} else {
			result = (IConstraint)constraintPool.pop();
		}
		return result;
	}

	/**
	 * Recycle an unneeded IConstraint
	 */
	public static void recycleConstraint(IConstraint oldConstraint) {
		oldConstraint.clearConstraints();
		constraintPool.push(oldConstraint);
	}

}