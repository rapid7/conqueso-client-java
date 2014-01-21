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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.netflix.config.DynamicListProperty;
import com.rapid7.conqueso.client.PropertyDefinition;
import com.rapid7.conqueso.client.PropertyDefinitionsProvider;

/**
 * Implementation of PropertyDefinitionsProvider that will find classes in the classpath annotated with
 * a given marker annotation, then pass the resulting classes to an {@link IntrospectorPropertyDefinitionsProvider}.
 * The annotation search will be scoped within the provided set of scanPackages - including child packages. For
 * example, specifying the marker annotation ConquesoConfig and the scanPackage "com.foo", this provider
 * will look for classes marked with <code>@ConquesoConfig</code> in the package "com.foo", "com.foo.bar",
 * "com.foo.baz".
 */
public class AnnotationScanPropertyDefinitionsProvider implements PropertyDefinitionsProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationScanPropertyDefinitionsProvider.class);
    
    private final Class<? extends Annotation> markerAnnotation;
    private final ImmutableList<String> scanPackages;
    private final String collectionDelimiter;
    
    /**
     * Scan for classes marked with the given marker annotation within the given packages.
     * @param markerAnnotation the marker annotation to detect on configuration classes.
     * @param scanPackages the packages to narrow the classpath scan.
     */
    public AnnotationScanPropertyDefinitionsProvider(Class<? extends Annotation> markerAnnotation,
            List<String> scanPackages) {
        this(markerAnnotation, scanPackages, DynamicListProperty.DEFAULT_DELIMITER);
    }
    
    /**
     * Scan for classes marked with the given marker annotation within the given packages.
     * @param markerAnnotation the marker annotation to detect on configuration classes.
     * @param scanPackages the packages to narrow the classpath scan.
     * @param collectionDelimiter the delimiter to use between collection property values
     */
    public AnnotationScanPropertyDefinitionsProvider(Class<? extends Annotation> markerAnnotation,
            List<String> scanPackages, String collectionDelimiter) {
        this.markerAnnotation = checkNotNull(markerAnnotation, "markerAnnotation");
        checkArgument(scanPackages != null && !scanPackages.isEmpty(), "scanPackages");
        this.scanPackages = ImmutableList.copyOf(scanPackages);
        this.collectionDelimiter = checkNotNull(collectionDelimiter, "collectionDelimiter");
    }
    
    @Override
    public void addPropertyDefinitions(Map<String, PropertyDefinition> targetPropertyDefinitionMap) {
        Collection<Class<?>> annotatedClasses = findAnnotatedClasses();
        if (annotatedClasses.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("No classes marked with %s annotation found in packages %s",
                    markerAnnotation.getName(),
                    scanPackages));
        }
        
        LOGGER.info("Discovered {} classes to scan for Archaius properties", annotatedClasses.size());
        
        IntrospectorPropertyDefinitionsProvider introspector = 
                new IntrospectorPropertyDefinitionsProvider(annotatedClasses, collectionDelimiter);

        introspector.addPropertyDefinitions(targetPropertyDefinitionMap);
    }
    
    private Collection<Class<?>> findAnnotatedClasses() {        
        Object[] params = new Object[scanPackages.size() + 1];
        int i = 0;
        for (; i < scanPackages.size(); i++) {
            params[i] = scanPackages.get(i);
        }
        params[i] = new TypeAnnotationsScanner();
        Reflections reflections = new Reflections(params);
        return reflections.getTypesAnnotatedWith(markerAnnotation);
    }
}
