/***************************************************************************
 * COPYRIGHT (C) 2012-2014, Rapid7 LLC, Boston, MA, USA.
 * All rights reserved. This material contains unpublished, copyrighted
 * work including confidential and proprietary information of Rapid7.
 **************************************************************************/
package com.rapid7.conqueso.client.metadata;

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
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.netflix.config.FixedDelayPollingScheduler;
import com.rapid7.conqueso.client.InstanceMetadataProvider;

/**
 * Provider that will incorporate instance metadata set as system properties. All system properties
 * with keys with the prefix <code>"conqueso.metadata."</code> will be included in the instance metadata. 
 * For example, if your application is launched with:
 * <pre>-Dconqueso.metadata.app.name=reporting-service</pre>
 * the key/value pair "app.name=reporting-service" will be included in the instance metadata.
 */
public class SystemPropertiesInstanceMetadataProvider implements InstanceMetadataProvider {
    
    /**
     * The prefix used to identify instance metadata system properties. The 
     */
    public static final String SYSTEM_PROPERTY_KEY_PREFIX = "conqueso.metadata.";
    
    private static final ImmutableMap<String, String> SYSTEM_PROPERTY_KEY_TRANSLATION = 
            ImmutableMap.<String, String>builder()
            // Translate the Archaius polling interval configuration to the conqueso poll interval
            .put(FixedDelayPollingScheduler.DELAY_PROPERTY, "conqueso.poll.interval")
            .build();

    @Override
    public Map<String, String> getInstanceMetadata() {
        return getInstanceMetadataFromProperties(System.getProperties());
    }
    
    @VisibleForTesting
    Map<String, String> getInstanceMetadataFromProperties(Properties properties) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
               
        Set<String> keys = properties.stringPropertyNames();

        for (String key : keys) {
            String value = properties.getProperty(key);
            if (SYSTEM_PROPERTY_KEY_TRANSLATION.containsKey(key)) {
                builder.put(SYSTEM_PROPERTY_KEY_TRANSLATION.get(key), value);
            } else if (key.startsWith(SYSTEM_PROPERTY_KEY_PREFIX)) {
                String strippedKey = key.substring(SYSTEM_PROPERTY_KEY_PREFIX.length());
                builder.put(strippedKey, value);
            }
        }

        return builder.build();
    }

}
