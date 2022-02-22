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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.auto.TypeExcludeFilter;
import cn.taketoday.context.loader.ClassPathScanningCandidateComponentProvider;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotationSelectors;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} for registering
 * {@link ConfigurationProperties @ConfigurationProperties} bean definitions via scanning.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigurationPropertiesScanRegistrar implements ImportBeanDefinitionRegistrar {

  private final Environment environment;
  private final ResourceLoader resourceLoader;

  ConfigurationPropertiesScanRegistrar(Environment environment, ResourceLoader resourceLoader) {
    this.environment = environment;
    this.resourceLoader = resourceLoader;
  }

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, DefinitionLoadingContext context) {
    Set<String> packagesToScan = getPackagesToScan(importMetadata);
    scan(context, packagesToScan);
  }

  private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
    MergedAnnotation<ConfigurationPropertiesScan> attributes = metadata.getAnnotations().get(
            ConfigurationPropertiesScan.class, null, MergedAnnotationSelectors.firstDirectlyDeclared());

    Assert.state(attributes.isPresent(), "ConfigurationPropertiesScan not present");
    String[] basePackages = attributes.getStringArray("basePackages");
    Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
    LinkedHashSet<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(basePackages));
    for (Class<?> basePackageClass : basePackageClasses) {
      packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
    }
    if (packagesToScan.isEmpty()) {
      packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
    }

    packagesToScan.removeIf(Predicate.not(StringUtils::hasText));
    return packagesToScan;
  }

  private void scan(DefinitionLoadingContext context, Set<String> packages) {
    ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(context);
    ClassPathScanningCandidateComponentProvider scanner = getScanner(context);
    for (String basePackage : packages) {
      try {
        scanner.scanCandidateComponents(basePackage, (metadataReader, factory) -> register(registrar, metadataReader));
      }
      catch (IOException e) {
        throw new IllegalStateException("ConfigurationProperties scanning failed", e);
      }
    }
  }

  private ClassPathScanningCandidateComponentProvider getScanner(DefinitionLoadingContext context) {
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
    scanner.setResourceLoader(resourceLoader);
    scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));
    TypeExcludeFilter typeExcludeFilter = new TypeExcludeFilter();
    typeExcludeFilter.setBeanFactory(context.getBeanFactory());
    scanner.addExcludeFilter(typeExcludeFilter);
    return scanner;
  }

  private void register(ConfigurationPropertiesBeanRegistrar registrar, MetadataReader reader) throws LinkageError {
    AnnotationMetadata annotationMetadata = reader.getAnnotationMetadata();
    if (!isComponent(annotationMetadata)) {
      Class<?> objectClass = ClassUtils.load(annotationMetadata.getClassName(), null);
      if (objectClass != null) {
        registrar.register(objectClass);
      }
    }
  }

  private boolean isComponent(AnnotationMetadata annotationMetadata) {
    // MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY).isPresent(Component.class)
    return annotationMetadata.isAnnotated(Component.class.getName());
  }

}
