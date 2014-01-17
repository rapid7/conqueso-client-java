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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.map.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.netflix.config.DynamicListProperty;
import com.netflix.config.DynamicMapProperty;
import com.netflix.config.Property;
import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyDefinitionsProvider;
import com.rapid7.conqueso.client.PropertyType;

/**
 * Implementation of PropertyDefinitionsProvider that will use reflection to discover Archaius properties defined
 * as static fields in the provided classes. Static fields with private or protected modifiers can be read by
 * this class. This class will not climb through a class hierarchy - pass both super and subclasses in to be
 * read to discover properties if needed. <code>IllegalArgumentException</code>s will be thrown if there are
 * issues reading the Properties from the target classes.
 */
public class IntrospectorPropertyDefinitionsProvider implements PropertyDefinitionsProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(IntrospectorPropertyDefinitionsProvider.class);
    
    private static final String MAP_KEY_VALUE_DELIMITER = "=";
    
    private final ImmutableList<Class<?>> targetClasses;
    private final Joiner collectionJoiner;
    
    public IntrospectorPropertyDefinitionsProvider(Class<?>...targetClasses) {
        this(Arrays.asList(targetClasses), DynamicListProperty.DEFAULT_DELIMITER);
    }
    
    public IntrospectorPropertyDefinitionsProvider(Collection<Class<?>> targetClasses) {
        this(targetClasses, DynamicListProperty.DEFAULT_DELIMITER);
    }
    
    public IntrospectorPropertyDefinitionsProvider(Collection<Class<?>> targetClasses, String collectionDelimiter) {
        checkArgument(!checkNotNull(targetClasses, "targetClasses").isEmpty(), "targetClasses");
        this.targetClasses = ImmutableList.copyOf(targetClasses);
        collectionJoiner = Joiner.on(checkNotNull(collectionDelimiter, "collectionDelimiter"));
    }
    
    @Override
    public void addPropertyDefinitions(Map<String, PropertyDefinition> targetPropertyDefinitionMap) {
        for (Class<?> targetClass : targetClasses) {
            addClassPropertyDefinitions(targetPropertyDefinitionMap, targetClass);
        }
    }
    
    private void addClassPropertyDefinitions(Map<String, PropertyDefinition> targetPropertyDefinitionMap, 
            Class<?> targetClass) {
        Set<Field> propertyFields = findStaticFieldsOfType(Property.class, targetClass);
        if (propertyFields.isEmpty()) {
            LOGGER.warn("No Archaius properties found as static fields on class " + targetClass.getName());
        }
        for (Field propertyField : propertyFields) {
            addFieldPropertyDefinition(targetPropertyDefinitionMap, propertyField);
        }
    }
    
    private static Set<Field> findStaticFieldsOfType(Class<?> typeClass, Class<?> targetClass) {
        ImmutableSet.Builder<Field> builder = ImmutableSet.builder();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (Modifier.isStatic(declaredField.getModifiers()) && 
                    typeClass.isAssignableFrom(declaredField.getType())) {
                builder.add(declaredField);
            }
        }
        return builder.build();
    }
    
    private void addFieldPropertyDefinition(Map<String, PropertyDefinition> targetPropertyDefinitionMap, 
            Field propertyField) {
        // Hack the Java permissions to allow us to access a private field
        ClassUtil.checkAndFixAccess(propertyField);
        try {
            // Read the static property field value from the target class
            Property<?> property = (Property<?>)propertyField.get(null);
            // Skip null field values
            if (property == null) {
                return;
            }

            addPropertyDefinition(targetPropertyDefinitionMap, propertyField, property);            
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Failed to read field %s from class %s", 
                    propertyField.getName(), propertyField.getDeclaringClass().getName()), e);
        }
    }
    
    private void addPropertyDefinition(Map<String, PropertyDefinition> targetPropertyDefinitionMap,
            Field propertyField, Property<?> property) {
        
        String propName = getPropertyName(propertyField, property);
        
        if (targetPropertyDefinitionMap.containsKey(propName)) {
            throw new IllegalArgumentException(String.format("Duplicate property name %s - %s.%s", 
                    propName,
                    propertyField.getDeclaringClass().getName(),
                    propertyField.getName()));
        }
        
        Object defaultValue = getDefaultValue(property);            
        PropertyType type = getPropertyType(propertyField, property);
        
        targetPropertyDefinitionMap.put(propName, new PropertyDefinition(propName, type, 
                defaultValue == null ? "" : defaultValue.toString()));
    }
    
    private Object getDefaultValue(Property<?> property) {
        Object defaultValue = property.getDefaultValue();
        if (defaultValue == null) {
            return null;
        }
        
        if (property instanceof DynamicMapProperty) {
            Map<?,?> mapValue = ((DynamicMapProperty<?,?>)property).getDefaultValueMap();
            if (mapValue != null) {
                defaultValue = collectionJoiner.withKeyValueSeparator(MAP_KEY_VALUE_DELIMITER)
                        .join(mapValue);
            }
            
        } else if (defaultValue instanceof Collection) {
            defaultValue = collectionJoiner.join((Collection<?>)defaultValue);
        }
        return defaultValue;
    }
    
    private String getPropertyName(Field propertyField, Property<?> property) {
        String propName = property.getName();
        if (Strings.isNullOrEmpty(propName)) {
            throw new IllegalArgumentException(String.format("Property field without name value - %s.%s", 
                    propertyField.getDeclaringClass().getName(),
                    propertyField.getName()));
        }
        return propName;
    }
    
    private PropertyType getPropertyType(Field propertyField, Property<?> property) {
        PropertyType type = PropertyType.getByPropertyClass(property.getClass());
        if (type == null) {
            throw new IllegalArgumentException(
                    String.format("Unsupported Property type %s for field %s from class %s", 
                    property.getClass().getName(), 
                    propertyField.getName(), 
                    propertyField.getDeclaringClass().getName()));
        }
        return type;
    }
}
