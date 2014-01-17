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

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicDoubleProperty;
import com.netflix.config.DynamicFloatProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringMapProperty;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.DynamicStringSetProperty;
import com.netflix.config.Property;

/**
 * Enum definition of the types of Archaius dynamic properties
 */
public enum PropertyType {
    
    BOOLEAN(DynamicBooleanProperty.class), 
    DOUBLE(DynamicDoubleProperty.class), 
    FLOAT(DynamicFloatProperty.class), 
    INT(DynamicIntProperty.class), 
    LONG(DynamicLongProperty.class), 
    STRING(DynamicStringProperty.class), 
    STRING_LIST(DynamicStringListProperty.class), 
    STRING_MAP(DynamicStringMapProperty.class), 
    STRING_SET(DynamicStringSetProperty.class);
    
    private final Class<? extends Property<?>> propertyClass;
    
    private PropertyType(Class<? extends Property<?>> propertyClass) {
        this.propertyClass = propertyClass;
    }
    
    public Class<? extends Property<?>> getPropertyClass() {
        return propertyClass;
    }
    
    @SuppressWarnings("rawtypes")
    public static PropertyType getByPropertyClass(Class<? extends Property> propertyClass) {
        for (PropertyType type : values()) {
            if (type.getPropertyClass().equals(propertyClass)) {
                return type;
            }
        }
        return null;
    }

}
