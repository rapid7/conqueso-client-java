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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyType;

/**
 * Implementation of PropertyDefinitionsProvider that will read property definitions from specified JSON files. 
 * The location of the files can be specified using the <code>conqueso.properties.jsonUrls</code> system property, 
 * or through the constructor that takes a URL parameter. The system property value is a comma-separated list of 
 * URLs for retrieving the property definition JSON files.
 * <p>
 * If the properties URL is not specified through the system properties or constructor,
 * this implementation does nothing.
 * <p>
 * The format of the JSON file is as follows:
 * <pre>
 * [
 *    {
 *       "name":"exampleString",
 *       "type":"STRING",
 *       "value":"foo"
 *    },
 *    {
 *       "name":"exampleStringList",
 *       "type":"STRING_LIST",
 *       "value":"foo,bar,baz"
 *    }
 * ]
 * </pre>
 * The values for the type field defined in the {@link PropertyType} enum. The value field provides the default value
 * for the property (but not necessarily the value returned by the Conqueso server).
 */
public class JsonFilePropertyDefinitionsProvider extends AbstractFileBasedPropertyDefinitionsProvider<List<PropertyDefinition>> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonFilePropertyDefinitionsProvider.class);
    
    public static final String JSON_FILE_SYSTEM_PROPERTY = "conqueso.properties.jsonUrls";
    
    public JsonFilePropertyDefinitionsProvider() {
        super(JSON_FILE_SYSTEM_PROPERTY);
    }
    
    public JsonFilePropertyDefinitionsProvider(URL fileUrl) {
        super(JSON_FILE_SYSTEM_PROPERTY, fileUrl);
    }
    
    public JsonFilePropertyDefinitionsProvider(List<URL> fileUrls) {
        super(JSON_FILE_SYSTEM_PROPERTY, fileUrls);
    }
   
    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected List<PropertyDefinition> readModelFromReader(Reader reader) throws IOException {
        ObjectMapper parser = new ObjectMapper();
        try {
            List<PropertyDefinition> results = parser.readValue(reader, 
                    new TypeReference<List<PropertyDefinition>>() { });
            return results;
        } catch (IOException e) {
            LOGGER.error("Failed to parse PropertyDefinition list from JSON from file", e);
            throw e;
        }
    }

    @Override
    protected void mergeProperties(List<PropertyDefinition> fileContents,
            Map<String, PropertyDefinition> targetPropertyDefinitionMap) {
        for (PropertyDefinition definition : fileContents) {
            String propertyName = definition.getName();
            if (targetPropertyDefinitionMap.containsKey(propertyName)) {
                LOGGER.info("Overriding property definition for " + propertyName);
            }
            targetPropertyDefinitionMap.put(propertyName, definition);
        }
    }    
    
}
