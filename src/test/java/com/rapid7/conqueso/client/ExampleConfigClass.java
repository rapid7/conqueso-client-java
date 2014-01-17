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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringMapProperty;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.DynamicStringSetProperty;

@SuppressWarnings("unused")
public class ExampleConfigClass {
    
    private static final DynamicStringProperty STRING1 = 
            DynamicPropertyFactory.getInstance().getStringProperty("string1", "foo");
    
    private static final DynamicStringProperty STRING2 = 
            DynamicPropertyFactory.getInstance().getStringProperty("string2", "bar");
    
    private static final DynamicStringProperty STRING_NULL = 
            DynamicPropertyFactory.getInstance().getStringProperty("string3", null);
    
    private static final DynamicIntProperty INT1 = 
            DynamicPropertyFactory.getInstance().getIntProperty("int1", 42);
    
    
    private static final DynamicStringListProperty STRING_LIST1 = new DynamicStringListProperty("stringList1", 
            ImmutableList.of("foo", "bar", "baz"));
    
    private static final DynamicStringSetProperty STRING_SET1 = new DynamicStringSetProperty("stringSet1",
            // Funky order to match hashing order
            ImmutableSet.of("baz", "foo", "bar"));
    
    private static final DynamicStringMapProperty STRING_MAP1 = new DynamicStringMapProperty("stringMap1",
            // Funky order to match hashing order
            ImmutableMap.of("k3","v3","k1","v1","k2","v2"));

}
