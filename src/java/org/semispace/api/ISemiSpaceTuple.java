/*
 * Copyright 2013, TopicQuests
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.semispace.api;

import java.util.Map;

/**
 * @author park
 * Give Tuple an API
 */
public interface ISemiSpaceTuple {
	
	/**
	 * <p>When an agent reads a tuple that is specific to a particular tag,
	 * then that agent should not be presented that tuple again</p>
	 * <p>The use case is polling for new tuples of a particular tag.</p>
	 * <p>To support that, we record the <code>agentName</code> with the tuple
	 * so that it wilil fail to be found when the same agent returns for another
	 * tuple</p>
	 * <p>This process is made complex since the tuple, itself, is sequestered
	 * inside a tuple collection.</p>
	 * @param agentName
	 */
	void addAgentName(String agentName);
	
	
	/**
	 * Gets the tag associated with this ISemiSpaceTuple.
	 * @return a String
	 */
	String getTag();
	
	long getId();
	
	void setId(Long id);
	/**
	 * Set the tag associated with this ISemiSpaceTuple -- to allow recycling
	 */
	void setTag(String newTag);
	
	Map<String, Object> getSearchMap();
	
	long getLiveUntil();
	
	/**
	 * Sets the value of a named field.
	 * @param String field name
	 * @param Object any Object. If this tuple is a template(anti-tuple)
	 *		  		the value may be null
	 */
	void set(String name, Object f);

	/**
	 * Get the value of the field with the given name.
	 * @param index an index into the ordered list
	 * @return an Object
	 */
	Object get(String name);
	
	String getJSON();
	
	void setLiveUntil(long liveUntil);
	
	void setPriority(Long p);
	Long getPriority();
}
