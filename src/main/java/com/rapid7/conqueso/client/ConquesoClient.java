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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.net.HttpHeaders;
import com.netflix.config.DynamicListProperty;
import com.netflix.config.sources.URLConfigurationSource;
import com.rapid7.conqueso.client.metadata.CompositeInstanceMetadataProvider;
import com.rapid7.conqueso.client.metadata.CustomInstanceMetadataProvider;
import com.rapid7.conqueso.client.metadata.EC2InstanceMetadataProvider;
import com.rapid7.conqueso.client.metadata.SystemPropertiesInstanceMetadataProvider;
import com.rapid7.conqueso.client.property.AnnotationScanPropertyDefinitionsProvider;
import com.rapid7.conqueso.client.property.CompositePropertyDefinitionsProvider;
import com.rapid7.conqueso.client.property.CustomPropertyDefinitionsProvider;
import com.rapid7.conqueso.client.property.IntrospectorPropertyDefinitionsProvider;
import com.rapid7.conqueso.client.property.JsonFilePropertyDefinitionsProvider;
import com.rapid7.conqueso.client.property.PropertyFileOverridePropertyDefinitionsProvider;

/**
 * Class used to populate the Conqueso server with information about a client application instance, as
 * well as provide methods to query the Conqueso server. Instances are created using the {@link #initializer()}
 * method.
 * <p>
 * On initialization of the ConquesoClient, metadata about the client application instance and the Archaius
 * properties used by the application are transmitted to the Conqueso server. Methods to specify and customize
 * the instance metadata and property definitions are available on the {@link Initializer} class.
 * <p>
 * The behavior of the ConquesoClient should provide reasonable defaults with minimal customization. A consumer
 * should minimally specify the classes containing their Archaius properties as static fields, or provide
 * a package to scan for classes annotated with <code>@ConquesoConfig</code> For example:
 * <pre>
 * ConquesoClient.initializer()
 *     .withConfigurationClasses(AppConfig.class, DbConfig.class, QueueConfig.class)
 *     .initialize();
 * or
 * ConquesoClient.initializer()
 *     .withConfigurationScan("com.example.package")
 *     .initialize();
 * </pre>
 * 
 * @see Initializer
 */
public class ConquesoClient {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConquesoClient.class);
    
    public static final String CONQUESO_SERVER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; 
        
    private final URL conquesoUrl;
    private final ObjectMapper objectMapper;
    
    /**
     * Utility method to parse the date values returned from the Conqueso server
     * @param conquesoDateValue date value from the Conqueso server
     * @return the value as a Java Date
     * @throws ParseException if there's an issue parsing the value
     */
    public static Date parseConquesoDate(String conquesoDateValue) throws ParseException {
        return new SimpleDateFormat(CONQUESO_SERVER_DATE_FORMAT).parse(conquesoDateValue);
    }
    
    /**
     * Create the Initializer object used to establish a connection to the Conqueso server.
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
        
        private List<String> scanPackages = null;
        private Class<? extends Annotation> markerAnnotation = null;
        
        private String collectionDelimiter;
        
        /**
         * Initialize the Conqueso Client. This will establish a connection to the server to send
         * the instance's metadata and property definitions.
         * 
         * @return the initialized ConquesoClient
         * @throws ConquesoCommunicationException if there was an issue communicating with the Conqueso server
         */
        public ConquesoClient initialize() {            
            URL url = getConquesoUrl();       
            
            Map<String, String> instanceMetadata = getInstanceMetadata();
            
            Set<PropertyDefinition> propertyDefs = getPropertyDefinitions();
            
            ConquesoClient client = new ConquesoClient(url);
            
            // Handle not running against a Conqueso server
            if (url.getProtocol().equals("http") || url.getProtocol().equals("https")) {
                LOGGER.info("Initializing connection with Conqueso Server: " + url.toExternalForm());
                client.postInitialInstanceInfo(instanceMetadata, propertyDefs);   
            } else {
                LOGGER.warn("Skipping posting of instance info to " + url.toExternalForm());
            }
            
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
            checkState(this.conquesoUrl == null, "Conqueso URL already configured");
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
            checkState(this.instanceMetadataProvider == null, "Instance data already configured");
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
            checkState(this.propertyDefinitionsProvider == null, "Property definitions provider already configured");
            this.propertyDefinitionsProvider = propertyDefinitionsProvider;
            return this;
        }
        
        /**
         * Configure the client to avoid transmitting property definitions to the Conqueso server for this instance.
         * 
         * @return the initializer for method chaining
         */
        public Initializer withNoProperties() {
            checkState(this.configurationClasses == null, "Configuration classes already configured");
            return withPropertyDefinitions(new CustomPropertyDefinitionsProvider());
        }
        
        /**
         * Configure the scanning for configuration classes with the {@link ConquesoConfig} annotation.
         * The classpath will be scanned for classes declared under the specified scanPackages, marked with the
         * ConquesoConfig annotation. The found classes will then be scanned to discover the static Archaius dynamic
         * property fields, and the definition of those properties will be transmitted to the Conqueso server. 
         * The definitions will be used to set up the properties to return to Archaius, the type of fields, and 
         * the default values.
         * @param scanPackages the Java packages to scan for configuration classes
         */
        public Initializer withConfigurationScan(String...scanPackages) {
            return withConfigurationScan(ConquesoConfig.class, scanPackages);
        }
        
        /**
         * Configure the scanning for configuration classes with a marker annotation.
         * The classpath will be scanned for classes declared under the specified scanPackages, marked with the
         * given markerAnnotation. The found classes will then be scanned to discover the static Archaius dynamic
         * property fields, and the definition of those properties will be transmitted to the Conqueso server. 
         * The definitions will be used to set up the properties to return to Archaius, the type of fields, and 
         * the default values.
         * @param markerAnnotation the annotation used to mark classes containing Archaius properties
         * @param scanPackages the Java packages to scan for configuration classes
         */
        public Initializer withConfigurationScan(Class<? extends Annotation> markerAnnotation, String...scanPackages) {
            checkState(this.markerAnnotation == null, "Configuration scan already configured");
            checkArgument(scanPackages.length > 0, "No scan packages specified");
            this.markerAnnotation = checkNotNull(markerAnnotation, "markerAnnotation");
            this.scanPackages = ImmutableList.copyOf(scanPackages);
            return this;
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
            checkState(this.configurationClasses == null, "Configuration classes already configured");
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
            checkState(this.collectionDelimiter == null, "Collection property delimiter already configured");
            checkArgument(!Strings.isNullOrEmpty(collectionDelimiter), "collectionDelimiter");
            this.collectionDelimiter = collectionDelimiter;
            return this;
        }
        
        /**
         * Create the default {@link InstanceMetadataProvider} to use for a ConquesoClient. This provider
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
            List<PropertyDefinitionsProvider> providers = Lists.newArrayList();
            
            String delimiter = Objects.firstNonNull(collectionDelimiter, DynamicListProperty.DEFAULT_DELIMITER);
            
            if (markerAnnotation != null) {
                providers.add(new AnnotationScanPropertyDefinitionsProvider(markerAnnotation, scanPackages, 
                        delimiter));                
            }
            
            if (configurationClasses != null) {
                providers.add(new IntrospectorPropertyDefinitionsProvider(configurationClasses, delimiter));
            }
            
            providers.add(new JsonFilePropertyDefinitionsProvider());
            providers.add(new PropertyFileOverridePropertyDefinitionsProvider());

            return new CompositePropertyDefinitionsProvider(providers);
        }
        
        private URL getConquesoUrl() {
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
            try {
                return new URL(conquesoUrl);
            } catch (MalformedURLException e) {
                throw new ConquesoCommunicationException("Bad URL for Conqueso Server", e);
            }
        }
        
        private Map<String, String> getInstanceMetadata() {
            if (instanceMetadataProvider == null) {
                withInstanceData(createDefaultInstanceDataProvider());
            }
            
            return instanceMetadataProvider.getInstanceMetadata();
        }
        
        private Set<PropertyDefinition> getPropertyDefinitions() {
            if (propertyDefinitionsProvider == null) {
                if (markerAnnotation == null && configurationClasses == null) {
                    LOGGER.warn("No configuration classes or configuration scan have been configured");
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
     * @throws ConquesoCommunicationException if there's an error communicating with the Conqueso Server.
     */
    public Properties getLatestProperties() {
        InputStream input = null;
        try {
            input = conquesoUrl.openStream();
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new ConquesoCommunicationException("Failed to retrieve latest properties from Conqueso server: " +
                    conquesoUrl.toExternalForm(), e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    throw new ConquesoCommunicationException(
                            "Failed to retrieve latest properties from Conqueso server", e);
                }
            }
        }
    }
    
    /**
     * Retrieve the latest value for the given property key from the Conqueso Server, returned
     * as a String.
     * @param key the property key to look up
     * @return the latest property value
     * @throws ConquesoCommunicationException if there's an error communicating with the Conqueso Server.
     */
    public String getPropertyValue(String key) {
        checkArgument(!Strings.isNullOrEmpty(key), "key");
        
        String errorMessage = String.format("Failed to retrieve %s property from Conqueso server: %s",
                key, conquesoUrl.toExternalForm());
        
        return readStringFromUrl("properties/" + key, errorMessage);
    }
    
    /**
     * Retrieve information about the roles from the Conqueso Server.
     * @return the role information
     * @throws ConquesoCommunicationException if there's an error communicating with the Conqueso Server.
     */
    public ImmutableList<RoleInfo> getRoles() {
        TypeReference<List<RoleInfo>> typeReference = new TypeReference<List<RoleInfo>>() { };
        
        String errorMessage = String.format("Failed to retrieve roles from Conqueso server: %s",
                conquesoUrl.toExternalForm());
        
        return ImmutableList.copyOf(readObjectFromJson(typeReference, "/api/roles", errorMessage));
    }
    
    /**
     * Retrieve information about all online instances from the Conqueso Server.
     * @return the information about the instances
     * @throws ConquesoCommunicationException if there's an error communicating with the Conqueso Server.
     */
    public ImmutableList<InstanceInfo> getInstances() {
        return getInstancesWithMetadataImpl(Collections.<String, String>emptyMap());
    }
    
    /**
     * Retrieve information about all online instances matching the given metadata query from the Conqueso Server.
     * The metadata query is expressed as key/value pairs. For example, calling this method like this:
     * <p>
     * <code>
     * conquesoClient.getInstancesWithMetadata("availability-zone", "us-east-1c", "instance-type", "m1.small");
     * </code>
     * </p>
     * will return all instances with metadata that matches availability-zone=us-east-1c and instance-type=m1.small.
     * 
     * @param metadataQueryPairs the key/value pairs representing a query for instances matching the metadata
     * @return the information about the matching instances
     * @throws ConquesoCommunicationException if there's an error communicating with the Conqueso Server.
     */
    public ImmutableList<InstanceInfo> getInstancesWithMetadata(String...metadataQueryPairs) {
        checkArgument(metadataQueryPairs.length > 0, "No metadata query pairs specified");
        checkArgument(metadataQueryPairs.length % 2 == 0, "Odd number of arguments passed as metadata query pairs");
                
        return getInstancesWithMetadata(toMap(metadataQueryPairs));
    }
    
    /**
     * Retrieve information about all online instances matching the given metadata query from the Conqueso Server.
     * The metadata query is expressed as key/value pairs in the provided map. For example, calling this method like 
     * this:
     * <p>
     * <code>
     * conquesoClient.getInstancesWithMetadata(ImmutableMap.of("availability-zone", "us-east-1c", 
     * "instance-type", "m1.small"));
     * </code>
     * </p>
     * will return all instances with metadata that matches availability-zone=us-east-1c and instance-type=m1.small.
     * 
     * @param metadataQuery the map key/value pairs representing a query for instances matching the metadata
     * @return the information about the matching instances
     * @throws ConquesoCommunicationException if there's an error communicating with the Conqueso Server.
     */
    public ImmutableList<InstanceInfo> getInstancesWithMetadata(Map<String, String> metadataQuery) {
        checkArgument(!checkNotNull(metadataQuery, "metadataQuery").isEmpty(), "No metadata query arguments specified");
        return getInstancesWithMetadataImpl(metadataQuery);        
    }
    
    private ImmutableList<InstanceInfo> getInstancesWithMetadataImpl(Map<String, String> metadataQuery) {        
        String queryParams = metadataQuery.isEmpty() ? "" : buildMetadataQueryString(metadataQuery);
        
        TypeReference<List<InstanceInfo>> typeReference = new TypeReference<List<InstanceInfo>>() { };
        
        String relativeUrl = String.format("/api/instances%s", queryParams);
        
        String errorMessage = String.format("Failed to retrieve instances from Conqueso server: %s",
                conquesoUrl.toExternalForm());
        
        return ImmutableList.copyOf(readObjectFromJson(typeReference, relativeUrl, errorMessage));
    }
    
    /**
     * Retrieve information about instances of a particular role from the Conqueso Server.
     * @param roleName the role to retrieve
     * @return the information about the instances of the given role
     * @throws ConquesoCommunicationException if there's an error communicating with the Conqueso Server.
     */
    public ImmutableList<InstanceInfo> getRoleInstances(String roleName) {
        return getRoleInstancesWithMetadataImpl(roleName, Collections.<String, String>emptyMap());
    }
    
    /**
     * Retrieve information about all online instances of a particular role matching the given metadata query from 
     * the Conqueso Server.
     * The metadata query is expressed as key/value pairs. For example, calling this method like this:
     * <p>
     * <code>
     * conquesoClient.getRoleInstancesWithMetadata("reporting-service", "availability-zone", "us-east-1c", 
     * "instance-type", "m1.small");
     * </code>
     * </p>
     * will return all instances of role reporting-service with metadata that matches availability-zone=us-east-1c and 
     * instance-type=m1.small.
     * 
     * @param roleName the role to retrieve
     * @param metadataQueryPairs the key/value pairs representing a query for instances matching the metadata
     * @return the information about the matching instances
     * @throws ConquesoCommunicationException if there's an error communicating with the Conqueso Server.
     */
    public ImmutableList<InstanceInfo> getRoleInstancesWithMetadata(String roleName, String...metadataQueryPairs) {
        checkArgument(metadataQueryPairs.length > 0, "No metadata query pairs specified");
        checkArgument(metadataQueryPairs.length % 2 == 0, "Odd number of arguments passed as metadata query pairs");
                
        return getRoleInstancesWithMetadata(roleName, toMap(metadataQueryPairs));
    }
    
    /**
     * Retrieve information about all online instances of a particular role matching the given metadata query from 
     * the Conqueso Server.
     * The metadata query is expressed as key/value pairs in the provided map. For example, calling this method like 
     * this:
     * <p>
     * <code>
     * conquesoClient.getRoleInstancesWithMetadata("reporting-service", ImmutableMap.of("availability-zone", 
     * "us-east-1c", "instance-type", "m1.small"));
     * </code>
     * </p>
     * will return all instances of role reporting-service with metadata that matches availability-zone=us-east-1c and 
     * instance-type=m1.small.
     * 
     * @param roleName the role to retrieve
     * @param metadataQuery the map key/value pairs representing a query for instances matching the metadata
     * @return the information about the matching instances
     * @throws ConquesoCommunicationException if there's an error communicating with the Conqueso Server.
     */
    public ImmutableList<InstanceInfo> getRoleInstancesWithMetadata(String roleName, 
            Map<String, String> metadataQuery) {
        checkArgument(!checkNotNull(metadataQuery, "metadataQuery").isEmpty(), "No metadata query arguments specified");
        return getRoleInstancesWithMetadataImpl(roleName, metadataQuery);        
    }
    
    private ImmutableList<InstanceInfo> getRoleInstancesWithMetadataImpl(String roleName, 
            Map<String, String> metadataQuery) {
        checkArgument(!Strings.isNullOrEmpty(roleName), "roleName");
        
        String queryParams = metadataQuery.isEmpty() ? "" : buildMetadataQueryString(metadataQuery);
        
        TypeReference<List<InstanceInfo>> typeReference = new TypeReference<List<InstanceInfo>>() { };
        
        String relativeUrl = String.format("/api/roles/%s/instances%s", roleName, queryParams);
        
        String errorMessage = String.format("Failed to retrieve %s instances from Conqueso server: %s",
                roleName, conquesoUrl.toExternalForm());
        
        return ImmutableList.copyOf(readObjectFromJson(typeReference, relativeUrl, errorMessage));
    }
        
    private static ImmutableMap<String, String> toMap(String...pairs) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        
        for (int i = 0; i < pairs.length; i += 2) {
            String key = pairs[i];
            String value = pairs[i + 1];
            builder.put(key, value);
        }
        return builder.build();
    }
    
    private static String buildMetadataQueryString(Map<String, String> metadataQuery) {
        try {
            StringBuilder sb = new StringBuilder("?");
            for (Map.Entry<String, String> entry : metadataQuery.entrySet()) {
                if (sb.length() > 1) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(entry.getKey(), Charsets.UTF_8.name()));
                sb.append('=');
                sb.append(URLEncoder.encode(entry.getValue(), Charsets.UTF_8.name()));
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF_8 encoding isn't supported apparently. Crazy.", e);
        }
    }
    
    @VisibleForTesting
    ConquesoClient(URL conquesoUrl) {
        this.conquesoUrl = conquesoUrl;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
        // Prevent construction outside of Initializer
    }
    
    private void postInitialInstanceInfo(Map<String, String> instanceMetadata,
            Set<PropertyDefinition> combinedPropertyDefinitions) {
        
        try {
            String json = toJson(instanceMetadata, combinedPropertyDefinitions);
            LOGGER.debug("Transmitting instance info to Conqueso Server:");
            LOGGER.debug(json);
            post(json);
        } catch (Exception e) {
            throw new ConquesoCommunicationException("Failed to send instance info to Conqueso Server: " +
                    conquesoUrl.toExternalForm(), e);
        }
    }
        
    @VisibleForTesting
    String toJson(Map<String, String> instanceMetadata,
            Set<PropertyDefinition> combinedPropertyDefinitions) throws IOException {
        InitialInstanceInfo info = new InitialInstanceInfo(instanceMetadata, combinedPropertyDefinitions);
        return objectMapper.writeValueAsString(info);
    }
    
    private void post(String message) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)conquesoUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json");
        connection.setRequestMethod("POST");
        
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
    
    @VisibleForTesting
    String readStringFromUrl(String relativeUrl, String errorMessage) {
        InputStream input = null;
        try {
            URL propertyUrl = new URL(conquesoUrl, relativeUrl);
            input = propertyUrl.openStream();
            return CharStreams.toString(new InputStreamReader(input, Charsets.UTF_8));
        } catch (IOException e) {
            throw new ConquesoCommunicationException(errorMessage, e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    throw new ConquesoCommunicationException(errorMessage, e);
                }
            }
        }
    }
    
    private <T> T readObjectFromJson(TypeReference<T> objectType, String relativeUrl, String errorMessage) {
        try {
            return objectMapper.readValue(readStringFromUrl(relativeUrl, errorMessage), objectType);
        } catch (IOException e) {
            throw new ConquesoCommunicationException(errorMessage, e);
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
