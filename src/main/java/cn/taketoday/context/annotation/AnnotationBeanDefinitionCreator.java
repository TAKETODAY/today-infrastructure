/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
import cn.taketoday.context.loader.BeanDefinitionCreationContext;
import cn.taketoday.context.loader.BeanDefinitionCreationStrategy;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.annotation.ClassMetaReader;
import cn.taketoday.core.bytecode.tree.ClassNode;

/**
 * @author TODAY 2021/10/10 22:20
 * @since 4.0
 */
public class AnnotationBeanDefinitionCreator implements BeanDefinitionCreationStrategy {
  private final Set<Class<? extends Annotation>> annotationTypes;

  public AnnotationBeanDefinitionCreator() {
    this.annotationTypes = new HashSet<>();
    annotationTypes.add(Component.class);
  }

  @Override
  public Set<BeanDefinition> create(
          ClassNode classNode, BeanDefinitionCreationContext creationContext) {
    if (Modifier.isAbstract(classNode.access) || Modifier.isInterface(classNode.access)) {
      return null;
    }

    LinkedHashSet<BeanDefinition> definitions = new LinkedHashSet<>();
    AnnotationAttributes[] annotations = ClassMetaReader.readAnnotations(classNode);
    for (Class<? extends Annotation> annotationType : annotationTypes) {
      AnnotationAttributes attributes = ClassMetaReader.selectAttributes(annotations, annotationType);
      if (attributes != null) {
        BeanDefinitionBuilder builder = creationContext.getDefinitionBuilder();
        builder.reset();
        builder.attributes(attributes);
        builder.build(creationContext.createBeanName(classNode.name), definitions::add);
      }
    }
    return definitions;
  }

}
