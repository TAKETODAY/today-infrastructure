/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.loader;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.classreading.CachingMetadataReaderFactory;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Repository;
import cn.taketoday.lang.Service;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.annotation.Controller;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * @author TODAY 2021/10/2 23:38
 * @since 4.0
 */
public class ScanningBeanDefinitionReader {
  private static final Logger log = LoggerFactory.getLogger(ScanningBeanDefinitionReader.class);
  static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

  private final BeanDefinitionRegistry registry;

  private String resourcePattern = DEFAULT_RESOURCE_PATTERN;

  @Nullable
  private PatternResourceLoader resourceLoader;

  private final BeanDefinitionLoadingStrategies scanningStrategies = new BeanDefinitionLoadingStrategies();
  private final DefinitionLoadingContext loadingContext;

  private final ArrayList<TypeFilter> includeFilters = new ArrayList<>();

  private final ArrayList<TypeFilter> excludeFilters = new ArrayList<>();

  @Nullable
  private MetadataReaderFactory metadataReaderFactory;

  public ScanningBeanDefinitionReader(DefinitionLoadingContext loadingContext) {
    this.registry = loadingContext.getRegistry();
    this.loadingContext = loadingContext;
    registerDefaultFilters();
  }

  /**
   * Load {@link BeanDefinition}s from input package locations
   *
   * @param basePackages package locations
   * @throws BeanDefinitionStoreException If BeanDefinition could not be store
   * @since 4.0
   */
  public int scanPackages(String... basePackages) throws BeanDefinitionStoreException {
    // Loading candidates components
    log.info("Scanning candidates components from packages: {}", Arrays.toString(basePackages));

    int beanDefinitionCount = registry.getBeanDefinitionCount();
    for (String location : basePackages) {
      scanFromPackage(location);
    }
    int afterScanCount = registry.getBeanDefinitionCount();
    log.info("There are [{}] candidates components in {}", afterScanCount - beanDefinitionCount, Arrays.toString(basePackages));
    return afterScanCount - beanDefinitionCount;
  }

  public void scanFromPackage(String packageName) {
    String resourceToUse = PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX + resolveBasePackage(packageName) + '/' + this.resourcePattern;
    doScanning(resourceToUse);
  }

  /**
   * Load {@link BeanDefinition}s from input pattern locations
   *
   * @param patternLocations package locations
   * @throws BeanDefinitionStoreException If BeanDefinition could not be store
   * @since 4.0
   */
  public int scan(String... patternLocations) {
    // Loading candidates components
    log.info("Scanning candidates components from resource location: '{}'", Arrays.toString(patternLocations));
    int beanDefinitionCount = registry.getBeanDefinitionCount();

    for (String location : patternLocations) {
      doScanning(location);
    }

    int afterScanCount = registry.getBeanDefinitionCount();
    log.info("There are [{}] candidates components in {}", afterScanCount - beanDefinitionCount, Arrays.toString(patternLocations));
    return afterScanCount - beanDefinitionCount;
  }

  public void doScanning(String patternLocation) {
    if (log.isDebugEnabled()) {
      log.debug("Scanning component candidates from pattern location: [{}]", patternLocation);
    }
    try {
      Set<Resource> resources = getResourceLoader().getResources(patternLocation);
      MetadataReaderFactory metadataReaderFactory = getMetadataReaderFactory();
      for (Resource resource : resources) {
        MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
        if (isCandidateComponent(metadataReader)) {
          process(metadataReader);
        }
      }
    }
    catch (IOException e) {
      throw new BeanDefinitionStoreException("IO exception occur With Msg: [" + e + ']', e);
    }
  }

  /**
   * Determine whether the given class does not match any exclude filter
   * and does match at least one include filter.
   *
   * @param metadataReader the ASM ClassReader for the class
   * @return whether the class qualifies as a candidate component
   */
  protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
    for (TypeFilter tf : this.excludeFilters) {
      if (tf.match(metadataReader, getMetadataReaderFactory())) {
        return false;
      }
    }
    for (TypeFilter tf : this.includeFilters) {
      if (tf.match(metadataReader, getMetadataReaderFactory())) {
        return isConditionMatch(metadataReader);
      }
    }
    return false;
  }

  /**
   * Determine whether the given class is a candidate component based on any
   * {@code @Conditional} annotations.
   *
   * @param metadataReader the ASM ClassReader for the class
   * @return whether the class qualifies as a candidate component
   */
  private boolean isConditionMatch(MetadataReader metadataReader) {
    return loadingContext.passCondition(metadataReader.getAnnotationMetadata());
  }

  protected void process(MetadataReader metadata) {
    Set<BeanDefinition> beanDefinitions = scanningStrategies.loadBeanDefinitions(metadata, loadingContext);
    if (CollectionUtils.isNotEmpty(beanDefinitions)) {
      for (BeanDefinition beanDefinition : beanDefinitions) {
        registry.registerBeanDefinition(beanDefinition);
      }
    }
  }

  /**
   * Set the resource pattern to use when scanning the classpath.
   * This value will be appended to each base package name.
   *
   * @see #DEFAULT_RESOURCE_PATTERN
   */
  public void setResourcePattern(String resourcePattern) {
    Assert.notNull(resourcePattern, "'resourcePattern' must not be null");
    this.resourcePattern = resourcePattern;
  }

  /**
   * Add an include type filter to the <i>end</i> of the inclusion list.
   */
  public void addIncludeFilter(TypeFilter includeFilter) {
    this.includeFilters.add(includeFilter);
  }

  /**
   * Add an exclude type filter to the <i>front</i> of the exclusion list.
   */
  public void addExcludeFilter(TypeFilter excludeFilter) {
    this.excludeFilters.add(0, excludeFilter);
  }

  /**
   * Reset the configured type filters.
   *
   * @param useDefaultFilters whether to re-register the default filters for
   * the {@link Component @Component}, {@link Repository @Repository},
   * {@link Service @Service}, and {@link Controller @Controller}
   * stereotype annotations
   * @see #registerDefaultFilters()
   */
  public void resetFilters(boolean useDefaultFilters) {
    this.includeFilters.clear();
    this.excludeFilters.clear();
    if (useDefaultFilters) {
      registerDefaultFilters();
    }
  }

  /**
   * Register the default filter for {@link Component @Component}.
   * <p>This will implicitly register all annotations that have the
   * {@link Component @Component} meta-annotation including the
   * {@link Repository @Repository}, {@link Service @Service}, and
   * {@link Controller @Controller} stereotype annotations.
   * <p>Also supports Java EE's {@link javax.annotation.ManagedBean} and
   * {@link javax.inject.Named} annotations, if available.
   */
  @SuppressWarnings("unchecked")
  protected void registerDefaultFilters() {
    this.includeFilters.add(new AnnotationTypeFilter(Component.class));
    ClassLoader cl = getClass().getClassLoader();
    try {
      this.includeFilters.add(new AnnotationTypeFilter(
              ((Class<? extends Annotation>) ClassUtils.forName("javax.annotation.ManagedBean", cl)), false));
      log.trace("'javax.annotation.ManagedBean' found and supported for component scanning");
    }
    catch (ClassNotFoundException ex) {
      // JSR-250 1.1 API (as included in Jakarta EE) not available - simply skip.
    }
    try {
      this.includeFilters.add(new AnnotationTypeFilter(
              ((Class<? extends Annotation>) ClassUtils.forName("javax.inject.Named", cl)), false));
      log.trace("'javax.inject.Named' annotation found and supported for component scanning");
    }
    catch (ClassNotFoundException ex) {
      // JSR-330 API not available - simply skip.
    }
  }

  /**
   * Set the {@link ResourceLoader} to use for resource locations.
   * This will typically be a {@link PatternResourceLoader} implementation.
   * <p>Default is a {@code PathMatchingResourcePatternResolver}, also capable of
   * resource pattern resolving through the {@code ResourcePatternResolver} interface.
   *
   * @see PatternResourceLoader
   * @see PathMatchingPatternResourceLoader
   */
  public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = PatternResourceLoader.fromResourceLoader(resourceLoader);
    this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
  }

  public PatternResourceLoader getResourceLoader() {
    return getResourcePatternResolver();
  }

  /**
   * Return the ResourceLoader that this component provider uses.
   */
  private PatternResourceLoader getResourcePatternResolver() {
    if (this.resourceLoader == null) {
      this.resourceLoader = new PathMatchingPatternResourceLoader();
    }
    return this.resourceLoader;
  }

  /**
   * Set the {@link MetadataReaderFactory} to use.
   * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
   * {@linkplain #setResourceLoader resource loader}.
   * <p>Call this setter method <i>after</i> {@link #setResourceLoader} in order
   * for the given MetadataReaderFactory to override the default factory.
   */
  public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
    this.metadataReaderFactory = metadataReaderFactory;
  }

  /**
   * Return the MetadataReaderFactory used by this component provider.
   */
  public final MetadataReaderFactory getMetadataReaderFactory() {
    if (this.metadataReaderFactory == null) {
      this.metadataReaderFactory = new CachingMetadataReaderFactory();
    }
    return this.metadataReaderFactory;
  }

  /**
   * Resolve the specified base package into a pattern specification for
   * the package search path.
   * <p>The default implementation resolves placeholders against system properties,
   * and converts a "."-based package path to a "/"-based resource path.
   *
   * @param basePackage the base package as specified by the user
   * @return the pattern specification to be used for package searching
   */
  protected String resolveBasePackage(String basePackage) {
    // TODO resolveRequiredPlaceholders
    return ClassUtils.convertClassNameToResourcePath(basePackage);
  }

  public void addLoadingStrategies(Set<Class<? extends BeanDefinitionLoadingStrategy>> loadingStrategies) {
    for (Class<? extends BeanDefinitionLoadingStrategy> loadingStrategy : loadingStrategies) {
      BeanDefinitionLoadingStrategy strategy = loadingContext.instantiate(loadingStrategy);
      scanningStrategies.addStrategies(strategy);
    }

  }
}
