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

package infra.oxm.jaxb;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;
import infra.core.type.classreading.CachingMetadataReaderFactory;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.core.type.filter.AnnotationTypeFilter;
import infra.core.type.filter.TypeFilter;
import infra.lang.Assert;
import infra.oxm.UncategorizedMappingException;
import infra.util.ClassUtils;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlRegistry;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Helper class for {@link Jaxb2Marshaller} that scans given packages for classes marked with JAXB2 annotations.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author David Harrigan
 * @author Biju Kunjummen
 * @see #scanPackages()
 * @since 4.0
 */
class ClassPathJaxb2TypeScanner {

  private static final String RESOURCE_PATTERN = "/**/*.class";

  private static final TypeFilter[] JAXB2_TYPE_FILTERS = new TypeFilter[] {
          new AnnotationTypeFilter(XmlRootElement.class, false),
          new AnnotationTypeFilter(XmlType.class, false),
          new AnnotationTypeFilter(XmlSeeAlso.class, false),
          new AnnotationTypeFilter(XmlEnum.class, false),
          new AnnotationTypeFilter(XmlRegistry.class, false) };

  private final PatternResourceLoader patternResourceLoader;

  private final String[] packagesToScan;

  public ClassPathJaxb2TypeScanner(@Nullable ClassLoader classLoader, String... packagesToScan) {
    Assert.notEmpty(packagesToScan, "'packagesToScan' must not be empty");
    this.patternResourceLoader = new PathMatchingPatternResourceLoader(classLoader);
    this.packagesToScan = packagesToScan;
  }

  /**
   * Scan the packages for classes marked with JAXB2 annotations.
   *
   * @throws UncategorizedMappingException in case of errors
   */
  public Class<?>[] scanPackages() throws UncategorizedMappingException {
    try {
      ArrayList<Class<?>> jaxb2Classes = new ArrayList<>();
      for (String packageToScan : this.packagesToScan) {
        String pattern = PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(packageToScan) + RESOURCE_PATTERN;
        Set<Resource> resources = this.patternResourceLoader.getResources(pattern);
        var metadataReaderFactory = new CachingMetadataReaderFactory(this.patternResourceLoader);
        for (Resource resource : resources) {
          MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
          if (isJaxb2Class(metadataReader, metadataReaderFactory)) {
            String className = metadataReader.getClassMetadata().getClassName();
            Class<?> jaxb2AnnotatedClass =
                    ClassUtils.forName(className, this.patternResourceLoader.getClassLoader());
            jaxb2Classes.add(jaxb2AnnotatedClass);
          }
        }
      }
      return ClassUtils.toClassArray(jaxb2Classes);
    }
    catch (IOException ex) {
      throw new UncategorizedMappingException("Failed to scan classpath for unlisted classes", ex);
    }
    catch (ClassNotFoundException ex) {
      throw new UncategorizedMappingException("Failed to load annotated classes from classpath", ex);
    }
  }

  protected boolean isJaxb2Class(MetadataReader reader, MetadataReaderFactory factory) throws IOException {
    for (TypeFilter filter : JAXB2_TYPE_FILTERS) {
      if (filter.match(reader, factory) && !reader.getClassMetadata().isInterface()) {
        return true;
      }
    }
    return false;
  }

}
