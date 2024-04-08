/*
 * ============================================================================
 *
 *  File:     Tuple.java
 *----------------------------------------------------------------------------
 *
 * Copyright 2008 Erlend Nossum
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 *  Description:  See javadoc below
 *
 *  Created:      24. des.. 2007
 * ============================================================================ 
 */

package org.semispace;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap; //TODO should be ConcurrentHashMap ???
import java.util.List;
import java.util.ArrayList;
import org.semispace.api.ISemiSpaceTuple;
import org.semispace.api.ITupleFields;

import org.json.simple.JSONObject;
/**
 * Tuple of an entry written into the space.
 * @park modifications
 */
public class Tuple implements ISemiSpaceTuple {
	//from IdentifyAdminQuery
    public Integer idx;
    public Boolean amIAdmin;
    public Boolean hasAnswered;
    //from NameValueQuery
    public String name;
    public String value;
    //from TimeAnswer
    public Long timeFromMaster;
    public int masterId;
    //from TimeQuery
    public Boolean isFinished;


    private long liveUntil = Long.MAX_VALUE; // default value
    private String json;
    private long id = -1;
    private Map<String, Object> searchMap;
    private String tag;
    
    public long getId() {
    	long result = this.id;
    	if (result == -1) {
    		Object o = searchMap.get(ITupleFields.ID);
    		if (o != null)
    			result = ((Long)o).longValue();
    	}
        return result;
    }

    public void setId(Long id) {
    	this.id = id;
    	set(ITupleFields.ID,id);
    }
  
    public Tuple(String json, long liveUntil, String tag, long id, Map<String, Object> map) {
        this.searchMap = map;
        this.json = json;
        this.liveUntil = liveUntil;
        setId(new Long(id));
        setTag(tag);
    }
    
    public Tuple(long id, String tag) {
    	this.searchMap = new HashMap<String,Object>();
        setId(new Long(id));
    	setTag(tag);
    }
    
    public Tuple(long id, String tag, Map<String,Object>props) {
    	this.searchMap = props;
        setId(new Long(id));
    	setTag(tag);
   }

    public String getTag() {
    	return this.tag;
    }
    public String getJSON() {
    	if (this.json == null) {
    		synchronized(searchMap) {
	    		JSONObject jobj= new JSONObject(this.searchMap);
	    		json = jobj.toString();
    		}
    	}
        return this.json;
    }

    public synchronized long getLiveUntil() {
        return this.liveUntil;
    }

    public Map<String, Object> getSearchMap() {
    	synchronized(searchMap) {
    		return this.searchMap;
    	}
    }


    public synchronized void setLiveUntil(long liveUntil) {
        this.liveUntil = liveUntil;
    }

	@Override
	public void setTag(String newTag) {
		this.tag = newTag;
	       this.searchMap.put("tag", tag);
	}

	@Override
	public void set(String name, Object f) {
		synchronized(searchMap) {
			this.searchMap.put(name, f);
		}
	}

	@Override
	public Object get(String name) {
		synchronized(searchMap) {
			return this.searchMap.get(name);
		}
	}


	@Override
	public void setPriority(Long p) {
		set("priority",p);
	}


	@Override
	public Long getPriority() {
		return (Long)get("priority");
	}

	@Override
	public void addAgentName(String agentName) {
		synchronized(searchMap) {
			List<String>o = (List<String>)this.searchMap.get(ITupleFields.AGENT_NAME);
			if (o == null) {
				o = new ArrayList<String>();
				searchMap.put(ITupleFields.AGENT_NAME, o);
			}
			o.add(agentName);
		}
	}


}
