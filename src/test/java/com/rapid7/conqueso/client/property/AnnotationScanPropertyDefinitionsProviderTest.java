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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.rapid7.conqueso.client.ConquesoConfig;
import com.rapid7.conqueso.client.ConquesoTestHelper;
import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyType;

public class AnnotationScanPropertyDefinitionsProviderTest {
    
    @Test
    public void standardSuccessfulScan() {
        // This test relies on the fact that the ExampleConfigClass is annotated with @ConquesoConfig
        AnnotationScanPropertyDefinitionsProvider provider = new AnnotationScanPropertyDefinitionsProvider(
                ConquesoConfig.class, 
                Collections.singletonList("com.rapid7.conqueso"));
        
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        
        provider.addPropertyDefinitions(results);
        
        ConquesoTestHelper.assertExampleConfigProperties(results);
    }
    
    @Test
    public void scanWithCustomDelimiter() {
        // This test relies on the fact that the ExampleConfigClass is annotated with @ConquesoConfig
        AnnotationScanPropertyDefinitionsProvider provider = new AnnotationScanPropertyDefinitionsProvider(
                ConquesoConfig.class, 
                Collections.singletonList("com.rapid7.conqueso"), ";;");
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
        
        assertContainsProperty("stringList1", PropertyType.STRING_LIST, "foo;;bar;;baz", results);
        assertContainsProperty("stringSet1",  PropertyType.STRING_SET, "baz;;foo;;bar", results);
        assertContainsProperty("stringMap1", PropertyType.STRING_MAP, "k3=v3;;k1=v1;;k2=v2", results);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void scanWithNoClassesWithAnnotationFound() {
        AnnotationScanPropertyDefinitionsProvider provider = new AnnotationScanPropertyDefinitionsProvider(
                DummyAnnotation.class, 
                Collections.singletonList("com.rapid7.conqueso"));
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void scanWithNoClassesWithBadPackageFound() {
        AnnotationScanPropertyDefinitionsProvider provider = new AnnotationScanPropertyDefinitionsProvider(
                ConquesoConfig.class, 
                Collections.singletonList("com.bad.package"));
        Map<String, PropertyDefinition> results = Maps.newHashMap();
        provider.addPropertyDefinitions(results);
    }
        
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DummyAnnotation {
        
    }
    
    

}
