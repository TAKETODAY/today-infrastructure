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

import java.io.FileNotFoundException;
import java.io.IOException;

import cn.taketoday.context.aware.ResourceLoaderAware;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.classreading.CachingMetadataReaderFactory;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

/**
 * A component provider that provides components from a base package. Can
 * use {@link CandidateComponentsIndex the index} if it is available of scans the
 * classpath otherwise.
 *
 * <p>This implementation is based on framework 's {@link MetadataReader MetadataReader}
 * facility, backed by an ASM {@link ClassReader ClassReader}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/18 13:50
 */
public class ClassPathScanningComponentProvider implements ResourceLoaderAware {
  private static final Logger log = LoggerFactory.getLogger(ClassPathScanningComponentProvider.class);

  public static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

  private String resourcePattern = DEFAULT_RESOURCE_PATTERN;

  private String resourcePrefix = PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX;

  @Nullable
  protected PatternResourceLoader resourcePatternResolver;

  @Nullable
  private MetadataReaderFactory metadataReaderFactory;

  public ClassPathScanningComponentProvider() { }

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
   * Set the resource prefix to use when scanning the resources.
   * This value will be appended to each base package name.
   *
   * @see PatternResourceLoader#CLASSPATH_ALL_URL_PREFIX
   */
  public void setResourcePrefix(String resourcePrefix) {
    Assert.notNull(resourcePrefix, "'resourcePrefix' is required");
    this.resourcePrefix = resourcePrefix;
  }

  public String getResourcePrefix() {
    return resourcePrefix;
  }

  /**
   * Set the {@link ResourceLoader} to use for resource locations.
   * This will typically be a {@link PatternResourceLoader} implementation.
   * <p>Default is a {@code PathMatchingResourcePatternResolver}, also capable of
   * resource pattern resolving through the {@code ResourcePatternResolver} interface.
   *
   * @see cn.taketoday.core.io.PatternResourceLoader
   * @see cn.taketoday.core.io.PathMatchingPatternResourceLoader
   */
  @Override
  public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
    this.resourcePatternResolver = PatternResourceLoader.fromResourceLoader(resourceLoader);
    this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
  }

  /**
   * Return the ResourceLoader that this component provider uses.
   */
  public final PatternResourceLoader getResourceLoader() {
    if (this.resourcePatternResolver == null) {
      this.resourcePatternResolver = new PathMatchingPatternResourceLoader();
    }
    return this.resourcePatternResolver;
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
   * Scan the class path for candidate components.
   *
   * @param basePackage the package to check for annotated classes
   * @throws IOException sneaky throw from {@link PatternResourceLoader#getResources(String)}
   */
  public void scan(String basePackage, MetadataReaderConsumer metadataReaderConsumer) throws IOException {
    doScanCandidateComponents(basePackage, metadataReaderConsumer);
  }

  private void doScanCandidateComponents(
          String basePackage, MetadataReaderConsumer metadataReaderConsumer) throws IOException {
    boolean traceEnabled = log.isTraceEnabled();
    MetadataReaderFactory metadataReaderFactory = getMetadataReaderFactory();
    String packageSearchPath = resourcePrefix + resolveBasePackage(basePackage) + '/' + this.resourcePattern;
    for (Resource resource : getResourceLoader().getResources(packageSearchPath)) {
      if (traceEnabled) {
        log.trace("Scanning {}", resource);
      }
      try {
        MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
        metadataReaderConsumer.accept(metadataReader);
      }
      catch (FileNotFoundException ex) {
        if (traceEnabled) {
          log.trace("Ignored non-readable {}: {}", resource, ex.getMessage());
        }
      }
    }
  }

  /**
   * Resolve the specified base package into a pattern specification for
   * the package search path.
   * <p>converts a "."-based package path to a "/"-based resource path.
   *
   * @param basePackage the base package as specified by the user
   * @return the pattern specification to be used for package searching
   */
  protected String resolveBasePackage(String basePackage) {
    return ClassUtils.convertClassNameToResourcePath(basePackage);
  }

  /**
   * Clear the local metadata cache, if any, removing all cached class metadata.
   */
  public void clearCache() {
    if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
      // Clear cache in externally provided MetadataReaderFactory; this is a no-op
      // for a shared cache since it'll be cleared by the ApplicationContext.
      ((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
    }
  }

}
