/*
 * Copyright 2012-present the original author or authors.
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
