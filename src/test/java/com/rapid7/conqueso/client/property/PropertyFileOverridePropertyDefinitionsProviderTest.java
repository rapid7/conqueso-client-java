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
import static org.junit.Assert.*;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyDefinitionsProvider;
import com.rapid7.conqueso.client.PropertyType;

public class PropertyFileOverridePropertyDefinitionsProviderTest {
    
    @Test
    public void singleProperties() {
        URL resourceUrl = PropertyFileOverridePropertyDefinitionsProviderTest.class
                .getResource("override.properties");
        PropertyFileOverridePropertyDefinitionsProvider provider = new PropertyFileOverridePropertyDefinitionsProvider(resourceUrl);
        
        PropertyDefinitionsProvider testProvider = compose(provider);
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        testProvider.addPropertyDefinitions(results);

        assertContainsProperty("foo", PropertyType.STRING, "bar", "foo description", results);
        assertContainsProperty("baz", PropertyType.INT, "84", null, results);
        assertContainsProperty("bingo", PropertyType.STRING_LIST, "bingo", null, results);
        assertContainsProperty("original", PropertyType.STRING, "still-good", "original description", results);
    }
    
    @Test
    public void multipleProperties() {
        URL resourceUrl1 = PropertyFileOverridePropertyDefinitionsProviderTest.class
                .getResource("override.properties");
        URL resourceUrl2 = PropertyFileOverridePropertyDefinitionsProviderTest.class
                .getResource("override2.properties");
        PropertyFileOverridePropertyDefinitionsProvider provider = new PropertyFileOverridePropertyDefinitionsProvider(
                ImmutableList.of(resourceUrl1, resourceUrl2));
        
        PropertyDefinitionsProvider testProvider = compose(provider);
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        testProvider.addPropertyDefinitions(results);
        
        assertContainsProperty("foo", PropertyType.STRING, "bar", "foo description", results);
        assertContainsProperty("baz", PropertyType.INT, "84", null, results);
        assertContainsProperty("bingo", PropertyType.STRING_LIST, "bazingo", null, results);
        assertContainsProperty("original", PropertyType.STRING, "still-good", "original description", results);
    }
    
    @Test
    public void readsSingleFromSystemProperty() {
        URL resourceUrl = PropertyFileOverridePropertyDefinitionsProviderTest.class
                .getResource("override.properties");
        
        System.setProperty(PropertyFileOverridePropertyDefinitionsProvider.PROPERTIES_OVERRIDE_FILE_SYSTEM_PROPERTY, 
                resourceUrl.toExternalForm());
        
        PropertyFileOverridePropertyDefinitionsProvider provider = new PropertyFileOverridePropertyDefinitionsProvider();
        
        PropertyDefinitionsProvider testProvider = compose(provider);
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        testProvider.addPropertyDefinitions(results);
        
        System.getProperties().remove(PropertyFileOverridePropertyDefinitionsProvider.PROPERTIES_OVERRIDE_FILE_SYSTEM_PROPERTY);

        assertContainsProperty("foo", PropertyType.STRING, "bar", "foo description", results);
        assertContainsProperty("baz", PropertyType.INT, "84", null, results);
        assertContainsProperty("bingo", PropertyType.STRING_LIST, "bingo", null, results);
        assertContainsProperty("original", PropertyType.STRING, "still-good", "original description", results);
    }
    
    @Test
    public void readsMultipleFromSystemProperty() {
        URL resourceUrl1 = PropertyFileOverridePropertyDefinitionsProviderTest.class
                .getResource("override.properties");
        URL resourceUrl2 = PropertyFileOverridePropertyDefinitionsProviderTest.class
                .getResource("override2.properties");
        
        String combinedUrl = resourceUrl1.toExternalForm() + "," + resourceUrl2.toExternalForm();
        
        System.setProperty(PropertyFileOverridePropertyDefinitionsProvider.PROPERTIES_OVERRIDE_FILE_SYSTEM_PROPERTY, 
                combinedUrl);
        
        PropertyFileOverridePropertyDefinitionsProvider provider = new PropertyFileOverridePropertyDefinitionsProvider();
        
        PropertyDefinitionsProvider testProvider = compose(provider);
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        testProvider.addPropertyDefinitions(results);
        
        System.getProperties().remove(PropertyFileOverridePropertyDefinitionsProvider.PROPERTIES_OVERRIDE_FILE_SYSTEM_PROPERTY);

        assertContainsProperty("foo", PropertyType.STRING, "bar", "foo description", results);
        assertContainsProperty("baz", PropertyType.INT, "84", null, results);
        assertContainsProperty("bingo", PropertyType.STRING_LIST, "bazingo", null, results);
        assertContainsProperty("original", PropertyType.STRING, "still-good", "original description", results);
    }
    
    @Test
    public void skipsUnknownProperties() {
        URL resourceUrl = PropertyFileOverridePropertyDefinitionsProviderTest.class
                .getResource("override.properties");
        PropertyFileOverridePropertyDefinitionsProvider provider = new PropertyFileOverridePropertyDefinitionsProvider(resourceUrl);
        
        List<PropertyDefinition> originalProperties = Lists.newArrayList();
        originalProperties.add(new PropertyDefinition("foo", PropertyType.STRING, "foo", "foo description"));
        
        CustomPropertyDefinitionsProvider originalProvider = new CustomPropertyDefinitionsProvider(originalProperties);
        PropertyDefinitionsProvider testProvider = new CompositePropertyDefinitionsProvider(originalProvider, provider);
        
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        testProvider.addPropertyDefinitions(results);

        assertContainsProperty("foo", PropertyType.STRING, "bar", "foo description", results);
        assertFalse(results.containsKey("baz"));
    }
    
    private PropertyDefinitionsProvider compose(PropertyFileOverridePropertyDefinitionsProvider testProvider) {
        List<PropertyDefinition> originalProperties = Lists.newArrayList();
        originalProperties.add(new PropertyDefinition("foo", PropertyType.STRING, "foo", "foo description"));
        originalProperties.add(new PropertyDefinition("baz", PropertyType.INT, "42", null));
        originalProperties.add(new PropertyDefinition("bingo", PropertyType.STRING_LIST, "bingo", null));
        originalProperties.add(new PropertyDefinition("original", PropertyType.STRING, "still-good", 
                "original description"));
        
        CustomPropertyDefinitionsProvider originalProvider = new CustomPropertyDefinitionsProvider(originalProperties);
        return new CompositePropertyDefinitionsProvider(originalProvider, testProvider);
    }

}
