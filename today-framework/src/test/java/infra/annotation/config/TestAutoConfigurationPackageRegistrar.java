/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.annotation.config;

import infra.context.BootstrapContext;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.context.annotation.config.AutoConfigurationPackages;
import infra.core.annotation.AnnotationAttributes;
import infra.core.type.AnnotationMetadata;
import infra.util.ClassUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} to store the base package for tests.
 *
 * @author Phillip Webb
 */
public class TestAutoConfigurationPackageRegistrar implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
    AnnotationAttributes attributes = AnnotationAttributes.fromMap(
            importMetadata.getAnnotationAttributes(TestAutoConfigurationPackage.class.getName(), true));
    AutoConfigurationPackages.register(
            context.getRegistry(), ClassUtils.getPackageName(attributes.getString("value")));
  }

}
