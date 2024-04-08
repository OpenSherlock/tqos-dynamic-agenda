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
	public void clearConstraints();
	/**
	 * Set the constraint sentence
	 * @param ArrayList -- a list that contains a fol sentence
	 *		to be interpreted
	 */
	public void setConstraints(ArrayList sentence);
	/**
	 * Set the compiled IConstraint
	 * @param ILogicElement build by caller
	 */
	public void setConstraint(ILogicElement newConstraint);
	/**
	 * The interpreter
	 * @return boolean
	 */
	public boolean eval(ITuple inTuple);
	
}