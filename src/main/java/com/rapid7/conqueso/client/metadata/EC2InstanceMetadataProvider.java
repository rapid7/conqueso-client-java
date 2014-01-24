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

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.io.CharStreams;
import com.rapid7.conqueso.client.InstanceMetadataProvider;

/**
 * Provider that will incorporate instance metadata obtained from the Amazon EC2 Instance Metadata Service.
 * <br>
 * Note: This class is a re-implementation of the features in the AWS SDK's EC2MetadataUtils and 
 * EC2MetadataClient classes. This was necessary to avoid adding a dependency on the aws-java-sdk artifact.
 * <br>
 * More information about Amazon EC2 Metadata
 * @see <a href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AESDG-chapter-instancedata.html">Amazon EC2 User Guide >> Instance Metadata</a>
 */
public class EC2InstanceMetadataProvider implements InstanceMetadataProvider {
    
    /** Default endpoint for the Amazon EC2 Instance Metadata Service. */
    private static final String EC2_METADATA_SERVICE_URL = "http://169.254.169.254";
    
    public static final String EC2_METADATA_ROOT = "/latest/meta-data";
    
    public static final ImmutableSet<MetadataLookup> DEFAULT_METADATA = ImmutableSet.<MetadataLookup>builder()
            .add(new MetadataLookup("ami-id",            EC2_METADATA_ROOT + "/ami-id"))
            .add(new MetadataLookup("instance-id",       EC2_METADATA_ROOT + "/instance-id"))
            .add(new MetadataLookup("instance-type",     EC2_METADATA_ROOT + "/instance-type"))
            .add(new MetadataLookup("local-hostname",    EC2_METADATA_ROOT + "/local-hostname"))
            .add(new MetadataLookup("local-ipv4",        EC2_METADATA_ROOT + "/local-ipv4"))
            .add(new MetadataLookup("public-hostname",   EC2_METADATA_ROOT + "/public-hostname"))
            .add(new MetadataLookup("public-ipv4",       EC2_METADATA_ROOT + "/public-ipv4"))
            .add(new MetadataLookup("availability-zone", EC2_METADATA_ROOT + "/placement/availability-zone"))
            .add(new MetadataLookup("security-groups",   EC2_METADATA_ROOT + "/security-groups"))
            .build();
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EC2InstanceMetadataProvider.class);
    
    private final ImmutableSet<MetadataLookup> metadataLookups;
    
    /**
     * Create an instance of the EC2InstanceMetadataProvider that will retrieve the default set of metadata
     * properties from EC2.
     */
    public EC2InstanceMetadataProvider() {
        this(Collections.<MetadataLookup>emptySet());
    }
    
    /**
     * Create an instance of the EC2InstanceMetadataProvider that will retrieve the default set of metadata
     * properties and the additional specified metadata from EC2.
     */
    public EC2InstanceMetadataProvider(Iterable<MetadataLookup> additionalMetadata) {
        Builder<MetadataLookup> builder = ImmutableSet.<MetadataLookup>builder();
        builder.addAll(DEFAULT_METADATA);
        builder.addAll(additionalMetadata);
        this.metadataLookups = builder.build();
    }
    
    @Override
    public Map<String, String> getInstanceMetadata() {
        if (!isMetadataServiceReachable()) {
            return Collections.emptyMap();
        }
        
        return readMetadataFromService();
    }
    
    private boolean isMetadataServiceReachable() {
        try {
            String response = readResource("/");
            return !Strings.isNullOrEmpty(response);
        } catch (IOException e) {
            // Failed to connect to the EC2 Metadata service
            return false;
        }
    }
    
    private Map<String, String> readMetadataFromService() {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        
        for (MetadataLookup lookup : metadataLookups) {
            try {
                String value = readResource(lookup.getResourcePath());
                if (!Strings.isNullOrEmpty(value)) {
                    builder.put(lookup.getConfiglyKey(), value);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to read EC2 metadata from path " + lookup.getResourcePath(), e);
            }
        }

        return builder.build();
    }
    
    private String readResource(String resourcePath) throws IOException {
        HttpURLConnection connection = openConnection(resourcePath);
        return readResponse(connection);
    }
    
    @VisibleForTesting
    protected HttpURLConnection openConnection(String resourcePath) throws IOException {
        URL url = new URL(EC2_METADATA_SERVICE_URL + resourcePath);

        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(1000 * 2);
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.connect();
        
        return connection;
    }
    
    private String readResponse(HttpURLConnection connection) throws IOException {
        if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            LOGGER.warn("The requested metadata is not found at " + connection.getURL());
            return null;
        }
        
        InputStreamReader reader = new InputStreamReader(connection.getInputStream(), Charsets.UTF_8);
        try {
            return CharStreams.toString(reader);
        } finally {
            reader.close();
        }
    }
    
    public static final class MetadataLookup {
        private final String configlyKey;
        private final String resourcePath;
        
        public MetadataLookup(String configlyKey, String resourcePath) {
            this.configlyKey = checkNotNull(configlyKey, "configlyKey");
            this.resourcePath = checkNotNull(resourcePath, "resourcePath");
        }

        public String getConfiglyKey() {
            return configlyKey;
        }

        public String getResourcePath() {
            return resourcePath;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(configlyKey, resourcePath);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MetadataLookup other = (MetadataLookup) obj;
            return Objects.equal(configlyKey, other.configlyKey) &&
                    Objects.equal(resourcePath, other.resourcePath);
        }
    }

}
