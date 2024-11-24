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

package infra.context.annotation;

import java.lang.annotation.Annotation;

import infra.context.BootstrapContext;
import infra.core.annotation.AnnotationProvider;
import infra.core.type.AnnotationMetadata;

/**
 * @author TODAY 2021/3/8 16:48
 * @since 3.0
 */
public interface AnnotationBeanDefinitionRegistrar<A extends Annotation>
        extends AnnotationProvider<A>, ImportBeanDefinitionRegistrar {

  @Override
  default void registerBeanDefinitions(
          AnnotationMetadata importMetadata, BootstrapContext context) {
    final A target = getAnnotation(importMetadata);
    registerBeanDefinitions(target, importMetadata, context);
  }

  void registerBeanDefinitions(
          A target, AnnotationMetadata annotatedMetadata, BootstrapContext context);

}
