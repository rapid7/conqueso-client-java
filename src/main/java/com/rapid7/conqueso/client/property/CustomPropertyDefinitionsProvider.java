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
package com.rapid7.conqueso.client.property;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyDefinitionsProvider;

/**
 * Provider to add static custom property definitions to reported to the Conqueso server.
 */
public class CustomPropertyDefinitionsProvider implements PropertyDefinitionsProvider {
    
    private final ImmutableMap<String, PropertyDefinition> propertyDefinitions;
    
    public CustomPropertyDefinitionsProvider(PropertyDefinition...definitions) {
        this(Arrays.asList(definitions));
    }
    
    public CustomPropertyDefinitionsProvider(Collection<PropertyDefinition> definitions) {
        ImmutableMap.Builder<String, PropertyDefinition> builder = ImmutableMap.builder();
        for (PropertyDefinition definition : definitions) {
            builder.put(definition.getName(), definition);
        }
        
        this.propertyDefinitions = builder.build();
    }

    @Override
    public void addPropertyDefinitions(Map<String, PropertyDefinition> targetPropertyDefinitionMap) {
        targetPropertyDefinitionMap.putAll(propertyDefinitions);
    }

}
