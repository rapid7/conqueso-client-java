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

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rapid7.conqueso.client.PropertyDefinition;

/**
 * Implementation of PropertyDefinitionsProvider that will use merge property values from external
 * Java {@link Properties} files. The location of the files can be specified using the
 * <code>conqueso.properties.overridePropertiesUrls</code> system property, or through the constructor that
 * takes a URL parameter. The system property value is a comma-separated list of URLs for retrieving the override
 * properties files.
 * <p>
 * If the properties URL is not specified through the system properties or constructor,
 * this implementation does nothing.
 * <p>
 * This properties file can only be used to override the default values of PropertyDefinitions discovered by
 * other means - it cannot be used to define additional property definitions.
 */
public class PropertyFileOverridePropertyDefinitionsProvider extends AbstractUrlBasedPropertyDefinitionsProvider<Properties> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyFileOverridePropertyDefinitionsProvider.class);
    
    public static final String PROPERTIES_OVERRIDE_FILE_SYSTEM_PROPERTY = "conqueso.properties.overridePropertiesUrls";
    
    public PropertyFileOverridePropertyDefinitionsProvider() {
        super(PROPERTIES_OVERRIDE_FILE_SYSTEM_PROPERTY);
    }
    
    public PropertyFileOverridePropertyDefinitionsProvider(URL fileUrl) {
        super(PROPERTIES_OVERRIDE_FILE_SYSTEM_PROPERTY, fileUrl);
    }
    
    public PropertyFileOverridePropertyDefinitionsProvider(List<URL> fileUrls) {
        super(PROPERTIES_OVERRIDE_FILE_SYSTEM_PROPERTY, fileUrls);
    }

    @Override
    protected Properties readModelFromReader(Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        return properties;
    }

    @Override
    protected void mergeProperties(Properties fileContents, Map<String, PropertyDefinition> targetPropertyDefinitionMap) {
        for (String propertyKey : fileContents.stringPropertyNames()) {
            if (targetPropertyDefinitionMap.containsKey(propertyKey)) {
                String propertyValue = fileContents.getProperty(propertyKey);
                
                PropertyDefinition existingDefinition = targetPropertyDefinitionMap.get(propertyKey);
                PropertyDefinition mergedDefinition = new PropertyDefinition(propertyKey, 
                        existingDefinition.getType(), propertyValue, existingDefinition.getDescription());
                
                targetPropertyDefinitionMap.put(propertyKey, mergedDefinition);
            } else {
                LOGGER.warn("Attempting to merge unknown property name, skipping: " + propertyKey);
            }
        } 
    }
}
