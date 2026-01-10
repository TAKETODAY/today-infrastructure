/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
