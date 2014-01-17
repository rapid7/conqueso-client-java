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

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.rapid7.conqueso.client.metadata.CompositeInstanceMetadataProvider;
import com.rapid7.conqueso.client.metadata.CustomInstanceMetadataProvider;

public class CompositeInstanceMetadataProviderTest {

    @Test
    public void returnsData() {
       Map<String, String> testValues1 = ImmutableMap.of("k1", "v1");
       Map<String, String> testValues2 = ImmutableMap.of("k2", "v2");
       Map<String, String> testValues3 = ImmutableMap.of("k3", "v3");
       
       Map<String, String> expected = Maps.newHashMap();
       expected.putAll(testValues1);
       expected.putAll(testValues2);
       expected.putAll(testValues3);
       
       CompositeInstanceMetadataProvider provider = new CompositeInstanceMetadataProvider(
               new CustomInstanceMetadataProvider(testValues1),
               new CustomInstanceMetadataProvider(testValues2),
               new CustomInstanceMetadataProvider(testValues3));
       
       Map<String, String> result = provider.getInstanceMetadata();
       assertEquals(expected, result);
    }
    
    @Test
    public void overwritesValues() {
       Map<String, String> testValues1 = ImmutableMap.of("k1", "v1", "k2", "v2");
       Map<String, String> testValues2 = ImmutableMap.of("k2", "v22", "k3", "v3");
       Map<String, String> testValues3 = ImmutableMap.of("k3", "v33", "k4", "v4");
       
       Map<String, String> expected = Maps.newHashMap();
       expected.put("k1", "v1");
       expected.put("k2", "v22");
       expected.put("k3", "v33");
       expected.put("k4", "v4");
       
       CompositeInstanceMetadataProvider provider = new CompositeInstanceMetadataProvider(
               new CustomInstanceMetadataProvider(testValues1),
               new CustomInstanceMetadataProvider(testValues2),
               new CustomInstanceMetadataProvider(testValues3));
       
       Map<String, String> result = provider.getInstanceMetadata();
       assertEquals(expected, result);
    }
    
}
