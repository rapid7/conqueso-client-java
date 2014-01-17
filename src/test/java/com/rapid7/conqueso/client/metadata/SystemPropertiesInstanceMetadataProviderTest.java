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
package com.rapid7.conqueso.client.metadata;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.netflix.config.FixedDelayPollingScheduler;
import com.rapid7.conqueso.client.metadata.SystemPropertiesInstanceMetadataProvider;

public class SystemPropertiesInstanceMetadataProviderTest {
    
    @Test
    public void findsConfiglyProperties() {
        String key1 = "test.key1";
        String value1 = "test.value1";
        String prefixedKey1 = SystemPropertiesInstanceMetadataProvider.SYSTEM_PROPERTY_KEY_PREFIX + key1;
        
        String key2 = "test.key2";
        String value2 = "test.value2";
        String prefixedKey2 = SystemPropertiesInstanceMetadataProvider.SYSTEM_PROPERTY_KEY_PREFIX + key2;
        
        Map<String, String> results = getMetadataFromProperties(prefixedKey1, value1, prefixedKey2, value2);
        assertTrue(results.containsKey(key1));
        assertEquals(value1, results.get(key1));
        
        assertTrue(results.containsKey(key2));
        assertEquals(value2, results.get(key2));
    }
    
    @Test
    public void skipsNonConfiglyProperties() {
        String key1 = "test.key1";
        String value1 = "test.value1";
        String prefixedKey1 = SystemPropertiesInstanceMetadataProvider.SYSTEM_PROPERTY_KEY_PREFIX + key1;
        
        String key2 = "test.key2";
        String value2 = "test.value2";
        
        Map<String, String> results = getMetadataFromProperties(prefixedKey1, value1, key2, value2);
        assertTrue(results.containsKey(key1));
        assertEquals(value1, results.get(key1));
        
        assertFalse(results.containsKey(key2));
    }
    
    @Test
    public void translatesKeys() {
        String srcKey = FixedDelayPollingScheduler.DELAY_PROPERTY;
        String translatedKey = "conqueso.poll.interval";
        String value = "15";
        
        Map<String, String> results = getMetadataFromProperties(srcKey, value);
        
        assertTrue(results.containsKey(translatedKey));
        assertEquals(value, results.get(translatedKey));
    }
    
    private Map<String, String> getMetadataFromProperties(String...keyValues) {
        return new SystemPropertiesInstanceMetadataProvider()
            .getInstanceMetadataFromProperties(createProperties(keyValues));
    }
    
    private Properties createProperties(String...keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Uneven number of key/value pairs");
        }
        Map<String, String> map = Maps.newHashMap();
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            map.put(key, value);
        }
        return createProperties(map);
    }
    
    private Properties createProperties(Map<String, String> map) {
        Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }

}
