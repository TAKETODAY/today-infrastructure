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

package cn.taketoday.orm.jpa.persistenceunit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.loader.CandidateComponentsIndex;
import cn.taketoday.context.loader.CandidateComponentsIndexLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.classreading.CachingMetadataReaderFactory;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ResourceUtils;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;

/**
 * Scanner of {@link PersistenceManagedTypes}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 10:24
 */
public final class PersistenceManagedTypesScanner {

  private static final String CLASS_RESOURCE_PATTERN = "/**/*.class";

  private static final String PACKAGE_INFO_SUFFIX = ".package-info";

  private final List<AnnotationTypeFilter> entityTypeFilters = List.of(
          new AnnotationTypeFilter(Entity.class, false),
          new AnnotationTypeFilter(Embeddable.class, false),
          new AnnotationTypeFilter(MappedSuperclass.class, false),
          new AnnotationTypeFilter(Converter.class, false)
  );

  private final PatternResourceLoader patternResourceLoader;

  @Nullable
  private final CandidateComponentsIndex componentsIndex;

  public PersistenceManagedTypesScanner(ResourceLoader resourceLoader) {
    this.patternResourceLoader = PatternResourceLoader.fromResourceLoader(resourceLoader);
    this.componentsIndex = CandidateComponentsIndexLoader.loadIndex(resourceLoader.getClassLoader());
  }

  /**
   * Scan the specified packages and return a {@link PersistenceManagedTypes} that
   * represents the result of the scanning.
   *
   * @param packagesToScan the packages to scan
   * @return the {@link PersistenceManagedTypes} instance
   */
  public PersistenceManagedTypes scan(String... packagesToScan) {
    ScanResult scanResult = new ScanResult();
    for (String pkg : packagesToScan) {
      scanPackage(pkg, scanResult);
    }
    return scanResult.toJpaManagedTypes();
  }

  private void scanPackage(String pkg, ScanResult scanResult) {
    if (componentsIndex != null) {
      var candidates = new HashSet<String>();
      for (AnnotationTypeFilter filter : entityTypeFilters) {
        candidates.addAll(componentsIndex.getCandidateTypes(pkg, filter.getAnnotationType().getName()));
      }
      scanResult.managedClassNames.addAll(candidates);
      scanResult.managedPackages.addAll(componentsIndex.getCandidateTypes(pkg, "package-info"));
      return;
    }

    try {
      String pattern = PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX +
              ClassUtils.convertClassNameToResourcePath(pkg) + CLASS_RESOURCE_PATTERN;
      Set<Resource> resources = patternResourceLoader.getResources(pattern);
      var readerFactory = new CachingMetadataReaderFactory(patternResourceLoader);
      for (Resource resource : resources) {
        try {
          MetadataReader reader = readerFactory.getMetadataReader(resource);
          String className = reader.getClassMetadata().getClassName();
          if (matchesFilter(reader, readerFactory)) {
            scanResult.managedClassNames.add(className);
            if (scanResult.persistenceUnitRootUrl == null) {
              URL url = resource.getURL();
              if (ResourceUtils.isJarURL(url)) {
                scanResult.persistenceUnitRootUrl = ResourceUtils.extractJarFileURL(url);
              }
            }
          }
          else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
            scanResult.managedPackages.add(className.substring(0,
                    className.length() - PACKAGE_INFO_SUFFIX.length()));
          }
        }
        catch (FileNotFoundException ex) {
          // Ignore non-readable resource
        }
      }
    }
    catch (IOException ex) {
      throw new PersistenceException("Failed to scan classpath for unlisted entity classes", ex);
    }
  }

  /**
   * Check whether any of the configured entity type filters matches
   * the current class descriptor contained in the metadata reader.
   */
  private boolean matchesFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
    for (TypeFilter filter : entityTypeFilters) {
      if (filter.match(reader, readerFactory)) {
        return true;
      }
    }
    return false;
  }

  private static class ScanResult {

    public final ArrayList<String> managedClassNames = new ArrayList<>();

    public final ArrayList<String> managedPackages = new ArrayList<>();

    @Nullable
    public URL persistenceUnitRootUrl;

    PersistenceManagedTypes toJpaManagedTypes() {
      return new SimplePersistenceManagedTypes(this.managedClassNames,
              this.managedPackages, this.persistenceUnitRootUrl);
    }

  }
}
