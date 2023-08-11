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

package cn.taketoday.test.context;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.core.io.PropertySourceDescriptor;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.cache.ContextCache;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@code MergedContextConfiguration} encapsulates the <em>merged</em>
 * context configuration declared on a test class and all of its superclasses
 * via {@link ContextConfiguration @ContextConfiguration},
 * {@link ActiveProfiles @ActiveProfiles}, and
 * {@link TestPropertySource @TestPropertySource}.
 *
 * <p>Merged context resource locations, annotated classes, active profiles,
 * property resource locations, and in-lined properties represent all declared
 * values in the test class hierarchy taking into consideration the semantics
 * of the {@link ContextConfiguration#inheritLocations},
 * {@link ActiveProfiles#inheritProfiles},
 * {@link TestPropertySource#inheritLocations}, and
 * {@link TestPropertySource#inheritProperties} flags.
 *
 * <p>A {@link SmartContextLoader} uses {@code MergedContextConfiguration}
 * to load an {@link cn.taketoday.context.ApplicationContext ApplicationContext}.
 *
 * <p>{@code MergedContextConfiguration} is also used by the
 * {@link ContextCache ContextCache}
 * as the key for caching an
 * {@link cn.taketoday.context.ApplicationContext ApplicationContext}
 * that was loaded using properties of this {@code MergedContextConfiguration}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ContextConfiguration
 * @see ContextHierarchy
 * @see ActiveProfiles
 * @see TestPropertySource
 * @see ContextConfigurationAttributes
 * @see SmartContextLoader#loadContext(MergedContextConfiguration)
 * @since 4.0
 */
public class MergedContextConfiguration implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

  private static final Set<Class<? extends ApplicationContextInitializer>>
      EMPTY_INITIALIZER_CLASSES = Collections.emptySet();

  private static final Set<ContextCustomizer> EMPTY_CONTEXT_CUSTOMIZERS = Collections.emptySet();

  private final Class<?> testClass;

  private final String[] locations;

  private final Class<?>[] classes;

  private final Set<Class<? extends ApplicationContextInitializer>> contextInitializerClasses;

  private final String[] activeProfiles;

  private final List<PropertySourceDescriptor> propertySourceDescriptors;

  private final String[] propertySourceLocations;

  private final String[] propertySourceProperties;

  private final Set<ContextCustomizer> contextCustomizers;

  @Nullable
  private final ContextLoader contextLoader;

  @Nullable
  private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

  @Nullable
  private final MergedContextConfiguration parent;

  /**
   * Create a new {@code MergedContextConfiguration} instance for the
   * supplied parameters.
   *
   * @param testClass the test class for which the configuration was merged
   * @param locations the merged context resource locations
   * @param classes the merged annotated classes
   * @param activeProfiles the merged active bean definition profiles
   * @param contextLoader the resolved {@code ContextLoader}
   */
  public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
      @Nullable String[] activeProfiles, ContextLoader contextLoader) {

    this(testClass, locations, classes, null, activeProfiles, contextLoader);
  }

  /**
   * Create a new {@code MergedContextConfiguration} instance for the
   * supplied parameters.
   *
   * @param testClass the test class for which the configuration was merged
   * @param locations the merged context resource locations
   * @param classes the merged annotated classes
   * @param contextInitializerClasses the merged context initializer classes
   * @param activeProfiles the merged active bean definition profiles
   * @param contextLoader the resolved {@code ContextLoader}
   */
  public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
      @Nullable Set<Class<? extends ApplicationContextInitializer>> contextInitializerClasses,
      @Nullable String[] activeProfiles, ContextLoader contextLoader) {

    this(testClass, locations, classes, contextInitializerClasses, activeProfiles, contextLoader, null, null);
  }

  /**
   * Create a new {@code MergedContextConfiguration} instance for the
   * supplied parameters.
   *
   * @param testClass the test class for which the configuration was merged
   * @param locations the merged context resource locations
   * @param classes the merged annotated classes
   * @param contextInitializerClasses the merged context initializer classes
   * @param activeProfiles the merged active bean definition profiles
   * @param contextLoader the resolved {@code ContextLoader}
   * @param cacheAwareContextLoaderDelegate a cache-aware context loader
   * delegate with which to retrieve the parent {@code ApplicationContext}
   * @param parent the parent configuration or {@code null} if there is no parent
   */
  public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
      @Nullable Set<Class<? extends ApplicationContextInitializer>> contextInitializerClasses,
      @Nullable String[] activeProfiles, ContextLoader contextLoader,
      @Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
      @Nullable MergedContextConfiguration parent) {

    this(testClass, locations, classes, contextInitializerClasses, activeProfiles, null, null,
        contextLoader, cacheAwareContextLoaderDelegate, parent);
  }

  /**
   * Create a new {@code MergedContextConfiguration} instance by copying
   * all fields from the supplied {@code MergedContextConfiguration}.
   */
  public MergedContextConfiguration(MergedContextConfiguration mergedConfig) {
    this(mergedConfig.testClass, mergedConfig.locations, mergedConfig.classes,
        mergedConfig.contextInitializerClasses, mergedConfig.activeProfiles,
        mergedConfig.propertySourceDescriptors, mergedConfig.propertySourceProperties,
        mergedConfig.contextCustomizers, mergedConfig.contextLoader,
        mergedConfig.cacheAwareContextLoaderDelegate, mergedConfig.parent);
  }

  /**
   * Create a new {@code MergedContextConfiguration} instance for the
   * supplied parameters.
   * <p>If a {@code null} value is supplied for {@code locations},
   * {@code classes}, {@code activeProfiles}, {@code propertySourceLocations},
   * or {@code propertySourceProperties} an empty array will be stored instead.
   * If a {@code null} value is supplied for the
   * {@code contextInitializerClasses} an empty set will be stored instead.
   * Furthermore, active profiles will be sorted, and duplicate profiles
   * will be removed.
   *
   * @param testClass the test class for which the configuration was merged
   * @param locations the merged context resource locations
   * @param classes the merged annotated classes
   * @param contextInitializerClasses the merged context initializer classes
   * @param activeProfiles the merged active bean definition profiles
   * @param propertySourceLocations the merged {@code PropertySource} locations
   * @param propertySourceProperties the merged {@code PropertySource} properties
   * @param contextLoader the resolved {@code ContextLoader}
   * @param cacheAwareContextLoaderDelegate a cache-aware context loader
   * delegate with which to retrieve the parent {@code ApplicationContext}
   * @param parent the parent configuration or {@code null} if there is no parent
   */
  public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
      @Nullable Set<Class<? extends ApplicationContextInitializer>> contextInitializerClasses,
      @Nullable String[] activeProfiles, @Nullable String[] propertySourceLocations,
      @Nullable String[] propertySourceProperties, ContextLoader contextLoader,
      @Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
      @Nullable MergedContextConfiguration parent) {

    this(testClass, locations, classes, contextInitializerClasses, activeProfiles,
        propertySourceLocations, propertySourceProperties,
        EMPTY_CONTEXT_CUSTOMIZERS, contextLoader,
        cacheAwareContextLoaderDelegate, parent);
  }

  /**
   * Create a new {@code MergedContextConfiguration} instance for the
   * supplied parameters.
   * <p>If a {@code null} value is supplied for {@code locations},
   * {@code classes}, {@code activeProfiles}, {@code propertySourceLocations},
   * or {@code propertySourceProperties} an empty array will be stored instead.
   * If a {@code null} value is supplied for {@code contextInitializerClasses}
   * or {@code contextCustomizers}, an empty set will be stored instead.
   * Furthermore, active profiles will be sorted, and duplicate profiles
   * will be removed.
   *
   * @param testClass the test class for which the configuration was merged
   * @param locations the merged context resource locations
   * @param classes the merged annotated classes
   * @param contextInitializerClasses the merged context initializer classes
   * @param activeProfiles the merged active bean definition profiles
   * @param propertySourceLocations the merged {@code PropertySource} locations
   * @param propertySourceProperties the merged {@code PropertySource} properties
   * @param contextCustomizers the context customizers
   * @param contextLoader the resolved {@code ContextLoader}
   * @param cacheAwareContextLoaderDelegate a cache-aware context loader
   * delegate with which to retrieve the parent {@code ApplicationContext}
   * @param parent the parent configuration or {@code null} if there is no parent
   */
  public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
      @Nullable Set<Class<? extends ApplicationContextInitializer>> contextInitializerClasses,
      @Nullable String[] activeProfiles, @Nullable String[] propertySourceLocations,
      @Nullable String[] propertySourceProperties, @Nullable Set<ContextCustomizer> contextCustomizers,
      ContextLoader contextLoader, @Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
      @Nullable MergedContextConfiguration parent) {

    this(testClass, locations, classes, contextInitializerClasses, activeProfiles,
        List.of(new PropertySourceDescriptor(processStrings(propertySourceLocations))),
        propertySourceProperties, contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate,
        parent);
  }

  /**
   * Create a new {@code MergedContextConfiguration} instance for the supplied
   * parameters.
   * <p>If a {@code null} value is supplied for {@code locations}, {@code classes},
   * {@code activeProfiles}, or {@code propertySourceProperties} an empty array
   * will be stored instead. If a {@code null} value is supplied for
   * {@code contextInitializerClasses} or {@code contextCustomizers}, an empty
   * set will be stored instead. Furthermore, active profiles will be sorted,
   * and duplicate profiles will be removed.
   *
   * @param testClass the test class for which the configuration was merged
   * @param locations the merged context resource locations
   * @param classes the merged annotated classes
   * @param contextInitializerClasses the merged context initializer classes
   * @param activeProfiles the merged active bean definition profiles
   * @param propertySourceDescriptors the merged property source descriptors
   * @param propertySourceProperties the merged inlined properties
   * @param contextCustomizers the context customizers
   * @param contextLoader the resolved {@code ContextLoader}
   * @param cacheAwareContextLoaderDelegate a cache-aware context loader
   * delegate with which to retrieve the parent {@code ApplicationContext}
   * @param parent the parent configuration or {@code null} if there is no parent
   */
  public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
      @Nullable Set<Class<? extends ApplicationContextInitializer>> contextInitializerClasses,
      @Nullable String[] activeProfiles, List<PropertySourceDescriptor> propertySourceDescriptors,
      @Nullable String[] propertySourceProperties, @Nullable Set<ContextCustomizer> contextCustomizers,
      @Nullable ContextLoader contextLoader, @Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
      @Nullable MergedContextConfiguration parent) {

    this.testClass = testClass;
    this.locations = processStrings(locations);
    this.classes = processClasses(classes);
    this.contextInitializerClasses = processContextInitializerClasses(contextInitializerClasses);
    this.activeProfiles = processActiveProfiles(activeProfiles);
    this.propertySourceDescriptors = Collections.unmodifiableList(propertySourceDescriptors);
    this.propertySourceLocations = this.propertySourceDescriptors.stream()
        .map(PropertySourceDescriptor::locations)
        .flatMap(List::stream)
        .toArray(String[]::new);
    this.propertySourceProperties = processStrings(propertySourceProperties);
    this.contextCustomizers = processContextCustomizers(contextCustomizers);
    this.contextLoader = contextLoader;
    this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
    this.parent = parent;
  }

  /**
   * Get the {@linkplain Class test class} associated with this
   * {@code MergedContextConfiguration}.
   */
  public Class<?> getTestClass() {
    return this.testClass;
  }

  /**
   * Get the merged resource locations for {@code ApplicationContext}
   * configuration files for the {@linkplain #getTestClass() test class}.
   * <p>Context resource locations typically represent XML configuration
   * files or Groovy scripts.
   */
  public String[] getLocations() {
    return this.locations;
  }

  /**
   * Get the merged annotated classes for the {@linkplain #getTestClass() test class}.
   */
  public Class<?>[] getClasses() {
    return this.classes;
  }

  /**
   * Determine if this {@code MergedContextConfiguration} instance has
   * path-based context resource locations.
   *
   * @return {@code true} if the {@link #getLocations() locations} array is not empty
   * @see #hasResources()
   * @see #hasClasses()
   */
  public boolean hasLocations() {
    return !ObjectUtils.isEmpty(getLocations());
  }

  /**
   * Determine if this {@code MergedContextConfiguration} instance has
   * class-based resources.
   *
   * @return {@code true} if the {@link #getClasses() classes} array is not empty
   * @see #hasResources()
   * @see #hasLocations()
   */
  public boolean hasClasses() {
    return !ObjectUtils.isEmpty(getClasses());
  }

  /**
   * Determine if this {@code MergedContextConfiguration} instance has
   * either path-based context resource locations or class-based resources.
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
   * Get the merged {@code ApplicationContextInitializer} classes for the
   * {@linkplain #getTestClass() test class}.
   */
  public Set<Class<? extends ApplicationContextInitializer>> getContextInitializerClasses() {
    return this.contextInitializerClasses;
  }

  /**
   * Get the merged active bean definition profiles for the
   * {@linkplain #getTestClass() test class}.
   *
   * @see ActiveProfiles
   */
  public String[] getActiveProfiles() {
    return this.activeProfiles;
  }

  /**
   * Get the merged descriptors for resource locations for test {@code PropertySources}
   * for the {@linkplain #getTestClass() test class}.
   * <p>Properties will be loaded into the {@code Environment}'s set of
   * {@code PropertySources}.
   *
   * @see TestPropertySource#locations
   * @see TestPropertySource#encoding
   * @see TestPropertySource#factory
   */
  public List<PropertySourceDescriptor> getPropertySourceDescriptors() {
    return this.propertySourceDescriptors;
  }

  /**
   * Get the merged resource locations of properties files for the
   * {@linkplain #getTestClass() test class}.
   *
   * @see TestPropertySource#locations
   * @see java.util.Properties
   */
  public String[] getPropertySourceLocations() {
    return this.propertySourceLocations;
  }

  /**
   * Get the merged inlined properties for the {@linkplain #getTestClass() test class}.
   * <p>Properties will be loaded into the {@code Environment}'s set of
   * {@code PropertySources}.
   *
   * @see TestPropertySource#properties
   * @see java.util.Properties
   */
  public String[] getPropertySourceProperties() {
    return this.propertySourceProperties;
  }

  /**
   * Get the merged {@link ContextCustomizer ContextCustomizers} that will be applied
   * when the application context is loaded.
   */
  public Set<ContextCustomizer> getContextCustomizers() {
    return this.contextCustomizers;
  }

  /**
   * Get the resolved {@link ContextLoader} for the {@linkplain #getTestClass() test class}.
   */
  @Nullable
  public ContextLoader getContextLoader() {
    return this.contextLoader;
  }

  /**
   * Get the {@link MergedContextConfiguration} for the parent application context
   * in a context hierarchy.
   *
   * @return the parent configuration or {@code null} if there is no parent
   * @see #getParentApplicationContext()
   */
  @Nullable
  public MergedContextConfiguration getParent() {
    return this.parent;
  }

  /**
   * Get the parent {@link ApplicationContext} for the context defined by this
   * {@code MergedContextConfiguration} from the context cache.
   * <p>If the parent context has not yet been loaded, it will be loaded, stored
   * in the cache, and then returned.
   *
   * @return the parent {@code ApplicationContext} or {@code null} if there is no parent
   * @see #getParent()
   */
  @Nullable
  public ApplicationContext getParentApplicationContext() {
    if (this.parent == null) {
      return null;
    }
    Assert.state(this.cacheAwareContextLoaderDelegate != null,
        "Cannot retrieve a parent application context without access to the CacheAwareContextLoaderDelegate");
    return this.cacheAwareContextLoaderDelegate.loadContext(this.parent);
  }

  /**
   * Determine if the supplied object is equal to this {@code MergedContextConfiguration}
   * instance by comparing both objects' {@linkplain #getLocations() locations},
   * {@linkplain #getClasses() annotated classes},
   * {@linkplain #getContextInitializerClasses() context initializer classes},
   * {@linkplain #getActiveProfiles() active profiles},
   * {@linkplain #getPropertySourceDescriptors() property source descriptors},
   * {@linkplain #getPropertySourceProperties() property source properties},
   * {@linkplain #getContextCustomizers() context customizers},
   * {@linkplain #getParent() parents}, and the fully qualified names of their
   * {@link #getContextLoader() ContextLoaders}.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || other.getClass() != getClass()) {
      return false;
    }

    MergedContextConfiguration otherConfig = (MergedContextConfiguration) other;
    if (!Arrays.equals(this.locations, otherConfig.locations)) {
      return false;
    }
    if (!Arrays.equals(this.classes, otherConfig.classes)) {
      return false;
    }
    if (!this.contextInitializerClasses.equals(otherConfig.contextInitializerClasses)) {
      return false;
    }
    if (!Arrays.equals(this.activeProfiles, otherConfig.activeProfiles)) {
      return false;
    }
    if (!this.propertySourceDescriptors.equals(otherConfig.propertySourceDescriptors)) {
      return false;
    }
    if (!Arrays.equals(this.propertySourceProperties, otherConfig.propertySourceProperties)) {
      return false;
    }
    if (!this.contextCustomizers.equals(otherConfig.contextCustomizers)) {
      return false;
    }

    if (this.parent == null) {
      if (otherConfig.parent != null) {
        return false;
      }
    }
    else if (!this.parent.equals(otherConfig.parent)) {
      return false;
    }

    return nullSafeClassName(this.contextLoader)
        .equals(nullSafeClassName(otherConfig.contextLoader));
  }

  /**
   * Generate a unique hash code for all properties of this
   * {@code MergedContextConfiguration} excluding the
   * {@linkplain #getTestClass() test class}.
   */
  @Override
  public int hashCode() {
    int result = Arrays.hashCode(this.locations);
    result = 31 * result + Arrays.hashCode(this.classes);
    result = 31 * result + this.contextInitializerClasses.hashCode();
    result = 31 * result + Arrays.hashCode(this.activeProfiles);
    result = 31 * result + this.propertySourceDescriptors.hashCode();
    result = 31 * result + Arrays.hashCode(this.propertySourceProperties);
    result = 31 * result + this.contextCustomizers.hashCode();
    result = 31 * result + (this.parent != null ? this.parent.hashCode() : 0);
    result = 31 * result + nullSafeClassName(this.contextLoader).hashCode();
    return result;
  }

  /**
   * Provide a String representation of the {@linkplain #getTestClass() test class},
   * {@linkplain #getLocations() locations}, {@linkplain #getClasses() annotated classes},
   * {@linkplain #getContextInitializerClasses() context initializer classes},
   * {@linkplain #getActiveProfiles() active profiles},
   * {@linkplain #getPropertySourceDescriptors() property source descriptors},
   * {@linkplain #getPropertySourceProperties() property source properties},
   * {@linkplain #getContextCustomizers() context customizers},
   * the name of the {@link #getContextLoader() ContextLoader}, and the
   * {@linkplain #getParent() parent configuration}.
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("testClass", this.testClass)
        .append("locations", this.locations)
        .append("classes", this.classes)
        .append("contextInitializerClasses", this.contextInitializerClasses)
        .append("activeProfiles", this.activeProfiles)
        .append("propertySourceDescriptors", this.propertySourceDescriptors)
        .append("propertySourceProperties", this.propertySourceProperties)
        .append("contextCustomizers", this.contextCustomizers)
        .append("contextLoader", (this.contextLoader != null ? this.contextLoader.getClass() : null))
        .append("parent", this.parent)
        .toString();
  }

  protected static String[] processStrings(@Nullable String[] array) {
    return (array != null ? array : EMPTY_STRING_ARRAY);
  }

  private static Class<?>[] processClasses(@Nullable Class<?>[] classes) {
    return (classes != null ? classes : EMPTY_CLASS_ARRAY);
  }

  private static Set<Class<? extends ApplicationContextInitializer>> processContextInitializerClasses(
      @Nullable Set<Class<? extends ApplicationContextInitializer>> contextInitializerClasses) {

    return (contextInitializerClasses != null ?
            Collections.unmodifiableSet(contextInitializerClasses) : EMPTY_INITIALIZER_CLASSES);
  }

  private static Set<ContextCustomizer> processContextCustomizers(
      @Nullable Set<ContextCustomizer> contextCustomizers) {

    return (contextCustomizers != null ?
            Collections.unmodifiableSet(contextCustomizers) : EMPTY_CONTEXT_CUSTOMIZERS);
  }

  private static String[] processActiveProfiles(@Nullable String[] activeProfiles) {
    if (activeProfiles == null) {
      return EMPTY_STRING_ARRAY;
    }

    // Active profiles must be unique
    Set<String> profilesSet = new LinkedHashSet<>(Arrays.asList(activeProfiles));
    return StringUtils.toStringArray(profilesSet);
  }

  /**
   * Generate a null-safe {@link String} representation of the supplied
   * {@link ContextLoader} based solely on the fully qualified name of the
   * loader or &quot;null&quot; if the supplied loader is {@code null}.
   */
  protected static String nullSafeClassName(@Nullable ContextLoader contextLoader) {
    return (contextLoader != null ? contextLoader.getClass().getName() : "null");
  }

}
