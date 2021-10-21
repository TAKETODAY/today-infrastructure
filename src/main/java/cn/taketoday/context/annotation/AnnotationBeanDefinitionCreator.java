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
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.loader.BeanDefinitionLoadingStrategy;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.bytecode.tree.ClassNode;
import cn.taketoday.lang.Component;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

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
          ClassNode classNode, DefinitionLoadingContext loadingContext) {
    if (Modifier.isAbstract(classNode.access) || Modifier.isInterface(classNode.access)) {
      return null;
    }
    // TODO lazy class loading

    String className = ClassUtils.convertResourcePathToClassName(classNode.name);
    Class<?> aClass = ClassUtils.resolveClassName(className, null);

    LinkedHashSet<BeanDefinition> definitions = new LinkedHashSet<>();
    for (Class<? extends Annotation> annotationType : annotationTypes) {
      AnnotationAttributes[] annotations = AnnotatedElementUtils.getMergedAttributesArray(aClass, annotationType);

      if (ObjectUtils.isNotEmpty(annotations)) {
        for (AnnotationAttributes attributes : annotations) {
          BeanDefinitionBuilder builder = loadingContext.createBuilder();
          builder.beanClass(aClass);
          builder.attributes(attributes);
          builder.build(loadingContext.createBeanName(className), definitions::add);
        }
      }
    }
    return definitions;
  }

}
