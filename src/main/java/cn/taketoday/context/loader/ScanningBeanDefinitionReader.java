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
import cn.taketoday.core.annotation.ClassMetaReader;
import cn.taketoday.core.bytecode.tree.ClassNode;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
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
  /** @since 2.1.7 Scan candidates */
  private final ArrayList<AnnotatedElement> componentScanned = new ArrayList<>();

  private String resourcePattern = DEFAULT_RESOURCE_PATTERN;


  private PatternResourceLoader resourceLoader = new PathMatchingPatternResourceLoader();
  private final BeanDefinitionLoadingStrategies scanningStrategies = new BeanDefinitionLoadingStrategies();
  private final DefinitionLoadingContext loadingContext;

  public ScanningBeanDefinitionReader(DefinitionLoadingContext loadingContext) {
    this.registry = loadingContext.getRegistry();
    this.loadingContext = loadingContext;
  }

  public void setResourceLoader(PatternResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public PatternResourceLoader getResourceLoader() {
    return resourceLoader;
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
    log.info("Scanning candidates components from: '{}'", Arrays.toString(basePackages));

    int beanDefinitionCount = registry.getBeanDefinitionCount();
    for (String location : basePackages) {
      scanFromPackage(location);
    }
    int afterScanCount = registry.getBeanDefinitionCount();
    log.info("There are [{}] candidates components in {}", afterScanCount - beanDefinitionCount, Arrays.toString(basePackages));
    return afterScanCount - beanDefinitionCount;
  }

  public void scanFromPackage(String packageName) {
    String resourceToUse = PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX +
            resolveBasePackage(packageName) + '/' + this.resourcePattern;
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
    log.info("Scanning candidates components from: '{}'", Arrays.toString(patternLocations));
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
      Set<Resource> resources = resourceLoader.getResources(patternLocation);
      for (Resource resource : resources) {
        ClassNode classNode = ClassMetaReader.read(resource);
        process(classNode);
      }
    }
    catch (IOException e) {
      throw new BeanDefinitionStoreException("IO exception occur With Msg: [" + e + ']', e);
    }
  }

  protected void process(ClassNode classNode) {
    Set<BeanDefinition> beanDefinitions = scanningStrategies.loadBeanDefinitions(classNode, loadingContext);
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


}
