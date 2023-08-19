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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.PropertySourceDescriptor;
import cn.taketoday.core.io.PropertySourceFactory;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.util.TestContextResourceUtils;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@code TestPropertySourceAttributes} encapsulates attributes declared
 * via {@link TestPropertySource @TestPropertySource} annotations.
 *
 * <p>In addition to encapsulating declared attributes,
 * {@code TestPropertySourceAttributes} also enforces configuration rules.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TestPropertySource
 * @see MergedTestPropertySources
 * @since 4.0
 */
class TestPropertySourceAttributes {

  private static final String SLASH = "/";
  private static final Logger logger = LoggerFactory.getLogger(TestPropertySourceAttributes.class);

  private final Class<?> declaringClass;

  private final MergedAnnotation<?> rootAnnotation;

  private final ArrayList<PropertySourceDescriptor> descriptors = new ArrayList<>();

  private final boolean inheritLocations;

  private final ArrayList<String> properties = new ArrayList<>();

  private final boolean inheritProperties;

  TestPropertySourceAttributes(MergedAnnotation<TestPropertySource> mergedAnnotation) {
    this.declaringClass = declaringClass(mergedAnnotation);
    this.rootAnnotation = mergedAnnotation.getRoot();
    this.inheritLocations = mergedAnnotation.getBoolean("inheritLocations");
    this.inheritProperties = mergedAnnotation.getBoolean("inheritProperties");
    addPropertiesAndLocationsFrom(mergedAnnotation, this.declaringClass);
  }

  /**
   * Merge this {@code TestPropertySourceAttributes} instance with the
   * supplied {@code TestPropertySourceAttributes}, asserting that the two sets
   * of test property source attributes have identical values for the
   * {@link TestPropertySource#inheritLocations} and
   * {@link TestPropertySource#inheritProperties} flags and that the two
   * underlying annotations were declared on the same class.
   */
  void mergeWith(TestPropertySourceAttributes attributes) {
    Assert.state(attributes.declaringClass == this.declaringClass,
            () -> "Detected @TestPropertySource declarations within an aggregate index "
                    + "with different sources: " + this.declaringClass.getName() + " and "
                    + attributes.declaringClass.getName());
    logger.trace("Retrieved {} for declaring class [{}].", attributes, this.declaringClass.getName());

    assertSameBooleanAttribute(this.inheritLocations, attributes.inheritLocations, "inheritLocations", attributes);
    assertSameBooleanAttribute(this.inheritProperties, attributes.inheritProperties, "inheritProperties", attributes);

    mergePropertiesAndLocationsFrom(attributes);
  }

  private void assertSameBooleanAttribute(boolean expected, boolean actual,
          String attributeName, TestPropertySourceAttributes that) {
    Assert.isTrue(expected == actual, () -> String.format(
            "@%s on %s and @%s on %s must declare the same value for '%s' as other " +
                    "directly present or meta-present @TestPropertySource annotations",
            this.rootAnnotation.getType().getSimpleName(), this.declaringClass.getSimpleName(),
            that.rootAnnotation.getType().getSimpleName(), that.declaringClass.getSimpleName(),
            attributeName));
  }

  private void addPropertiesAndLocationsFrom(
          MergedAnnotation<TestPropertySource> mergedAnnotation, Class<?> declaringClass) {

    String[] locations = mergedAnnotation.getStringArray("locations");
    String[] properties = mergedAnnotation.getStringArray("properties");
    String[] convertedLocations =
            TestContextResourceUtils.convertToClasspathResourcePaths(declaringClass, true, locations);
    Class<? extends PropertySourceFactory> factoryClass = mergedAnnotation.getClass("factory");
    if (factoryClass == PropertySourceFactory.class) {
      factoryClass = null; // default factory type will be inferred
    }

    String encoding = mergedAnnotation.getString("encoding");
    if (encoding.isBlank()) {
      encoding = null; // default encoding will be inferred
    }
    PropertySourceDescriptor descriptor = new PropertySourceDescriptor(
            List.of(convertedLocations), false, null, factoryClass, encoding);
    addPropertiesAndLocations(List.of(descriptor), properties, declaringClass, encoding, false);
  }

  private void mergePropertiesAndLocationsFrom(TestPropertySourceAttributes attributes) {
    addPropertiesAndLocations(attributes.getPropertySourceDescriptors(), attributes.getProperties(),
            attributes.getDeclaringClass(), null, true);
  }

  private void addPropertiesAndLocations(List<PropertySourceDescriptor> descriptors,
          String[] properties, Class<?> declaringClass, @Nullable String encoding, boolean prepend) {

    if (hasNoLocations(descriptors) && ObjectUtils.isEmpty(properties)) {
      String defaultPropertiesFile = detectDefaultPropertiesFile(declaringClass);
      PropertySourceDescriptor descriptor = new PropertySourceDescriptor(
              List.of(defaultPropertiesFile), false, null, null, encoding);
      addAll(prepend, this.descriptors, List.of(descriptor));
    }
    else {
      addAll(prepend, this.descriptors, descriptors);
      addAll(prepend, this.properties, properties);
    }
  }

  /**
   * Add all the supplied elements to the provided list, honoring the
   * {@code prepend} flag.
   * <p>If the {@code prepend} flag is {@code false}, the elements will be appended
   * to the list.
   *
   * @param prepend whether the elements should be prepended to the list
   * @param list the list to which to add the elements
   * @param elements the elements to add to the list
   */
  private void addAll(boolean prepend, List<PropertySourceDescriptor> list, List<PropertySourceDescriptor> elements) {
    list.addAll((prepend ? 0 : list.size()), elements);
  }

  /**
   * Add all the supplied elements to the provided list, honoring the
   * {@code prepend} flag.
   * <p>If the {@code prepend} flag is {@code false}, the elements will be appended
   * to the list.
   *
   * @param prepend whether the elements should be prepended to the list
   * @param list the list to which to add the elements
   * @param elements the elements to add to the list
   */
  private void addAll(boolean prepend, List<String> list, String... elements) {
    list.addAll((prepend ? 0 : list.size()), Arrays.asList(elements));
  }

  private String detectDefaultPropertiesFile(Class<?> testClass) {
    String resourcePath = ClassUtils.convertClassNameToResourcePath(testClass.getName()) + ".properties";
    ClassPathResource classPathResource = new ClassPathResource(resourcePath);
    if (!classPathResource.exists()) {
      String msg = String.format(
              "Could not detect default properties file for test class [%s]: " +
                      "%s does not exist. Either declare the 'locations' or 'properties' attributes " +
                      "of @TestPropertySource or make the default properties file available.",
              testClass.getName(), classPathResource);
      logger.error(msg);
      throw new IllegalStateException(msg);
    }
    String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + SLASH + resourcePath;
    if (logger.isDebugEnabled()) {
      logger.debug("Detected default properties file \"{}\" for test class [{}]",
              prefixedResourcePath, testClass.getName());
    }
    return prefixedResourcePath;
  }

  /**
   * Get the {@linkplain Class class} that declared {@code @TestPropertySource}.
   *
   * @return the declaring class; never {@code null}
   */
  Class<?> getDeclaringClass() {
    return this.declaringClass;
  }

  /**
   * Get the descriptors for resource locations that were declared via
   * {@code @TestPropertySource}.
   * <p>Note: The returned value may represent a <em>detected default</em>
   * or merged locations that do not match the original value declared via a
   * single {@code @TestPropertySource} annotation.
   *
   * @return the resource location descriptors; potentially <em>empty</em>
   * @see TestPropertySource#value
   * @see TestPropertySource#locations
   * @see TestPropertySource#factory
   */
  List<PropertySourceDescriptor> getPropertySourceDescriptors() {
    return this.descriptors;
  }

  /**
   * Get the {@code inheritLocations} flag that was declared via {@code @TestPropertySource}.
   *
   * @return the {@code inheritLocations} flag
   * @see TestPropertySource#inheritLocations
   */
  boolean isInheritLocations() {
    return this.inheritLocations;
  }

  /**
   * Get the inlined properties that were declared via {@code @TestPropertySource}.
   * <p>Note: The returned value may represent merged properties that do not
   * match the original value declared via a single {@code @TestPropertySource}
   * annotation.
   *
   * @return the inlined properties; potentially <em>empty</em>
   * @see TestPropertySource#properties
   */
  String[] getProperties() {
    return StringUtils.toStringArray(this.properties);
  }

  /**
   * Get the {@code inheritProperties} flag that was declared via {@code @TestPropertySource}.
   *
   * @return the {@code inheritProperties} flag
   * @see TestPropertySource#inheritProperties
   */
  boolean isInheritProperties() {
    return this.inheritProperties;
  }

  boolean isEmpty() {
    return hasNoLocations(this.descriptors) && this.properties.isEmpty();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || other.getClass() != getClass()) {
      return false;
    }

    TestPropertySourceAttributes that = (TestPropertySourceAttributes) other;
    if (!this.descriptors.equals(that.descriptors)) {
      return false;
    }
    if (!this.properties.equals(that.properties)) {
      return false;
    }
    if (this.inheritLocations != that.inheritLocations) {
      return false;
    }
    return this.inheritProperties == that.inheritProperties;
  }

  @Override
  public int hashCode() {
    int result = this.descriptors.hashCode();
    result = 31 * result + this.properties.hashCode();
    result = 31 * result + (this.inheritLocations ? 1231 : 1237);
    result = 31 * result + (this.inheritProperties ? 1231 : 1237);
    return result;
  }

  /**
   * Provide a String representation of the {@code @TestPropertySource}
   * attributes and declaring class.
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("declaringClass", this.declaringClass.getName())
            .append("descriptors", this.descriptors)
            .append("inheritLocations", this.inheritLocations)
            .append("properties", this.properties)
            .append("inheritProperties", this.inheritProperties)
            .toString();
  }

  private static Class<?> declaringClass(MergedAnnotation<?> mergedAnnotation) {
    Object source = mergedAnnotation.getSource();
    Assert.state(source instanceof Class, "No source class available");
    return (Class<?>) source;
  }

  /**
   * Determine if the supplied list contains no descriptor with locations.
   */
  private static boolean hasNoLocations(List<PropertySourceDescriptor> descriptors) {
    return descriptors.stream()
            .map(PropertySourceDescriptor::locations)
            .flatMap(List::stream)
            .findAny()
            .isEmpty();
  }

}
