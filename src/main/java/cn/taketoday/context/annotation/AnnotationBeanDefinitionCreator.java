/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.loader.BeanDefinitionLoadingStrategy;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.lang.Component;

/**
 * @author TODAY 2021/10/10 22:20
 * @since 4.0
 */
public class AnnotationBeanDefinitionCreator implements BeanDefinitionLoadingStrategy {
  private final Set<Class<? extends Annotation>> annotationTypes;

  public AnnotationBeanDefinitionCreator() {
    this.annotationTypes = new HashSet<>();
    annotationTypes.add(Component.class);
  }

  @Override
  public Set<BeanDefinition> loadBeanDefinitions(
          MetadataReader metadata, DefinitionLoadingContext loadingContext) {
    AnnotationMetadata annotationMetadata = metadata.getAnnotationMetadata();

    LinkedHashSet<BeanDefinition> definitions = new LinkedHashSet<>();
    for (Class<? extends Annotation> annotationType : annotationTypes) {
      if (annotationMetadata.isAnnotated(annotationType.getName())) {
        AnnotationAttributes[] annotations = annotationMetadata.getAnnotations().getAttributes(annotationType);
        for (AnnotationAttributes attributes : annotations) {
          BeanDefinitionBuilder builder = loadingContext.createBuilder();
          builder.beanClassName(annotationMetadata.getClassName());
          builder.attributes(attributes);
          builder.build(loadingContext.createBeanName(annotationMetadata.getClassName()), definitions::add);
        }
      }
    }
    return definitions;
  }

}
