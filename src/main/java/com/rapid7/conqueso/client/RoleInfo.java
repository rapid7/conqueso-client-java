/**
 * COPYRIGHT (C) 2014, Rapid7 LLC, Boston, MA, USA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rapid7.conqueso.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Information about a Conqueso role, returned from querying the Conqueso server.
 */
public class RoleInfo {
    
    private final String name;
    private final int instances;
    
    @JsonCreator
    public RoleInfo(@JsonProperty("name") String name,
            @JsonProperty("instances") int instances) {
        
        this.name = name;
        this.instances = instances;
    }

    public String getName() {
        return name;
    }

    public int getInstances() {
        return instances;
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, instances);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RoleInfo other = (RoleInfo) obj;
        return Objects.equal(name, other.name) &&
                Objects.equal(instances, other.instances);
    }
}
