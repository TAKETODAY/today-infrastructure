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

import cn.taketoday.beans.factory.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionBuilder;
import cn.taketoday.context.loader.BeanDefinitionLoadingStrategy;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.lang.Component;

/**
 * @author TODAY 2021/10/10 22:20
 * @since 4.0
 */
public class ComponentAnnotationBeanDefinitionCreator implements BeanDefinitionLoadingStrategy {

  @Override
  public void loadBeanDefinitions(
          MetadataReader metadata, DefinitionLoadingContext loadingContext) {
    // annotation on class
    AnnotationMetadata annotationMetadata = metadata.getAnnotationMetadata();
    annotationMetadata.getAnnotations().stream(Component.class).forEach(component -> {
      for (String name : BeanDefinitionBuilder.determineName(
              loadingContext.createBeanName(annotationMetadata.getClassName()), component.getStringArray(MergedAnnotation.VALUE))) {
        AnnotatedBeanDefinition definition = new AnnotatedBeanDefinition(annotationMetadata);
        definition.setBeanClassName(annotationMetadata.getClassName());
        definition.setName(name);
        loadingContext.registerBeanDefinition(definition);
      }
    });
  }

}
