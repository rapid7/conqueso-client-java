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

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyType;

public class CustomPropertyDefinitionsProviderTest {
    
    @Test
    public void addPropertyDefinitions() {
        PropertyDefinition prop1 = new PropertyDefinition("foo", PropertyType.STRING, "bar", null);
        PropertyDefinition prop2 = new PropertyDefinition("the-answer", PropertyType.INT, "42", null);
        
        CustomPropertyDefinitionsProvider provider = new CustomPropertyDefinitionsProvider(prop1, prop2);
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
        
        assertEquals(prop1, results.get("foo"));
        assertEquals(prop2, results.get("the-answer"));
    }

}
