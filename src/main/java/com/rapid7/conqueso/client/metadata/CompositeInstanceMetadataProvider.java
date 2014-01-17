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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.rapid7.conqueso.client.InstanceMetadataProvider;

/**
 * Instance metadata provider that combines the results of multiple metadata providers together.
 * 
 * The CompositeInstanceMetadataProvider is constructed with a list of {@link InstanceMetadataProvider}
 * instances. The resulting instance metadata map provided by the composite will merge the maps from each provider,
 * with the later providers' keys taking precedence for conflicts.
 */
public class CompositeInstanceMetadataProvider implements InstanceMetadataProvider {
    
    private final ImmutableList<InstanceMetadataProvider> childProviders;
    
    public CompositeInstanceMetadataProvider(InstanceMetadataProvider...providers) {
        this(Arrays.asList(providers));
    }
    
    public CompositeInstanceMetadataProvider(List<InstanceMetadataProvider> providers) {
        this.childProviders = ImmutableList.copyOf(providers);
    }

    @Override
    public Map<String, String> getInstanceMetadata() {
        Map<String, String> results = Maps.newHashMap();
        
        for (InstanceMetadataProvider childProvider : childProviders) {
            results.putAll(childProvider.getInstanceMetadata());
        }
        
        return results;
    }

}
