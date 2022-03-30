/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.test.context;

import java.util.Arrays;

import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@code ContextConfigurationAttributes} encapsulates the context configuration
 * attributes declared via {@link ContextConfiguration @ContextConfiguration}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @see ContextConfiguration
 * @see SmartContextLoader#processContextConfiguration(ContextConfigurationAttributes)
 * @see MergedContextConfiguration
 * @since 4.0
 */
public class ContextConfigurationAttributes {

  private static final String[] EMPTY_LOCATIONS = new String[0];

  private static final Class<?>[] EMPTY_CLASSES = new Class<?>[0];

  private static final Logger logger = LoggerFactory.getLogger(ContextConfigurationAttributes.class);

  private final Class<?> declaringClass;

  private Class<?>[] classes;

  private String[] locations;

  private final boolean inheritLocations;

  private final Class<? extends ApplicationContextInitializer>[] initializers;

  private final boolean inheritInitializers;

  @Nullable
  private final String name;

  private final Class<? extends ContextLoader> contextLoaderClass;

  /**
   * Construct a new {@link ContextConfigurationAttributes} instance with default values.
   *
   * @param declaringClass the test class that declared {@code @ContextConfiguration},
   * either explicitly or implicitly
   */
  @SuppressWarnings("unchecked")
  public ContextConfigurationAttributes(Class<?> declaringClass) {
    this(declaringClass, EMPTY_LOCATIONS, EMPTY_CLASSES, false, (Class[]) EMPTY_CLASSES, true, ContextLoader.class);
  }

  /**
   * Construct a new {@link ContextConfigurationAttributes} instance for the
   * supplied {@link ContextConfiguration @ContextConfiguration} annotation and
   * the {@linkplain Class test class} that declared it.
   *
   * @param declaringClass the test class that declared {@code @ContextConfiguration}
   * @param contextConfiguration the annotation from which to retrieve the attributes
   */
  public ContextConfigurationAttributes(Class<?> declaringClass, ContextConfiguration contextConfiguration) {
    this(declaringClass, contextConfiguration.locations(), contextConfiguration.classes(),
            contextConfiguration.inheritLocations(), contextConfiguration.initializers(),
            contextConfiguration.inheritInitializers(), contextConfiguration.name(), contextConfiguration.loader());
  }

  /**
   * Construct a new {@link ContextConfigurationAttributes} instance for the
   * supplied {@link AnnotationAttributes} (parsed from a
   * {@link ContextConfiguration @ContextConfiguration} annotation) and
   * the {@linkplain Class test class} that declared them.
   *
   * @param declaringClass the test class that declared {@code @ContextConfiguration}
   * @param annAttrs the annotation attributes from which to retrieve the attributes
   */
  @SuppressWarnings("unchecked")
  public ContextConfigurationAttributes(Class<?> declaringClass, AnnotationAttributes annAttrs) {
    this(declaringClass, annAttrs.getStringArray("locations"), annAttrs.getClassArray("classes"),
            annAttrs.getBoolean("inheritLocations"),
            annAttrs.getClassArray("initializers"),
            annAttrs.getBoolean("inheritInitializers"), annAttrs.getString("name"), annAttrs.getClass("loader"));
  }

  /**
   * Construct a new {@link ContextConfigurationAttributes} instance for the
   * {@linkplain Class test class} that declared the
   * {@link ContextConfiguration @ContextConfiguration} annotation and its
   * corresponding attributes.
   *
   * @param declaringClass the test class that declared {@code @ContextConfiguration}
   * @param locations the resource locations declared via {@code @ContextConfiguration}
   * @param classes the annotated classes declared via {@code @ContextConfiguration}
   * @param inheritLocations the {@code inheritLocations} flag declared via {@code @ContextConfiguration}
   * @param initializers the context initializers declared via {@code @ContextConfiguration}
   * @param inheritInitializers the {@code inheritInitializers} flag declared via {@code @ContextConfiguration}
   * @param contextLoaderClass the {@code ContextLoader} class declared via {@code @ContextConfiguration}
   * @throws IllegalArgumentException if the {@code declaringClass} or {@code contextLoaderClass} is
   * {@code null}
   */
  public ContextConfigurationAttributes(
          Class<?> declaringClass, String[] locations, Class<?>[] classes, boolean inheritLocations,
          Class<? extends ApplicationContextInitializer>[] initializers,
          boolean inheritInitializers, Class<? extends ContextLoader> contextLoaderClass) {

    this(declaringClass, locations, classes, inheritLocations, initializers, inheritInitializers, null,
            contextLoaderClass);
  }

  /**
   * Construct a new {@link ContextConfigurationAttributes} instance for the
   * {@linkplain Class test class} that declared the
   * {@link ContextConfiguration @ContextConfiguration} annotation and its
   * corresponding attributes.
   *
   * @param declaringClass the test class that declared {@code @ContextConfiguration}
   * @param locations the resource locations declared via {@code @ContextConfiguration}
   * @param classes the annotated classes declared via {@code @ContextConfiguration}
   * @param inheritLocations the {@code inheritLocations} flag declared via {@code @ContextConfiguration}
   * @param initializers the context initializers declared via {@code @ContextConfiguration}
   * @param inheritInitializers the {@code inheritInitializers} flag declared via {@code @ContextConfiguration}
   * @param name the name of level in the context hierarchy, or {@code null} if not applicable
   * @param contextLoaderClass the {@code ContextLoader} class declared via {@code @ContextConfiguration}
   * @throws IllegalArgumentException if the {@code declaringClass} or {@code contextLoaderClass} is
   * {@code null}
   */
  public ContextConfigurationAttributes(
          Class<?> declaringClass, String[] locations, Class<?>[] classes, boolean inheritLocations,
          Class<? extends ApplicationContextInitializer>[] initializers,
          boolean inheritInitializers, @Nullable String name, Class<? extends ContextLoader> contextLoaderClass) {

    Assert.notNull(declaringClass, "'declaringClass' must not be null");
    Assert.notNull(contextLoaderClass, "'contextLoaderClass' must not be null");

    if (ObjectUtils.isNotEmpty(locations) && ObjectUtils.isNotEmpty(classes) && logger.isDebugEnabled()) {
      logger.debug(String.format(
              "Test class [%s] has been configured with @ContextConfiguration's 'locations' (or 'value') %s " +
                      "and 'classes' %s attributes. Most SmartContextLoader implementations support " +
                      "only one declaration of resources per @ContextConfiguration annotation.",
              declaringClass.getName(), ObjectUtils.nullSafeToString(locations),
              ObjectUtils.nullSafeToString(classes)));
    }

    this.declaringClass = declaringClass;
    this.locations = locations;
    this.classes = classes;
    this.inheritLocations = inheritLocations;
    this.initializers = initializers;
    this.inheritInitializers = inheritInitializers;
    this.name = (StringUtils.hasText(name) ? name : null);
    this.contextLoaderClass = contextLoaderClass;
  }

  /**
   * Get the {@linkplain Class class} that declared the
   * {@link ContextConfiguration @ContextConfiguration} annotation, either explicitly
   * or implicitly.
   *
   * @return the declaring class (never {@code null})
   */
  public Class<?> getDeclaringClass() {
    return this.declaringClass;
  }

  /**
   * Set the <em>processed</em> annotated classes, effectively overriding the
   * original value declared via {@link ContextConfiguration @ContextConfiguration}.
   *
   * @see #getClasses()
   */
  public void setClasses(Class<?>... classes) {
    this.classes = classes;
  }

  /**
   * Get the annotated classes that were declared via
   * {@link ContextConfiguration @ContextConfiguration}.
   * <p>Note: this is a mutable property. The returned value may therefore
   * represent a <em>processed</em> value that does not match the original value
   * declared via {@link ContextConfiguration @ContextConfiguration}.
   *
   * @return the annotated classes (potentially {<em>empty</em>)
   * @see ContextConfiguration#classes
   * @see #setClasses(Class[])
   */
  public Class<?>[] getClasses() {
    return this.classes;
  }

  /**
   * Determine if this {@code ContextConfigurationAttributes} instance has
   * class-based resources.
   *
   * @return {@code true} if the {@link #getClasses() classes} array is not empty
   * @see #hasResources()
   * @see #hasLocations()
   */
  public boolean hasClasses() {
    return (getClasses().length > 0);
  }

  /**
   * Set the <em>processed</em> resource locations, effectively overriding the
   * original value declared via {@link ContextConfiguration @ContextConfiguration}.
   *
   * @see #getLocations()
   */
  public void setLocations(String... locations) {
    this.locations = locations;
  }

  /**
   * Get the resource locations that were declared via
   * {@link ContextConfiguration @ContextConfiguration}.
   * <p>Note: this is a mutable property. The returned value may therefore
   * represent a <em>processed</em> value that does not match the original value
   * declared via {@link ContextConfiguration @ContextConfiguration}.
   *
   * @return the resource locations (potentially <em>empty</em>)
   * @see ContextConfiguration#value
   * @see ContextConfiguration#locations
   * @see #setLocations
   */
  public String[] getLocations() {
    return this.locations;
  }

  /**
   * Determine if this {@code ContextConfigurationAttributes} instance has
   * path-based resource locations.
   *
   * @return {@code true} if the {@link #getLocations() locations} array is not empty
   * @see #hasResources()
   * @see #hasClasses()
   */
  public boolean hasLocations() {
    return (getLocations().length > 0);
  }

  /**
   * Determine if this {@code ContextConfigurationAttributes} instance has
   * either path-based resource locations or class-based resources.
   *
   * @return {@code true} if either the {@link #getLocations() locations}
   * or the {@link #getClasses() classes} array is not empty
   * @see #hasLocations()
   * @see #hasClasses()
   */
  public boolean hasResources() {
    return (hasLocations() || hasClasses());
  }

  /**
   * Get the {@code inheritLocations} flag that was declared via
   * {@link ContextConfiguration @ContextConfiguration}.
   *
   * @return the {@code inheritLocations} flag
   * @see ContextConfiguration#inheritLocations
   */
  public boolean isInheritLocations() {
    return this.inheritLocations;
  }

  /**
   * Get the {@code ApplicationContextInitializer} classes that were declared via
   * {@link ContextConfiguration @ContextConfiguration}.
   *
   * @return the {@code ApplicationContextInitializer} classes
   */
  public Class<? extends ApplicationContextInitializer>[] getInitializers() {
    return this.initializers;
  }

  /**
   * Get the {@code inheritInitializers} flag that was declared via
   * {@link ContextConfiguration @ContextConfiguration}.
   *
   * @return the {@code inheritInitializers} flag
   */
  public boolean isInheritInitializers() {
    return this.inheritInitializers;
  }

  /**
   * Get the name of the context hierarchy level that was declared via
   * {@link ContextConfiguration @ContextConfiguration}.
   *
   * @return the name of the context hierarchy level or {@code null} if not applicable
   * @see ContextConfiguration#name()
   */
  @Nullable
  public String getName() {
    return this.name;
  }

  /**
   * Get the {@code ContextLoader} class that was declared via
   * {@link ContextConfiguration @ContextConfiguration}.
   *
   * @return the {@code ContextLoader} class
   * @see ContextConfiguration#loader
   */
  public Class<? extends ContextLoader> getContextLoaderClass() {
    return this.contextLoaderClass;
  }

  /**
   * Determine if the supplied object is equal to this
   * {@code ContextConfigurationAttributes} instance by comparing both object's
   * {@linkplain #getDeclaringClass() declaring class},
   * {@linkplain #getLocations() locations},
   * {@linkplain #getClasses() annotated classes},
   * {@linkplain #isInheritLocations() inheritLocations flag},
   * {@linkplain #getInitializers() context initializer classes},
   * {@linkplain #isInheritInitializers() inheritInitializers flag}, and the
   * {@link #getContextLoaderClass() ContextLoader class}.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ContextConfigurationAttributes otherAttr)) {
      return false;
    }
    return (ObjectUtils.nullSafeEquals(this.declaringClass, otherAttr.declaringClass) &&
            Arrays.equals(this.classes, otherAttr.classes)) &&
            Arrays.equals(this.locations, otherAttr.locations) &&
            this.inheritLocations == otherAttr.inheritLocations &&
            Arrays.equals(this.initializers, otherAttr.initializers) &&
            this.inheritInitializers == otherAttr.inheritInitializers &&
            ObjectUtils.nullSafeEquals(this.name, otherAttr.name) &&
            ObjectUtils.nullSafeEquals(this.contextLoaderClass, otherAttr.contextLoaderClass);
  }

  /**
   * Generate a unique hash code for all properties of this
   * {@code ContextConfigurationAttributes} instance excluding the
   * {@linkplain #getName() name}.
   */
  @Override
  public int hashCode() {
    int result = this.declaringClass.hashCode();
    result = 31 * result + Arrays.hashCode(this.classes);
    result = 31 * result + Arrays.hashCode(this.locations);
    result = 31 * result + Arrays.hashCode(this.initializers);
    return result;
  }

  /**
   * Provide a String representation of the context configuration attributes
   * and declaring class.
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("declaringClass", this.declaringClass.getName())
            .append("classes", ObjectUtils.nullSafeToString(this.classes))
            .append("locations", ObjectUtils.nullSafeToString(this.locations))
            .append("inheritLocations", this.inheritLocations)
            .append("initializers", ObjectUtils.nullSafeToString(this.initializers))
            .append("inheritInitializers", this.inheritInitializers)
            .append("name", this.name)
            .append("contextLoaderClass", this.contextLoaderClass.getName())
            .toString();
  }

}
