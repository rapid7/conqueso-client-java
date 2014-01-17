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
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyDefinitionsProvider;

/**
 * Property definitions provider that combines the results of multiple property definition providers together.
 * 
 * The CompositePropertyDefinitionsProvider is constructed with a list of {@link PropertyDefinitionsProvider}
 * instances. The resulting property definition map provided by the composite will merge the results from each
 * provider in order specified in the construction.
 */
public class CompositePropertyDefinitionsProvider implements PropertyDefinitionsProvider {
    
    private final ImmutableList<PropertyDefinitionsProvider> childProviders;
    
    public CompositePropertyDefinitionsProvider(PropertyDefinitionsProvider...providers) {
        this(Arrays.asList(providers));
    }
    
    public CompositePropertyDefinitionsProvider(List<PropertyDefinitionsProvider> providers) {
        this.childProviders = ImmutableList.copyOf(providers);
    }

    @Override
    public void addPropertyDefinitions(Map<String, PropertyDefinition> targetPropertyDefinitionMap) {
        for (PropertyDefinitionsProvider provider : childProviders) {
            provider.addPropertyDefinitions(targetPropertyDefinitionMap);
        }
    }

}
