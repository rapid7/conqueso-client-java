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

import static com.google.common.base.Preconditions.*;

import javax.annotation.Nullable;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * Data object used to represent information about an Archaius dynamic property. This data will be serialized
 * to JSON to transmit to the Conqueso server.
 */
public class PropertyDefinition {
    
    private final String name;
    private final PropertyType type;
    private final String value;
    
    @JsonCreator
    public PropertyDefinition(@JsonProperty("name") String name, 
            @JsonProperty("type") PropertyType type, 
            @JsonProperty("value") @Nullable String value) {
        checkArgument(!Strings.isNullOrEmpty(name), "name");
        this.name = name;
        this.type = checkNotNull(type, "type");
        this.value = Strings.nullToEmpty(value);
    }

    public String getName() {
        return name;
    }
    
    public PropertyType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, type, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PropertyDefinition other = (PropertyDefinition) obj;
        return Objects.equal(name, other.name) &&
                Objects.equal(type, other.type) &&
                Objects.equal(value, other.value);
    }

    @Override
    public String toString() {
        return "PropertyDefinition [name=" + name + ", type=" + type + ", value=" + value + "]";
    }
}
