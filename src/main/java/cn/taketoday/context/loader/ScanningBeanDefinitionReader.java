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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

/**
 * @author TODAY 2021/10/2 23:38
 * @see BeanDefinitionLoadingStrategies
 * @since 4.0
 */
public class ScanningBeanDefinitionReader {
  private static final Logger log = LoggerFactory.getLogger(ScanningBeanDefinitionReader.class);
  public static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

  private final BeanDefinitionRegistry registry;

  private String resourcePattern = DEFAULT_RESOURCE_PATTERN;

  private final BeanDefinitionLoadingStrategies scanningStrategies = new BeanDefinitionLoadingStrategies();
  private final DefinitionLoadingContext loadingContext;

  private final ClassPathScanningComponentProvider componentProvider;

  public ScanningBeanDefinitionReader(DefinitionLoadingContext loadingContext) {
    this.registry = loadingContext.getRegistry();
    this.loadingContext = loadingContext;
    this.componentProvider = new ClassPathScanningComponentProvider();
    componentProvider.setMetadataReaderFactory(loadingContext.getMetadataReaderFactory());
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
      componentProvider.scan(patternLocation, (metadataReader, metadataReaderFactory) -> {
        scanningStrategies.loadBeanDefinitions(metadataReader, loadingContext);
      });
    }
    catch (IOException e) {
      throw new BeanDefinitionStoreException("I/O failure during classpath scanning", e);
    }
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

  @SafeVarargs
  public final void addLoadingStrategies(
          Class<? extends BeanDefinitionLoadingStrategy>... loadingStrategies) {
    for (Class<? extends BeanDefinitionLoadingStrategy> loadingStrategy : loadingStrategies) {
      BeanDefinitionLoadingStrategy strategy = loadingContext.instantiate(loadingStrategy);
      scanningStrategies.addStrategies(strategy);
    }
  }

  public void addLoadingStrategies(
          Collection<Class<? extends BeanDefinitionLoadingStrategy>> loadingStrategies) {
    for (Class<? extends BeanDefinitionLoadingStrategy> loadingStrategy : loadingStrategies) {
      BeanDefinitionLoadingStrategy strategy = loadingContext.instantiate(loadingStrategy);
      scanningStrategies.addStrategies(strategy);
    }
  }
}
