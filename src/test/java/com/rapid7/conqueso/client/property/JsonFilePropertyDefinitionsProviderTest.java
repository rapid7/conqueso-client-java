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

import static com.rapid7.conqueso.client.ConquesoTestHelper.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyType;

public class JsonFilePropertyDefinitionsProviderTest {
    
    @Test
    public void singleValidJson() {
        URL resourceUrl = JsonFilePropertyDefinitionsProviderTest.class.getResource("example-config-class.json");
        JsonFilePropertyDefinitionsProvider provider = new JsonFilePropertyDefinitionsProvider(resourceUrl);
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
        
        assertExampleConfigProperties(results);
    }
    
    @Test
    public void multipleValidJson() {
        URL resourceUrl1 = JsonFilePropertyDefinitionsProviderTest.class.getResource("example-config-class.json");
        URL resourceUrl2 = JsonFilePropertyDefinitionsProviderTest.class.getResource("additional-valid.json");
        JsonFilePropertyDefinitionsProvider provider = new JsonFilePropertyDefinitionsProvider(
                ImmutableList.of(resourceUrl1, resourceUrl2));
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
        
        assertContainsProperty("float1", PropertyType.FLOAT, "3.14", results);
        assertContainsProperty("boolean1", PropertyType.BOOLEAN, "true", results);
    }
    
    @Test
    public void readsSingleFromSystemProperty() {
        URL resourceUrl = JsonFilePropertyDefinitionsProviderTest.class.getResource("example-config-class.json");
        System.setProperty(JsonFilePropertyDefinitionsProvider.JSON_FILE_SYSTEM_PROPERTY, resourceUrl.toExternalForm());
        
        JsonFilePropertyDefinitionsProvider provider = new JsonFilePropertyDefinitionsProvider();
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
        
        System.getProperties().remove(JsonFilePropertyDefinitionsProvider.JSON_FILE_SYSTEM_PROPERTY);
        
        assertExampleConfigProperties(results);
    }
    
    @Test
    public void readsMultipleFromSystemProperty() {
        URL resourceUrl1 = JsonFilePropertyDefinitionsProviderTest.class.getResource("example-config-class.json");
        URL resourceUrl2 = JsonFilePropertyDefinitionsProviderTest.class.getResource("additional-valid.json");
        String combinedUrl = resourceUrl1.toExternalForm() + "," + resourceUrl2.toExternalForm();
        
        System.setProperty(JsonFilePropertyDefinitionsProvider.JSON_FILE_SYSTEM_PROPERTY, combinedUrl);
        
        JsonFilePropertyDefinitionsProvider provider = new JsonFilePropertyDefinitionsProvider();
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
        
        System.getProperties().remove(JsonFilePropertyDefinitionsProvider.JSON_FILE_SYSTEM_PROPERTY);
        
        assertContainsProperty("long1", PropertyType.LONG, "9876543210", results);
        assertContainsProperty("double1", PropertyType.DOUBLE, "6.28", results);
    }
    
    @Test
    public void overridesProperties() {
        URL resourceUrl1 = JsonFilePropertyDefinitionsProviderTest.class.getResource("example-config-class.json");
        URL resourceUrl2 = JsonFilePropertyDefinitionsProviderTest.class.getResource("override-valid.json");
        JsonFilePropertyDefinitionsProvider provider = new JsonFilePropertyDefinitionsProvider(
                ImmutableList.of(resourceUrl1, resourceUrl2));
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
        
        assertContainsProperty("string1", PropertyType.STRING, "foobar", results);
        assertContainsProperty("stringList1", PropertyType.STRING_LIST, "foo,bar,baz,bingo", results);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUrl() throws MalformedURLException {
        URL badUrl = new URL("file:/tmp/foo.baz");
        JsonFilePropertyDefinitionsProvider provider = new JsonFilePropertyDefinitionsProvider(badUrl);
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badFormat1() {
        URL resourceUrl = JsonFilePropertyDefinitionsProviderTest.class.getResource("bad-format1.json");
        JsonFilePropertyDefinitionsProvider provider = new JsonFilePropertyDefinitionsProvider(resourceUrl);
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badFormat2() {
        URL resourceUrl = JsonFilePropertyDefinitionsProviderTest.class.getResource("bad-format2.json");
        JsonFilePropertyDefinitionsProvider provider = new JsonFilePropertyDefinitionsProvider(resourceUrl);
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
    }

}
