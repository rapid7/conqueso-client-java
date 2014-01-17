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

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.rapid7.conqueso.client.ExampleConfigClass;
import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyType;

public class IntrospectorPropertyDefinitionProviderTest {
    
    @Test
    public void findsAllProperties() {
        IntrospectorPropertyDefinitionsProvider introspector = 
                new IntrospectorPropertyDefinitionsProvider(ExampleConfigClass.class);
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        introspector.addPropertyDefinitions(results);
        
        assertEquals(ExampleConfigClass.class.getDeclaredFields().length, results.size());
        assertContainsProperty("string1", PropertyType.STRING, "foo", results);
        assertContainsProperty("string2", PropertyType.STRING, "bar", results);
        assertContainsProperty("string3", PropertyType.STRING, "", results);
        assertContainsProperty("int1", PropertyType.INT, "42", results);
        
        assertContainsProperty("stringList1", PropertyType.STRING_LIST, "foo,bar,baz", results);
        assertContainsProperty("stringSet1",  PropertyType.STRING_SET, "baz,foo,bar", results);
        assertContainsProperty("stringMap1", PropertyType.STRING_MAP, "k3=v3,k1=v1,k2=v2", results);
    }
    
    @Test
    public void alternateDelimiter() {
        IntrospectorPropertyDefinitionsProvider introspector = 
                new IntrospectorPropertyDefinitionsProvider(Collections.<Class<?>>singleton(ExampleConfigClass.class), ";;");
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        introspector.addPropertyDefinitions(results);
        
        assertContainsProperty("stringList1", PropertyType.STRING_LIST, "foo;;bar;;baz", results);
        assertContainsProperty("stringSet1",  PropertyType.STRING_SET, "baz;;foo;;bar", results);
        assertContainsProperty("stringMap1", PropertyType.STRING_MAP, "k3=v3;;k1=v1;;k2=v2", results);
    }
    
    private static void assertContainsProperty(String propName, PropertyType propType, String defaultValue, 
            Map<String, PropertyDefinition> results) {
        assertEquals(new PropertyDefinition(propName, propType, defaultValue), results.get(propName));
    }

}
