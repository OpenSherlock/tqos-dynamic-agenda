/*
 * ============================================================================
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
 *  Created:      Feb 25, 2008
 * ============================================================================ 
 */
package org.semispace;

import java.util.Map;

import org.semispace.api.ISemiEventListener;

/**
 * Holds a reference to the listener, including how long it shall live.
 */
public class ListenerHolder {
    private long liveUntil;
    private long id;
    private ISemiEventListener listener;
    private Map<String, Object> searchMap;

    public ListenerHolder(long id, ISemiEventListener listener, long liveUntil, Map<String, Object> map) {
        this.listener = listener;
        this.liveUntil = liveUntil;
        this.searchMap = map;
        this.id=id;
    }

    public ISemiEventListener getListener() {
        return this.listener;
    }

    public long getLiveUntil() {
        return this.liveUntil;
    }

    public Map<String, Object> getSearchMap() {
        return this.searchMap;
    }

    public long getId() {
        return this.id;
    }

    protected void setLiveUntil(long liveUntil) {
        this.liveUntil = liveUntil;
    }

}
