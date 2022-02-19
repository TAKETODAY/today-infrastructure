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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.boot.context.TypeExcludeFilter;
import cn.taketoday.context.annotation.ClassPathScanningCandidateComponentProvider;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} for registering
 * {@link ConfigurationProperties @ConfigurationProperties} bean definitions via scanning.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigurationPropertiesScanRegistrar implements ImportBeanDefinitionRegistrar {

  private final Environment environment;

  private final ResourceLoader resourceLoader;

  ConfigurationPropertiesScanRegistrar(Environment environment, ResourceLoader resourceLoader) {
    this.environment = environment;
    this.resourceLoader = resourceLoader;
  }

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);
    scan(registry, packagesToScan);
  }

  private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
    AnnotationAttributes attributes = AnnotationAttributes
            .fromMap(metadata.getAnnotationAttributes(ConfigurationPropertiesScan.class.getName()));
    String[] basePackages = attributes.getStringArray("basePackages");
    Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
    Set<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(basePackages));
    for (Class<?> basePackageClass : basePackageClasses) {
      packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
    }
    if (packagesToScan.isEmpty()) {
      packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
    }
    packagesToScan.removeIf((candidate) -> !StringUtils.hasText(candidate));
    return packagesToScan;
  }

  private void scan(BeanDefinitionRegistry registry, Set<String> packages) {
    ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(registry);
    ClassPathScanningCandidateComponentProvider scanner = getScanner(registry);
    for (String basePackage : packages) {
      for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
        register(registrar, candidate.getBeanClassName());
      }
    }
  }

  private ClassPathScanningCandidateComponentProvider getScanner(BeanDefinitionRegistry registry) {
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.setEnvironment(this.environment);
    scanner.setResourceLoader(this.resourceLoader);
    scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));
    TypeExcludeFilter typeExcludeFilter = new TypeExcludeFilter();
    typeExcludeFilter.setBeanFactory((BeanFactory) registry);
    scanner.addExcludeFilter(typeExcludeFilter);
    return scanner;
  }

  private void register(ConfigurationPropertiesBeanRegistrar registrar, String className) throws LinkageError {
    try {
      register(registrar, ClassUtils.forName(className, null));
    }
    catch (ClassNotFoundException ex) {
      // Ignore
    }
  }

  private void register(ConfigurationPropertiesBeanRegistrar registrar, Class<?> type) {
    if (!isComponent(type)) {
      registrar.register(type);
    }
  }

  private boolean isComponent(Class<?> type) {
    return MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY).isPresent(Component.class);
  }

}
