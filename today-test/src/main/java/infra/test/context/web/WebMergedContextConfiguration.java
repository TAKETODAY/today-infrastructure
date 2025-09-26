/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.web;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.List;
import java.util.Set;

import infra.context.ApplicationContextInitializer;
import infra.core.io.PropertySourceDescriptor;
import infra.core.style.ToStringBuilder;
import infra.test.context.ActiveProfiles;
import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.SmartContextLoader;
import infra.test.context.TestContext;
import infra.util.StringUtils;

/**
 * {@code WebMergedContextConfiguration} encapsulates the <em>merged</em>
 * context configuration declared on a test class and all of its superclasses
 * via {@link ContextConfiguration @ContextConfiguration},
 * {@link WebAppConfiguration @WebAppConfiguration}, and
 * {@link ActiveProfiles @ActiveProfiles}.
 *
 * <p>{@code WebMergedContextConfiguration} extends the contract of
 * {@link MergedContextConfiguration} by adding support for the {@link
 * #getResourceBasePath() resource base path} configured via {@code @WebAppConfiguration}.
 * This allows the {@link TestContext TestContext}
 * to properly cache the corresponding {@link
 * infra.web.mock.WebApplicationContext WebApplicationContext}
 * that was loaded using properties of this {@code WebMergedContextConfiguration}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebAppConfiguration
 * @see MergedContextConfiguration
 * @see ContextConfiguration
 * @see ActiveProfiles
 * @see ContextConfigurationAttributes
 * @see SmartContextLoader#loadContext(MergedContextConfiguration)
 * @since 4.0
 */
public class WebMergedContextConfiguration extends MergedContextConfiguration {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String resourceBasePath;

  /**
   * Create a new {@code WebMergedContextConfiguration} instance by copying
   * all properties from the supplied {@code MergedContextConfiguration}.
   * <p>If an <em>empty</em> value is supplied for the {@code resourceBasePath}
   * an empty string will be used.
   *
   * @param resourceBasePath the resource path to the root directory of the web application
   */
  public WebMergedContextConfiguration(MergedContextConfiguration mergedConfig, String resourceBasePath) {
    super(mergedConfig);
    this.resourceBasePath = (StringUtils.hasText(resourceBasePath) ? resourceBasePath : "");
  }

  /**
   * Create a new {@code WebMergedContextConfiguration} instance for the
   * supplied parameters.
   * <p>If a {@code null} value is supplied for {@code locations},
   * {@code classes}, {@code activeProfiles}, {@code propertySourceLocations},
   * or {@code propertySourceProperties} an empty array will be stored instead.
   * If a {@code null} value is supplied for the
   * {@code contextInitializerClasses} an empty set will be stored instead.
   * If an <em>empty</em> value is supplied for the {@code resourceBasePath}
   * an empty string will be used. Furthermore, active profiles will be sorted,
   * and duplicate profiles will be removed.
   *
   * @param testClass the test class for which the configuration was merged
   * @param locations the merged resource locations
   * @param classes the merged annotated classes
   * @param contextInitializerClasses the merged context initializer classes
   * @param activeProfiles the merged active bean definition profiles
   * @param propertySourceLocations the merged {@code PropertySource} locations
   * @param propertySourceProperties the merged {@code PropertySource} properties
   * @param resourceBasePath the resource path to the root directory of the web application
   * @param contextLoader the resolved {@code ContextLoader}
   * @param cacheAwareContextLoaderDelegate a cache-aware context loader
   * delegate with which to retrieve the parent context
   * @param parent the parent configuration or {@code null} if there is no parent
   */
  public WebMergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
          @Nullable Set<Class<? extends ApplicationContextInitializer>> contextInitializerClasses,
          @Nullable String[] activeProfiles, @Nullable String[] propertySourceLocations, @Nullable String[] propertySourceProperties,
          String resourceBasePath, ContextLoader contextLoader,
          CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, @Nullable MergedContextConfiguration parent) {

    this(testClass, locations, classes, contextInitializerClasses, activeProfiles, propertySourceLocations,
            propertySourceProperties, null, resourceBasePath, contextLoader, cacheAwareContextLoaderDelegate, parent);
  }

  /**
   * Create a new {@code WebMergedContextConfiguration} instance for the
   * supplied parameters.
   * <p>If a {@code null} value is supplied for {@code locations},
   * {@code classes}, {@code activeProfiles}, {@code propertySourceLocations},
   * or {@code propertySourceProperties} an empty array will be stored instead.
   * If a {@code null} value is supplied for {@code contextInitializerClasses}
   * or {@code contextCustomizers}, an empty set will be stored instead.
   * If an <em>empty</em> value is supplied for the {@code resourceBasePath}
   * an empty string will be used. Furthermore, active profiles will be sorted,
   * and duplicate profiles will be removed.
   *
   * @param testClass the test class for which the configuration was merged
   * @param locations the merged context resource locations
   * @param classes the merged annotated classes
   * @param contextInitializerClasses the merged context initializer classes
   * @param activeProfiles the merged active bean definition profiles
   * @param propertySourceLocations the merged {@code PropertySource} locations
   * @param propertySourceProperties the merged {@code PropertySource} properties
   * @param contextCustomizers the context customizers
   * @param resourceBasePath the resource path to the root directory of the web application
   * @param contextLoader the resolved {@code ContextLoader}
   * @param cacheAwareContextLoaderDelegate a cache-aware context loader
   * delegate with which to retrieve the parent context
   * @param parent the parent configuration or {@code null} if there is no parent
   */
  public WebMergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
          @Nullable Set<Class<? extends ApplicationContextInitializer>> contextInitializerClasses,
          @Nullable String[] activeProfiles, @Nullable String[] propertySourceLocations, @Nullable String[] propertySourceProperties,
          @Nullable Set<ContextCustomizer> contextCustomizers, String resourceBasePath, ContextLoader contextLoader,
          CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, @Nullable MergedContextConfiguration parent) {

    this(testClass, locations, classes, contextInitializerClasses, activeProfiles,
            List.of(new PropertySourceDescriptor(processStrings(propertySourceLocations))),
            propertySourceProperties, contextCustomizers, resourceBasePath, contextLoader,
            cacheAwareContextLoaderDelegate, parent);
  }

  /**
   * Create a new {@code WebMergedContextConfiguration} instance for the supplied
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
   * @param resourceBasePath the resource path to the root directory of the web application
   * @param contextLoader the resolved {@code ContextLoader}
   * @param cacheAwareContextLoaderDelegate a cache-aware context loader
   * delegate with which to retrieve the parent {@code ApplicationContext}
   * @param parent the parent configuration or {@code null} if there is no parent
   */
  public WebMergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
          @Nullable Set<Class<? extends ApplicationContextInitializer>> contextInitializerClasses,
          @Nullable String[] activeProfiles,
          List<PropertySourceDescriptor> propertySourceDescriptors, @Nullable String[] propertySourceProperties,
          @Nullable Set<ContextCustomizer> contextCustomizers, String resourceBasePath, ContextLoader contextLoader,
          CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, @Nullable MergedContextConfiguration parent) {

    super(testClass, locations, classes, contextInitializerClasses, activeProfiles, propertySourceDescriptors,
            propertySourceProperties, contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate, parent);

    this.resourceBasePath = (StringUtils.hasText(resourceBasePath) ? resourceBasePath : "");
  }

  /**
   * Get the resource path to the root directory of the web application for the
   * {@linkplain #getTestClass() test class}, configured via {@code @WebAppConfiguration}.
   *
   * @see WebAppConfiguration
   */
  public String getResourceBasePath() {
    return this.resourceBasePath;
  }

  /**
   * Determine if the supplied object is equal to this {@code WebMergedContextConfiguration}
   * instance by comparing both objects' {@linkplain #getLocations() locations},
   * {@linkplain #getClasses() annotated classes},
   * {@linkplain #getContextInitializerClasses() context initializer classes},
   * {@linkplain #getActiveProfiles() active profiles},
   * {@linkplain #getResourceBasePath() resource base paths},
   * {@linkplain #getPropertySourceDescriptors() property source descriptors},
   * {@linkplain #getPropertySourceProperties() property source properties},
   * {@linkplain #getContextCustomizers() context customizers},
   * {@linkplain #getParent() parents}, and the fully qualified names of their
   * {@link #getContextLoader() ContextLoaders}.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (super.equals(other) &&
            this.resourceBasePath.equals(((WebMergedContextConfiguration) other).resourceBasePath)));
  }

  /**
   * Generate a unique hash code for all properties of this
   * {@code WebMergedContextConfiguration} excluding the
   * {@linkplain #getTestClass() test class}.
   */
  @Override
  public int hashCode() {
    return (31 * super.hashCode() + this.resourceBasePath.hashCode());
  }

  /**
   * Provide a String representation of the {@linkplain #getTestClass() test class},
   * {@linkplain #getLocations() locations}, {@linkplain #getClasses() annotated classes},
   * {@linkplain #getContextInitializerClasses() context initializer classes},
   * {@linkplain #getActiveProfiles() active profiles},
   * {@linkplain #getPropertySourceDescriptors() property source descriptors},
   * {@linkplain #getPropertySourceProperties() property source properties},
   * {@linkplain #getContextCustomizers() context customizers},
   * {@linkplain #getResourceBasePath() resource base path}, the name of the
   * {@link #getContextLoader() ContextLoader}, and the
   * {@linkplain #getParent() parent configuration}.
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("testClass", getTestClass())
            .append("locations", getLocations())
            .append("classes", getClasses())
            .append("contextInitializerClasses", getContextInitializerClasses())
            .append("activeProfiles", getActiveProfiles())
            .append("propertySourceDescriptors", getPropertySourceDescriptors())
            .append("propertySourceProperties", getPropertySourceProperties())
            .append("contextCustomizers", getContextCustomizers())
            .append("resourceBasePath", getResourceBasePath())
            .append("contextLoader", (getContextLoader() != null ? getContextLoader().getClass() : null))
            .append("parent", getParent())
            .toString();
  }

}
