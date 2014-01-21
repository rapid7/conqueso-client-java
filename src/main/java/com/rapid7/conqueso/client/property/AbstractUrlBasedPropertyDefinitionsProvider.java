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

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyDefinitionsProvider;

/**
 * Base class for PropertyDefinitionsProvider implementations that read definitions from files specified by URLs.
 */
public abstract class AbstractUrlBasedPropertyDefinitionsProvider<M> implements PropertyDefinitionsProvider {
    
    public static final char SYSTEM_PROPERTY_SEPARATOR = ',';
    private static final Splitter SYSTEM_PROPERTY_SPLITTER = Splitter.on(SYSTEM_PROPERTY_SEPARATOR)
            .omitEmptyStrings().trimResults();
    
    private final String systemPropertyKey;
    
    private final ImmutableList<URL> fileUrls;
    
    protected AbstractUrlBasedPropertyDefinitionsProvider(String systemPropertyKey) {
        this(systemPropertyKey, Collections.<URL>emptyList());
    }
    
    protected AbstractUrlBasedPropertyDefinitionsProvider(String systemPropertyKey, URL fileUrl) {
        this(systemPropertyKey, Collections.singletonList(checkNotNull(fileUrl, "fileUrl")));
    }
    
    protected AbstractUrlBasedPropertyDefinitionsProvider(String systemPropertyKey, List<URL> fileUrls) {
        this.systemPropertyKey = checkNotNull(systemPropertyKey, "systemPropertyKey");
        this.fileUrls = ImmutableList.copyOf(fileUrls);
    }
        
    protected abstract M readModelFromReader(Reader reader) throws IOException;
    
    protected abstract void mergeProperties(M fileContents, Map<String, PropertyDefinition> targetPropertyDefinitionMap);

    @Override
    public void addPropertyDefinitions(Map<String, PropertyDefinition> targetPropertyDefinitionMap) {
        List<M> propertyFiles = readFiles();
        for (M fileContents : propertyFiles) {
            mergeProperties(fileContents, targetPropertyDefinitionMap);    
        }
    }
    
    private List<M> readFiles() {
        List<URL> targetUrls = fileUrls;
        if (targetUrls.isEmpty()) {
            targetUrls = getTargetUrlsFromSystemProperty();
        }
        
        ImmutableList.Builder<M> builder = ImmutableList.builder();
        for (URL targetUrl : targetUrls) {
            M properties = readPropertiesFromFile(targetUrl);
            if (properties != null) {
                builder.add(properties);
            }
        }
        
        return builder.build();
    }
    
    private M readPropertiesFromFile(URL fileUrl) {
        Reader input = null;
        try {
            input = new InputStreamReader(fileUrl.openStream(), Charsets.UTF_8);
            M properties = readModelFromReader(input);
            return properties;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read properties from url: " + fileUrl, e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    throw new IllegalArgumentException("Failed to close properties file url: " + fileUrl, e);
                }
            }
        }
    }

    private List<URL> getTargetUrlsFromSystemProperty() {
        String propertyValue = System.getProperty(systemPropertyKey);
        if (Strings.isNullOrEmpty(propertyValue)) {
            return Collections.emptyList();
        }
        
        ImmutableList.Builder<URL> builder = ImmutableList.builder();
        for (String propertyUrl : SYSTEM_PROPERTY_SPLITTER.split(propertyValue)) {
            try {
                builder.add(new URL(propertyUrl));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid override properties file URL: " + propertyValue);
            }
        }
        
        return builder.build();
    }

}
