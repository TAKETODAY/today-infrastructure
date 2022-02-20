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

package cn.taketoday.framework.web.servlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.util.ClassUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} used by
 * {@link ServletComponentScan @ServletComponentScan}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class ServletComponentScanRegistrar implements ImportBeanDefinitionRegistrar {

  private static final String BEAN_NAME = "servletComponentRegisteringPostProcessor";

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, DefinitionLoadingContext context) {
    BeanDefinitionRegistry registry = context.getRegistry();

    Set<String> packagesToScan = getPackagesToScan(importMetadata);
    if (registry.containsBeanDefinition(BEAN_NAME)) {
      updatePostProcessor(registry, packagesToScan);
    }
    else {
      addPostProcessor(registry, packagesToScan);
    }
  }

  private void updatePostProcessor(BeanDefinitionRegistry registry, Set<String> packagesToScan) {
    ServletComponentRegisteringPostProcessorBeanDefinition definition = (ServletComponentRegisteringPostProcessorBeanDefinition) registry
            .getBeanDefinition(BEAN_NAME);
    definition.addPackageNames(packagesToScan);
  }

  private void addPostProcessor(BeanDefinitionRegistry registry, Set<String> packagesToScan) {
    ServletComponentRegisteringPostProcessorBeanDefinition definition = new ServletComponentRegisteringPostProcessorBeanDefinition(
            packagesToScan);
    registry.registerBeanDefinition(BEAN_NAME, definition);
  }

  private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
    AnnotationAttributes attributes = AnnotationAttributes
            .fromMap(metadata.getAnnotationAttributes(ServletComponentScan.class.getName()));
    String[] basePackages = attributes.getStringArray("basePackages");
    Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
    Set<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(basePackages));
    for (Class<?> basePackageClass : basePackageClasses) {
      packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
    }
    if (packagesToScan.isEmpty()) {
      packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
    }
    return packagesToScan;
  }

  static final class ServletComponentRegisteringPostProcessorBeanDefinition extends BeanDefinition {

    private final Set<String> packageNames = new LinkedHashSet<>();

    ServletComponentRegisteringPostProcessorBeanDefinition(Collection<String> packageNames) {
      setBeanClass(ServletComponentRegisteringPostProcessor.class);
      setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      addPackageNames(packageNames);
    }

    @Override
    public Supplier<?> getInstanceSupplier() {
      return () -> new ServletComponentRegisteringPostProcessor(this.packageNames);
    }

    private void addPackageNames(Collection<String> additionalPackageNames) {
      this.packageNames.addAll(additionalPackageNames);
    }

  }

}
