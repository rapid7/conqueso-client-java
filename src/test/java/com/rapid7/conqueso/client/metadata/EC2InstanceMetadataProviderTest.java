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
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rapid7.conqueso.client.metadata.EC2InstanceMetadataProvider;
import com.rapid7.conqueso.client.metadata.EC2InstanceMetadataProvider.MetadataLookup;

public class EC2InstanceMetadataProviderTest {
    
    @Test
    public void returnsAllResponses() {
        Map<String, String> response = createDefaultResponse();
        
        StubEC2InstanceMetadataProvider provider = new StubEC2InstanceMetadataProvider(response);
        Map<String, String> result = provider.getInstanceMetadata();
        assertEquals(EC2InstanceMetadataProvider.DEFAULT_METADATA.size(), result.size());
    }
    
    @Test
    public void skipsEmptyResult() {
        Map<String, String> response = createDefaultResponse();
        // Override the response for ami-id
        response.put(EC2InstanceMetadataProvider.EC2_METADATA_ROOT + "/ami-id", "");
        
        Map<String, String> result = getInstanceMetadata(response);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertFalse(result.containsKey("ami-id"));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void throwsNotFoundResult() {
        Map<String, String> response = createDefaultResponse();
        // Remove the response for ami-id
        response.remove(EC2InstanceMetadataProvider.EC2_METADATA_ROOT + "/ami-id");
        
        getInstanceMetadata(response);
    }

    @Test
    public void skipsIfNotReachable() {
        ThrowingProvider throwingProvider = new ThrowingProvider();
        
        Map<String, String> result = throwingProvider.getInstanceMetadata();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(1, throwingProvider.getRequestedResourcePaths().size());
    }
    
    @Test
    public void queriesCustomValues() {
        Map<String, String> response = createDefaultResponse();
        response.put("custom-query1", "custom-response1");
        response.put("custom-query2", "custom-response2");
        
        List<MetadataLookup> customLookups = Lists.newArrayList(new MetadataLookup("query1-key", "custom-query1"),
                new MetadataLookup("query2-key", "custom-query2"));
        
        StubEC2InstanceMetadataProvider provider = new StubEC2InstanceMetadataProvider(response, customLookups);
        Map<String, String> result = provider.getInstanceMetadata();
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("custom-response1", result.get("query1-key"));
        assertEquals("custom-response2", result.get("query2-key"));
        assertTrue(provider.getRequestedResourcePaths().contains("custom-query1"));
        assertTrue(provider.getRequestedResourcePaths().contains("custom-query2"));
    }
    
    private Map<String, String> createDefaultResponse() {
        Map<String, String> response = Maps.newHashMap();
        
        // Ensure the isMetadataServiceReachable() returns fine
        response.put("/", "stuff");
        
        for (MetadataLookup lookup : EC2InstanceMetadataProvider.DEFAULT_METADATA) {
            response.put(lookup.getResourcePath(), lookup.getConfiglyKey() + "-response");
        }
        
        return response;
    }
    
    private Map<String, String> getInstanceMetadata(Map<String, String> resourcePathToResponse) {
        return new StubEC2InstanceMetadataProvider(resourcePathToResponse).getInstanceMetadata();
    }
    
    private class StubEC2InstanceMetadataProvider extends EC2InstanceMetadataProvider {
        
        private final Map<String, String> resourcePathToResponse;       
        private List<String> requestedResourcePaths = Lists.newArrayList();

        public StubEC2InstanceMetadataProvider(Map<String, String> resourcePathToResponse) {
            super();
            this.resourcePathToResponse = resourcePathToResponse;
        }
        
        public StubEC2InstanceMetadataProvider(Map<String, String> resourcePathToResponse, 
                Iterable<MetadataLookup> additionalMetadata) {
            super(additionalMetadata);
            this.resourcePathToResponse = resourcePathToResponse;
        }

        @Override
        protected HttpURLConnection openConnection(String resourcePath) throws IOException {
            requestedResourcePaths.add(resourcePath);
            String response = resourcePathToResponse.get(resourcePath);
            
            HttpURLConnection mockConnection = mock(HttpURLConnection.class);
            
            int responseCode = (response == null ? HttpURLConnection.HTTP_NOT_FOUND : HttpURLConnection.HTTP_OK);
            byte[] responseBytes = (response == null ? new byte[0] : response.getBytes(Charsets.UTF_8));
            
            when(mockConnection.getResponseCode()).thenReturn(Integer.valueOf(responseCode));
            when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(responseBytes));

            return mockConnection;
        }

        public List<String> getRequestedResourcePaths() {
            return requestedResourcePaths;
        }
    }
    
    private class ThrowingProvider extends EC2InstanceMetadataProvider {
        private List<String> requestedResourcePaths = Lists.newArrayList();
        
        @Override
        protected HttpURLConnection openConnection(String resourcePath) throws IOException {
            requestedResourcePaths.add(resourcePath);
            
            throw new IOException("Fail.");
        }

        public List<String> getRequestedResourcePaths() {
            return requestedResourcePaths;
        }
    }

}
