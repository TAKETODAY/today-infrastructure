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

package cn.taketoday.context.annotation;

import java.io.FileNotFoundException;
import java.io.IOException;

import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.context.ResourceLoaderAware;
import cn.taketoday.context.index.CandidateComponentsIndex;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
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
    Assert.notNull(resourcePattern, "'resourcePattern' is required");
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
    PatternResourceLoader resourceLoader = this.resourcePatternResolver;
    if (resourceLoader == null) {
      resourceLoader = new PathMatchingPatternResourceLoader();
      this.resourcePatternResolver = resourceLoader;
    }
    return resourceLoader;
  }

  /**
   * Set the {@link MetadataReaderFactory} to use.
   * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
   * {@linkplain #setResourceLoader resource loader}.
   * <p>Call this setter method <i>after</i> {@link #setResourceLoader} in order
   * for the given MetadataReaderFactory to override the default factory.
   */
  public void setMetadataReaderFactory(@Nullable MetadataReaderFactory metadataReaderFactory) {
    this.metadataReaderFactory = metadataReaderFactory;
  }

  /**
   * Return the MetadataReaderFactory used by this component provider.
   */
  public final MetadataReaderFactory getMetadataReaderFactory() {
    MetadataReaderFactory metadataReaderFactory = this.metadataReaderFactory;
    if (metadataReaderFactory == null) {
      metadataReaderFactory = new CachingMetadataReaderFactory(getResourceLoader());
      this.metadataReaderFactory = metadataReaderFactory;
    }
    return metadataReaderFactory;
  }

  /**
   * Scan the class path for candidate components.
   *
   * @param basePackage the package to check for annotated classes
   * @throws IOException sneaky throw from {@link PatternResourceLoader#getResources(String)}
   */
  public void scan(String basePackage, MetadataReaderConsumer metadataReaderConsumer) throws IOException {
    boolean traceEnabled = log.isTraceEnabled();
    String packageSearchPath = getPatternLocation(basePackage);
    getResourceLoader().scan(packageSearchPath, resource -> {
      String filename = resource.getName();
      if (filename != null && filename.contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
        // Ignore CGLIB-generated classes in the classpath
        return;
      }
      if (traceEnabled) {
        log.trace("Scanning {}", resource);
      }
      try {
        MetadataReaderFactory factory = getMetadataReaderFactory();
        MetadataReader metadataReader = factory.getMetadataReader(resource);
        metadataReaderConsumer.accept(metadataReader, factory);
      }
      catch (FileNotFoundException ex) {
        if (traceEnabled) {
          log.trace("Ignored non-readable {}: {}", resource, ex.getMessage());
        }
      }
    });
  }

  protected String getPatternLocation(String input) {
    return resourcePrefix + resolveBasePackage(input) + '/' + this.resourcePattern;
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
    if (metadataReaderFactory instanceof CachingMetadataReaderFactory caching) {
      // Clear cache in externally provided MetadataReaderFactory; this is a no-op
      // for a shared cache since it'll be cleared by the ApplicationContext.
      caching.clearCache();
    }
  }

}
