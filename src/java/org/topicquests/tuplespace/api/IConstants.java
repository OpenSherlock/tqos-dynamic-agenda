package org.topicquests.tuplespace.api;

/**
 * <p>Title: TupleSpace4J</p>
 * <p>Description: Tuplespace implementation</p>
 * <p>Copyright: Copyright (c) 2003 Jack Park</p>
 * <p>Company: Nex</p>
 * @author Jack Park
 * @version 1.0
 * @license Jabber Open Source License (JOSL)
 */

public interface IConstants {
  /////////////
  // COMMANDS
  /////////////
  /**
   * Put a tuple (insert)
   */
  public static final String PUT = "put";
  /**
   * Take without waiting
   * Can return null
   */
  public static final String TAKE = "take";
  /**
   * Read without waiting
   * Can return null
   */
  public static final String READ = "read";
  /**
   * Take while waiting
   */
  public static final String TAKE_WAIT = "twait";
  /**
   * Read while waiting
   */
  public static final String READ_WAIT = "rwait";
  /**
   * Read and return later to a "po box"
   * Requires a response that is either an empty Tuple
   * or the result
   * po_box is a TupleGroup associated with the <userId> tag
   * should also be associated with a <requestId> to properly
   * associate returned Tuple with request
   * A READ_POST with an empty Tuple means just send back
   * what's in my mailbox. Response would be to send
   * a <message> with all <tuple>s found
   */
  public static final String READ_POST = "rpost";
  /**
   * Take and return later to a "po box"
   * Requires a response that is either an empty Tuple
   * or the result
   * po_box is a TupleGroup associated with the <userId> tag
   * should also be associated with a <requestId> to properly
   * associate returned Tuple with request
   * A TAKE_POST with an empty Tuple means just send back
   * what's in my mailbox. Response would be to send
   * a <message> with all <tuple>s found
   */
  public static final String TAKE_POST = "tpost";
  /**
   * Read all matching Tuples
   * Returns a list of Tuples inside a <message>
   * Can return an empty message
   */
  public static final String COLLECT = "collect";
  /**
   * Fetch a list of all tuples in active memory
   * The theory here is that we want to see what's in the cache
   * May modify this to tell all tuples persisted (some of which may
   * be in active memory)
   */
   public static final String LIST ="list";
  /////////////
  // TSX XML tags
  /////////////
  public static final String MESSAGE = "message";
  public static final String DO = "do";  // the command executed by server
  public static final String ID = "id";  // for the tuple itself
  public static final String USER_ID = "userId"; // for the particular user
  public static final String REQUEST_ID = "requestID";
  public static final String TUPLE = "tuple";
  public static final String SPACE = "space";
  public static final String GROUP = "group";
  public static final String FIELD = "field";
  public static final String NAME = "name";
  public static final String VALUE = "value";
  public static final String PRIORITY = "priority"; // special for The Scholar's Companion
  public static final String PARTIAL_MATCH = "partialMatch";

}