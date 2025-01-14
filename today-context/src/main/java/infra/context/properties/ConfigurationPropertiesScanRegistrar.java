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

package infra.context.properties;

import java.io.IOException;
import java.util.Set;

import infra.context.BootstrapContext;
import infra.context.annotation.ClassPathScanningCandidateComponentProvider;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.context.annotation.config.TypeExcludeFilter;
import infra.core.annotation.MergedAnnotationSelectors;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotationMetadata;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.filter.AnnotationTypeFilter;
import infra.lang.Assert;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

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
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
    Set<String> packagesToScan = getPackagesToScan(importMetadata);
    scan(context, packagesToScan);
  }

  private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
    var attributes = metadata.getAnnotations().get(
            ConfigurationPropertiesScan.class, null, MergedAnnotationSelectors.firstDirectlyDeclared());

    Assert.state(attributes.isPresent(), "ConfigurationPropertiesScan not present");
    String[] basePackages = attributes.getStringArray("basePackages");
    Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
    var packagesToScan = CollectionUtils.newLinkedHashSet(basePackages);
    for (Class<?> basePackageClass : basePackageClasses) {
      packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
    }
    if (packagesToScan.isEmpty()) {
      packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
    }
    packagesToScan.removeIf(StringUtils::isBlank);
    return packagesToScan;
  }

  private void scan(BootstrapContext context, Set<String> packages) {
    var registrar = new ConfigurationPropertiesBeanRegistrar(context);
    var scanner = getScanner(context);
    for (String basePackage : packages) {
      try {
        scanner.scanCandidateComponents(
                basePackage, (metadataReader, factory) -> register(registrar, metadataReader));
      }
      catch (IOException e) {
        throw new IllegalStateException("ConfigurationProperties scanning failed", e);
      }
    }
  }

  private ClassPathScanningCandidateComponentProvider getScanner(BootstrapContext context) {
    var scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
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
    return annotationMetadata.isAnnotated(Component.class);
  }

}
