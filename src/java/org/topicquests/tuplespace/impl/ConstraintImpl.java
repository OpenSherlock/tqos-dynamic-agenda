package org.topicquests.tuplespace.impl;

/**
 * ConstraintImpl.java
 *
 * A class that carries 'first order logic' pattern for Tuple matching
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * @author Jack Park
 */
import java.util.ArrayList;

import org.topicquests.tuplespace.api.IConstraint;
import org.topicquests.tuplespace.api.ILogicElement;
import org.topicquests.tuplespace.api.ITuple;

public class ConstraintImpl implements IConstraint {
	/**
	 * The initial sentence supplied by user <NOT IMPLEMENTED YET>
	 *  e.g.
	 *	and(<obj>,<obj>,and(<obj>,or(<obj>,<obj>)))
	 */
	private ArrayList constraintSentence = new ArrayList();
	/**
	 * The compiled sentence
	 */
	private ILogicElement theConstraint = null;

	public ConstraintImpl() {}


	/**
	 * The interpreter
	 * @return boolean
	 */
	public boolean eval(ITuple inTuple) {
		boolean result = false;
		if (theConstraint != null)
			result = theConstraint.eval(inTuple);
		// FIXME: else need error message here
		return result;
	}


	public void clearConstraints() {
		constraintSentence.clear();
		theConstraint = null;
	}

	/**
	 * Set the constraint sentence
	 * @param ArrayList -- a list that contains a fol sentence
	 *		to be interpreted
	 */
	public void setConstraints(ArrayList sentence) {
		this.constraintSentence = sentence;
		compileSentence();
	}

	/**
	 * Set the compiled IConstraint
	 * @param LogicElement build by caller
	 */
	public void setConstraint(ILogicElement newConstraint) {
		this.theConstraint = newConstraint;
	}

	/**
	 * Compile a sentence into a prefix, rooted sentence
	 */
	void compileSentence() {

		//FIXME:
	}

}