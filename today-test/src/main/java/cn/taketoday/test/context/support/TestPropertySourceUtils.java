/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.support;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.io.DefaultPropertySourceFactory;
import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.PropertySourceDescriptor;
import cn.taketoday.core.io.PropertySourceFactory;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Utility methods for working with {@link TestPropertySource @TestPropertySource}
 * and adding test {@link PropertySource PropertySources} to the {@code Environment}.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @author Anatoliy Korovin
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TestPropertySource
 * @since 4.0
 */
public abstract class TestPropertySourceUtils {

  /**
   * The name of the {@link MapPropertySource} created from <em>inlined properties</em>.
   *
   * @see #addInlinedPropertiesToEnvironment
   */
  public static final String INLINED_PROPERTIES_PROPERTY_SOURCE_NAME = "Inlined Test Properties";

  private static final PropertySourceFactory defaultPropertySourceFactory = new DefaultPropertySourceFactory();

  private static final Logger logger = LoggerFactory.getLogger(TestPropertySourceUtils.class);

  static MergedTestPropertySources buildMergedTestPropertySources(Class<?> testClass) {
    List<TestPropertySourceAttributes> attributesList = new ArrayList<>();

    TestPropertySourceAttributes previousAttributes = null;
    // Iterate over all aggregate levels, where each level is represented by
    // a list of merged annotations found at that level (e.g., on a test
    // class in the class hierarchy).
    for (List<MergedAnnotation<TestPropertySource>> aggregatedAnnotations :
        findRepeatableAnnotations(testClass, TestPropertySource.class)) {

      // Convert all the merged annotations for the current aggregate
      // level to a list of TestPropertySourceAttributes.
      List<TestPropertySourceAttributes> aggregatedAttributesList =
          aggregatedAnnotations.stream().map(TestPropertySourceAttributes::new).toList();
      // Merge all TestPropertySourceAttributes instances for the current
      // aggregate level into a single TestPropertySourceAttributes instance.
      TestPropertySourceAttributes mergedAttributes = mergeTestPropertySourceAttributes(aggregatedAttributesList);
      if (mergedAttributes != null) {
        if (!duplicationDetected(mergedAttributes, previousAttributes)) {
          attributesList.add(mergedAttributes);
        }
        previousAttributes = mergedAttributes;
      }
    }

    if (attributesList.isEmpty()) {
      return MergedTestPropertySources.empty();
    }
    return new MergedTestPropertySources(mergeLocations(attributesList), mergeProperties(attributesList));
  }

  @Nullable
  private static TestPropertySourceAttributes mergeTestPropertySourceAttributes(
      List<TestPropertySourceAttributes> aggregatedAttributesList) {

    TestPropertySourceAttributes mergedAttributes = null;
    TestPropertySourceAttributes previousAttributes = null;
    for (TestPropertySourceAttributes currentAttributes : aggregatedAttributesList) {
      if (mergedAttributes == null) {
        mergedAttributes = currentAttributes;
      }
      else if (!duplicationDetected(currentAttributes, previousAttributes)) {
        mergedAttributes.mergeWith(currentAttributes);
      }
      previousAttributes = currentAttributes;
    }

    return mergedAttributes;
  }

  private static boolean duplicationDetected(TestPropertySourceAttributes currentAttributes,
      @Nullable TestPropertySourceAttributes previousAttributes) {

    boolean duplicationDetected =
        (currentAttributes.equals(previousAttributes) && !currentAttributes.isEmpty());

    if (duplicationDetected && logger.isTraceEnabled()) {
      logger.trace("Ignoring duplicate {} declaration on {} since it is also declared on {}",
          currentAttributes, currentAttributes.getDeclaringClass().getName(),
          previousAttributes.getDeclaringClass().getName());
    }

    return duplicationDetected;
  }

  private static List<PropertySourceDescriptor> mergeLocations(List<TestPropertySourceAttributes> attributesList) {
    List<PropertySourceDescriptor> descriptors = new ArrayList<>();
    for (TestPropertySourceAttributes attrs : attributesList) {
      if (logger.isTraceEnabled()) {
        logger.trace("Processing locations for {}", attrs);
      }
      descriptors.addAll(0, attrs.getPropertySourceDescriptors());
      if (!attrs.isInheritLocations()) {
        break;
      }
    }
    return descriptors;
  }

  private static String[] mergeProperties(List<TestPropertySourceAttributes> attributesList) {
    List<String> properties = new ArrayList<>();
    for (TestPropertySourceAttributes attrs : attributesList) {
      if (logger.isTraceEnabled()) {
        logger.trace("Processing inlined properties for {}", attrs);
      }
      String[] attrProps = attrs.getProperties();
      properties.addAll(0, Arrays.asList(attrProps));
      if (!attrs.isInheritProperties()) {
        break;
      }
    }
    return StringUtils.toStringArray(properties);
  }

  /**
   * Add the {@link Properties} files from the given resource {@code locations}
   * to the {@link Environment} of the supplied {@code context}.
   * <p>This method simply delegates to
   * {@link #addPropertiesFilesToEnvironment(ConfigurableEnvironment, ResourceLoader, String...)}.
   *
   * @param context the application context whose environment should be updated;
   * never {@code null}
   * @param locations the resource locations of {@code Properties} files to add
   * to the environment; potentially empty but never {@code null}
   * @throws IllegalStateException if an error occurs while processing a properties file
   * @see cn.taketoday.core.io.ResourcePropertySource
   * @see TestPropertySource#locations
   * @see #addPropertiesFilesToEnvironment(ConfigurableEnvironment, ResourceLoader, String...)
   * @see #addPropertySourcesToEnvironment(ConfigurableApplicationContext, List)
   */
  public static void addPropertiesFilesToEnvironment(ConfigurableApplicationContext context, String... locations) {
    Assert.notNull(context, "'context' must not be null");
    Assert.notNull(locations, "'locations' must not be null");
    addPropertiesFilesToEnvironment(context.getEnvironment(), context, locations);
  }

  /**
   * Add the {@link Properties} files from the given resource {@code locations}
   * to the supplied {@link ConfigurableEnvironment environment}.
   * <p>Property placeholders in resource locations (i.e., <code>${...}</code>)
   * will be {@linkplain Environment#resolveRequiredPlaceholders(String) resolved}
   * against the {@code Environment}.
   * <p>Each properties file will be converted to a
   * {@link cn.taketoday.core.io.ResourcePropertySource ResourcePropertySource}
   * that will be added to the {@link PropertySources} of the environment with
   * the highest precedence.
   *
   * @param environment the environment to update; never {@code null}
   * @param resourceLoader the {@code ResourceLoader} to use to load each resource;
   * never {@code null}
   * @param locations the resource locations of {@code Properties} files to add
   * to the environment; potentially empty but never {@code null}
   * @throws IllegalStateException if an error occurs while processing a properties file
   * @see cn.taketoday.core.io.ResourcePropertySource
   * @see TestPropertySource#locations
   * @see #addPropertiesFilesToEnvironment(ConfigurableApplicationContext, String...)
   * @see #addPropertySourcesToEnvironment(ConfigurableApplicationContext, List)
   */
  public static void addPropertiesFilesToEnvironment(ConfigurableEnvironment environment,
      ResourceLoader resourceLoader, String... locations) {

    Assert.notNull(locations, "'locations' must not be null");
    addPropertySourcesToEnvironment(environment, resourceLoader,
        List.of(new PropertySourceDescriptor(locations)));
  }

  /**
   * Add property sources for the given {@code descriptors} to the
   * {@link Environment} of the supplied {@code context}.
   * <p>Property placeholders in resource locations (i.e., <code>${...}</code>)
   * will be {@linkplain Environment#resolveRequiredPlaceholders(String) resolved}
   * against the {@code Environment}.
   * <p>Each {@link PropertySource} will be created via the configured
   * {@link PropertySourceDescriptor#propertySourceFactory() PropertySourceFactory}
   * (or the {@link DefaultPropertySourceFactory} if no factory is configured)
   * and added to the {@link PropertySources} of the environment with the highest
   * precedence.
   *
   * @param context the application context whose environment should be updated;
   * never {@code null}
   * @param descriptors the property source descriptors to process; potentially
   * empty but never {@code null}
   * @throws IllegalStateException if an error occurs while processing the
   * descriptors and registering property sources
   * @see TestPropertySource#locations
   * @see TestPropertySource#encoding
   * @see TestPropertySource#factory
   * @see PropertySourceFactory
   * @see #addPropertySourcesToEnvironment(ConfigurableEnvironment, ResourceLoader, List)
   */
  public static void addPropertySourcesToEnvironment(ConfigurableApplicationContext context,
      List<PropertySourceDescriptor> descriptors) {

    Assert.notNull(context, "'context' must not be null");
    Assert.notNull(descriptors, "'descriptors' must not be null");
    addPropertySourcesToEnvironment(context.getEnvironment(), context, descriptors);
  }

  /**
   * Add property sources for the given {@code descriptors} to the supplied
   * {@link ConfigurableEnvironment environment}.
   * <p>Property placeholders in resource locations (i.e., <code>${...}</code>)
   * will be {@linkplain Environment#resolveRequiredPlaceholders(String) resolved}
   * against the {@code Environment}.
   * <p>Each {@link PropertySource} will be created via the configured
   * {@link PropertySourceDescriptor#propertySourceFactory() PropertySourceFactory}
   * (or the {@link DefaultPropertySourceFactory} if no factory is configured)
   * and added to the {@link PropertySources} of the environment with the highest
   * precedence.
   *
   * @param environment the environment to update; never {@code null}
   * @param resourceLoader the {@code ResourceLoader} to use to load each resource;
   * never {@code null}
   * @param descriptors the property source descriptors to process; potentially
   * empty but never {@code null}
   * @throws IllegalStateException if an error occurs while processing the
   * descriptors and registering property sources
   * @see TestPropertySource#locations
   * @see TestPropertySource#encoding
   * @see TestPropertySource#factory
   * @see PropertySourceFactory
   */
  public static void addPropertySourcesToEnvironment(ConfigurableEnvironment environment,
      ResourceLoader resourceLoader, List<PropertySourceDescriptor> descriptors) {

    Assert.notNull(environment, "'environment' must not be null");
    Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
    Assert.notNull(descriptors, "'descriptors' must not be null");
    PropertySources propertySources = environment.getPropertySources();
    try {
      for (PropertySourceDescriptor descriptor : descriptors) {
        if (!descriptor.locations().isEmpty()) {
          Class<? extends PropertySourceFactory> factoryClass = descriptor.propertySourceFactory();
          PropertySourceFactory factory =
              (factoryClass != null && factoryClass != PropertySourceFactory.class ?
               BeanUtils.newInstance(factoryClass) : defaultPropertySourceFactory);

          for (String location : descriptor.locations()) {
            String resolvedLocation = environment.resolveRequiredPlaceholders(location);
            Resource resource = resourceLoader.getResource(resolvedLocation);
            PropertySource<?> propertySource = factory.createPropertySource(descriptor.name(),
                new EncodedResource(resource, descriptor.encoding()));
            propertySources.addFirst(propertySource);
          }
        }
      }
    }
    catch (IOException ex) {
      throw new IllegalStateException("Failed to add PropertySource to Environment", ex);
    }
  }

  /**
   * Add the given <em>inlined properties</em> to the {@link Environment} of the
   * supplied {@code context}.
   * <p>This method simply delegates to
   * {@link #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])}.
   *
   * @param context the application context whose environment should be updated;
   * never {@code null}
   * @param inlinedProperties the inlined properties to add to the environment;
   * potentially empty but never {@code null}
   * @see TestPropertySource#properties
   * @see #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])
   */
  public static void addInlinedPropertiesToEnvironment(ConfigurableApplicationContext context, String... inlinedProperties) {
    Assert.notNull(context, "'context' must not be null");
    Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
    addInlinedPropertiesToEnvironment(context.getEnvironment(), inlinedProperties);
  }

  /**
   * Add the given <em>inlined properties</em> (in the form of <em>key-value</em>
   * pairs) to the supplied {@link ConfigurableEnvironment environment}.
   * <p>All key-value pairs will be added to the {@code Environment} as a
   * single {@link MapPropertySource} with the highest precedence.
   * <p>For details on the parsing of <em>inlined properties</em>, consult the
   * Javadoc for {@link #convertInlinedPropertiesToMap}.
   *
   * @param environment the environment to update; never {@code null}
   * @param inlinedProperties the inlined properties to add to the environment;
   * potentially empty but never {@code null}
   * @see MapPropertySource
   * @see #INLINED_PROPERTIES_PROPERTY_SOURCE_NAME
   * @see TestPropertySource#properties
   * @see #convertInlinedPropertiesToMap
   */
  public static void addInlinedPropertiesToEnvironment(ConfigurableEnvironment environment, String... inlinedProperties) {
    Assert.notNull(environment, "'environment' must not be null");
    Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
    if (!ObjectUtils.isEmpty(inlinedProperties)) {
      if (logger.isTraceEnabled()) {
        logger.trace("Adding inlined properties to environment: " +
            ObjectUtils.nullSafeToString(inlinedProperties));
      }
      MapPropertySource ps = (MapPropertySource)
          environment.getPropertySources().get(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
      if (ps == null) {
        ps = new MapPropertySource(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME, new LinkedHashMap<>());
        environment.getPropertySources().addFirst(ps);
      }
      ps.getSource().putAll(convertInlinedPropertiesToMap(inlinedProperties));
    }
  }

  /**
   * Convert the supplied <em>inlined properties</em> (in the form of <em>key-value</em>
   * pairs) into a map keyed by property name, preserving the ordering of property names
   * in the returned map.
   * <p>Parsing of the key-value pairs is achieved by converting all pairs
   * into <em>virtual</em> properties files in memory and delegating to
   * {@link Properties#load(java.io.Reader)} to parse each virtual file.
   * <p>For a full discussion of <em>inlined properties</em>, consult the Javadoc
   * for {@link TestPropertySource#properties}.
   *
   * @param inlinedProperties the inlined properties to convert; potentially empty
   * but never {@code null}
   * @return a new, ordered map containing the converted properties
   * @throws IllegalStateException if a given key-value pair cannot be parsed, or if
   * a given inlined property contains multiple key-value pairs
   * @see #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])
   */
  public static Map<String, Object> convertInlinedPropertiesToMap(String... inlinedProperties) {
    Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
    Map<String, Object> map = new LinkedHashMap<>();
    Properties props = new Properties();

    for (String pair : inlinedProperties) {
      if (!StringUtils.hasText(pair)) {
        continue;
      }
      try {
        props.load(new StringReader(pair));
      }
      catch (Exception ex) {
        throw new IllegalStateException("Failed to load test environment property from [" + pair + "]", ex);
      }
      Assert.state(props.size() == 1, () -> "Failed to load exactly one test environment property from [" + pair + "]");
      for (String name : props.stringPropertyNames()) {
        map.put(name, props.getProperty(name));
      }
      props.clear();
    }

    return map;
  }

  private static <T extends Annotation> List<List<MergedAnnotation<T>>> findRepeatableAnnotations(
      Class<?> clazz, Class<T> annotationType) {

    List<List<MergedAnnotation<T>>> listOfLists = new ArrayList<>();
    findRepeatableAnnotations(clazz, annotationType, listOfLists, new int[] { 0 });
    return listOfLists;
  }

  private static <T extends Annotation> void findRepeatableAnnotations(
      Class<?> clazz, Class<T> annotationType, List<List<MergedAnnotation<T>>> listOfLists, int[] aggregateIndex) {

    // Ensure we have a list for the current aggregate index.
    if (listOfLists.size() < aggregateIndex[0] + 1) {
      listOfLists.add(new ArrayList<>());
    }

    MergedAnnotations.from(clazz, SearchStrategy.DIRECT)
        .stream(annotationType)
        .sorted(highMetaDistancesFirst())
        .forEach(annotation -> listOfLists.get(aggregateIndex[0]).add(0, annotation));

    aggregateIndex[0]++;

    // Declared on an interface?
    for (Class<?> ifc : clazz.getInterfaces()) {
      findRepeatableAnnotations(ifc, annotationType, listOfLists, aggregateIndex);
    }

    // Declared on a superclass?
    Class<?> superclass = clazz.getSuperclass();
    if (superclass != null & superclass != Object.class) {
      findRepeatableAnnotations(superclass, annotationType, listOfLists, aggregateIndex);
    }

    // Declared on an enclosing class of an inner class?
    if (TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
      findRepeatableAnnotations(clazz.getEnclosingClass(), annotationType, listOfLists, aggregateIndex);
    }
  }

  private static <A extends Annotation> Comparator<MergedAnnotation<A>> highMetaDistancesFirst() {
    return Comparator.<MergedAnnotation<A>>comparingInt(MergedAnnotation::getDistance).reversed();
  }

}
