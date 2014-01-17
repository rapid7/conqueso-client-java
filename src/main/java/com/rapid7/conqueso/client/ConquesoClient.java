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

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.netflix.config.DynamicListProperty;
import com.netflix.config.sources.URLConfigurationSource;
import com.rapid7.conqueso.client.metadata.CompositeInstanceMetadataProvider;
import com.rapid7.conqueso.client.metadata.CustomInstanceMetadataProvider;
import com.rapid7.conqueso.client.metadata.EC2InstanceMetadataProvider;
import com.rapid7.conqueso.client.metadata.SystemPropertiesInstanceMetadataProvider;
import com.rapid7.conqueso.client.property.CompositePropertyDefinitionsProvider;
import com.rapid7.conqueso.client.property.CustomPropertyDefinitionsProvider;
import com.rapid7.conqueso.client.property.IntrospectorPropertyDefinitionsProvider;
import com.rapid7.conqueso.client.property.JsonFilePropertyDefinitionsProvider;
import com.rapid7.conqueso.client.property.PropertyFileOverridePropertyDefinitionsProvider;

/**
 * Class used to populate the Conqueso Server with information about a client application instance, as
 * well as provide methods to query the Conqueso Server. Instances are created using the {@link #initializer()}
 * method.
 * <p>
 * On initialization of the ConquesoClient, metadata about the client application instance and the Archaius
 * properties used by the application are transmitted to the Conqueso Server. Methods to specify and customize
 * the instance metadata and property definitions are available on the {@link Initializer} class.
 * <p>
 * The behavior of the ConquesoClient should provide reasonable defaults with minimal customization. A consumer
 * should minimally specify the classes containing their Archaius properties as static fields. For example:
 * <pre>
 * ConquesoClient.initializer()
 *     .withConfigurationClasses(AppConfig.class, DbConfig.class, QueueConfig.class)
 *     .initialize();
 * </pre>
 * 
 * @see Initializer
 */
public class ConquesoClient {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConquesoClient.class);
    
    private final URL conquesoUrl;
    
    /**
     * Create the Initializer object used to establish a connection to the Conqueso Server.
     * @return the Initializer to use to configure the communication with Conqueso.
     */
    public static Initializer initializer() {
        return new Initializer();
    }
    
    /**
     * Class used to set up a connection to Conqueso, and send the server the instance metadata and
     * default property values.
     */
    public static class Initializer {
        
        private String conquesoUrl = null;
        
        private InstanceMetadataProvider instanceMetadataProvider = null;
        
        private PropertyDefinitionsProvider propertyDefinitionsProvider = null;
        
        private List<Class<?>> configurationClasses;
        private String collectionDelimiter;
        
        /**
         * Initialize the Conqueso Client. This will establish a connection to the Server to send
         * the instance's metadata and property definitions.
         * 
         * @return the initialized ConquesoClient
         * @throws IOException if there was an issue communicating with the Conqueso Server
         */
        public ConquesoClient initialize() throws IOException {
            URL url = getConquesoUrl();            
            
            Map<String, String> instanceMetadata = getInstanceMetadata();
            
            Set<PropertyDefinition> propertyDefs = getPropertyDefinitions();
            
            ConquesoClient client = new ConquesoClient(url);
            client.postInitialInstanceInfo(instanceMetadata, propertyDefs);
            
            return client;
        }
        
        /**
         * Optionally set the URL of the Conqueso server. By default, this value will be read from the
         * "archaius.configurationSource.additionalUrls" system property.
         * 
         * @param conquesoUrl the alternate URL to connect
         * @return the initializer for method chaining
         */
        public Initializer withConquesoUrl(String conquesoUrl) {
            checkState(conquesoUrl == null, "Conqueso URL already configured");
            this.conquesoUrl = checkNotNull(conquesoUrl, "conquesoUrl");
            return this;
        }
        
        /**
         * Optionally set the provider to use to generate the instance metadata for this instance. This
         * metadata will be transmitted to the Conqueso server to attach to this instance.
         * If not specified, the default provider will be created via the 
         * {@link #createDefaultInstanceDataProvider()} method.
         * 
         * @param instanceMetadataProvider the provider used to generate the instance metadata
         * @return the initializer for method chaining
         */
        public Initializer withInstanceData(InstanceMetadataProvider instanceMetadataProvider) {
            checkState(instanceMetadataProvider == null, "Instance data already configured");
            this.instanceMetadataProvider = checkNotNull(instanceMetadataProvider, "instanceMetadataProvider");
            return this;
        }
        
        /**
         * Optionally set static instance metadata for this instance. This  metadata will be transmitted 
         * to the Conqueso server to attach to this instance.
         * 
         * @param instanceMetadata the metadata to associate with this instance 
         * @return the initializer for method chaining
         */
        public Initializer withInstanceData(Map<String, String> instanceMetadata) {
            return withInstanceData(new CustomInstanceMetadataProvider(instanceMetadata));
        }
        
        /**
         * Configure the client to avoid transmitting instance metadata to the Conqueso server for this instance.
         * 
         * @return the initializer for method chaining
         */
        public Initializer skipReportingInstanceData() {
            return withInstanceData(Collections.<String, String>emptyMap());
        }
        
        /**
         * Optionally set the provider to use to generate property definitions for this instance. These
         * property definitions will be transmitted to the Conqueso server to attach to this instance.
         * If not specified, the default provider will be created via the 
         * {@link #createDefaultPropertyDefinitionsProvider()} method.
         * 
         * @param propertyDefinitionsProvider the provider used to generate the property definitions 
         * @return the initializer for method chaining
         */
        public Initializer withPropertyDefinitions(PropertyDefinitionsProvider propertyDefinitionsProvider) {
            checkState(propertyDefinitionsProvider == null, "Property definitions provider already configured");
            this.propertyDefinitionsProvider = propertyDefinitionsProvider;
            return this;
        }
        
        /**
         * Configure the client to avoid transmitting property definitions to the Conqueso server for this instance.
         * 
         * @return the initializer for method chaining
         */
        public Initializer withNoProperties() {
            checkState(configurationClasses == null, "Configuration classes already configured");
            return withPropertyDefinitions(new CustomPropertyDefinitionsProvider());
        }
        
        /**
         * Specify the configuration classes used in your app containing the Archaius dynamic properties.
         * The classes will be scanned to discover the static Archaius dynamic property fields, and the 
         * definition of those properties will be transmitted to the Conqueso server. The definitions will
         * be used to set up the properties to return to Archaius, the type of fields, and the default values.  
         * 
         * @param configurationClasses the configuration classes to scan for Archaius properties
         * @return the initializer for method chaining
         */
        public Initializer withConfigurationClasses(Class<?>...configurationClasses) {
            checkState(configurationClasses == null, "Configuration classes already configured");
            checkArgument(configurationClasses.length > 0, "No configuration classes specified");
            this.configurationClasses = Arrays.asList(configurationClasses);
            return this;
        }
        
        /**
         * Set the delimiter used for separating Archaius collection property values. This defaults to
         * {@link DynamicListProperty#DEFAULT_DELIMITER} ( , ).
         * 
         * @param collectionDelimiter the delimiter to use for separating Archaius collection properties
         * @return the initializer for method chaining
         */
        public Initializer withCollectionPropertyDelimiter(String collectionDelimiter) {
            checkState(collectionDelimiter == null, "Collection property delimiter already configured");
            checkArgument(!Strings.isNullOrEmpty(collectionDelimiter), "collectionDelimiter");
            this.collectionDelimiter = collectionDelimiter;
            return this;
        }
        
        /**
         * Create the default {@link InstanceMedataProvider} to use for a ConquesoClient. This provider
         * can be composed with additional custom providers using the {@link CompositeInstanceMetadataProvider} class,
         * then configured into the client with the {@link #withInstanceData(InstanceMetadataProvider)} method.
         * @return the default implementation of InstanceMetadataProvider
         */
        public static InstanceMetadataProvider createDefaultInstanceDataProvider() {
            return new CompositeInstanceMetadataProvider(new EC2InstanceMetadataProvider(), 
                    new SystemPropertiesInstanceMetadataProvider());
        }
        
        /**
         * Create the default {@link PropertyDefinitionsProvider} to use for a ConquesoClient. This provider
         * can be composed with additional custom providers using the {@link CompositePropertyDefinitionsProvider} 
         * class, then configured into the client with the 
         * {@link #withPropertyDefinitions(PropertyDefinitionsProvider)} method.
         * @return the default implementation of InstanceMetadataProvider
         */
        public PropertyDefinitionsProvider createDefaultPropertyDefinitionsProvider() {
            IntrospectorPropertyDefinitionsProvider introspector = 
                    (collectionDelimiter == null ? new IntrospectorPropertyDefinitionsProvider(configurationClasses) :
                new IntrospectorPropertyDefinitionsProvider(configurationClasses, collectionDelimiter));
                    
            return new CompositePropertyDefinitionsProvider(introspector,
                    new JsonFilePropertyDefinitionsProvider(),
                    new PropertyFileOverridePropertyDefinitionsProvider());
        }
        
        private URL getConquesoUrl() throws MalformedURLException {
            if (conquesoUrl == null) {
                String additionalUrls = System.getProperty(URLConfigurationSource.CONFIG_URL);
                if (Strings.isNullOrEmpty(additionalUrls)) {
                    throw new IllegalStateException(
                        "Conqueso URL not specified on the initializer and not set using system property " + 
                                URLConfigurationSource.CONFIG_URL);
                }
                String[] splitUrls = additionalUrls.split(",");
                withConquesoUrl(splitUrls[0]);
            }
            return new URL(conquesoUrl);
        }
        
        private Map<String, String> getInstanceMetadata() {
            if (instanceMetadataProvider == null) {
                withInstanceData(createDefaultInstanceDataProvider());
            }
            
            return instanceMetadataProvider.getInstanceMetadata();
        }
        
        private Set<PropertyDefinition> getPropertyDefinitions() {
            if (propertyDefinitionsProvider == null) {
                if (configurationClasses == null) {
                    LOGGER.warn("No configuration classes have been configured");
                    configurationClasses = Collections.emptyList();
                }
                propertyDefinitionsProvider = createDefaultPropertyDefinitionsProvider();
            }
            
            Map<String, PropertyDefinition> definitions = Maps.newHashMap();
            propertyDefinitionsProvider.addPropertyDefinitions(definitions);
            
            LOGGER.info("{} Archaius property definitions detected", definitions.size());
            
            return ImmutableSet.copyOf(definitions.values());
        }
    }
    
    /**
     * Retrieve the latest set of service properties from the Conqueso Server, returned
     * as a Java Properties object.
     * @return the latest Properties value
     * @throws IOException if there's an error communicating with the Conqueso Server.
     */
    public Properties getLatestProperties() throws IOException {
        InputStream input = null;
        try {
            input = conquesoUrl.openStream();
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }
    
    private ConquesoClient(URL conquesoUrl) {
        this.conquesoUrl = conquesoUrl;
        // Prevent construction outside of Initializer
    }
    
    private void postInitialInstanceInfo(Map<String, String> instanceMetadata,
            Set<PropertyDefinition> combinedPropertyDefinitions) throws IOException {
        
        String json = toJson(instanceMetadata, combinedPropertyDefinitions);
        String message = encode(json);
        post(message);
    }
        
    @VisibleForTesting
    String toJson(Map<String, String> instanceMetadata,
            Set<PropertyDefinition> combinedPropertyDefinitions) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InitialInstanceInfo info = new InitialInstanceInfo(instanceMetadata, combinedPropertyDefinitions);
        return mapper.writeValueAsString(info);
    }
    
    private void post(String message) throws IOException {
        URLConnection connection = conquesoUrl.openConnection();
        connection.setDoOutput(true);
        
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(connection.getOutputStream(), Charsets.UTF_8);
            writer.write(message);
        } finally {
            if (writer != null) {
                writer.close();
            }
            // Need to call this to send data
            connection.getInputStream().close();
        }
    }
    
    private static String encode(String message) {
        try {
            return URLEncoder.encode(message, Charsets.UTF_8.name());
        } catch( UnsupportedEncodingException uee ) {
            throw new IllegalStateException("Default encoding not found", uee);
        }
    }
    
    static class InitialInstanceInfo {
        private final Map<String, String> instanceMetadata;
        private final Set<PropertyDefinition> properties;

        public InitialInstanceInfo(Map<String, String> instanceMetadata, Set<PropertyDefinition> properties) {
            this.instanceMetadata = checkNotNull(instanceMetadata, "instanceMetadata");
            this.properties = checkNotNull(properties, "properties");
        }

        public Map<String, String> getInstanceMetadata() {
            return instanceMetadata;
        }

        public Set<PropertyDefinition> getProperties() {
            return properties;
        }
    }

}
