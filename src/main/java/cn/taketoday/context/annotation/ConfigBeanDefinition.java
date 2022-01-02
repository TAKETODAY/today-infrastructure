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

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.support.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.NonNull;

/**
 * @author TODAY 2021/11/1 12:55
 * @since 4.0
 */
public class ConfigBeanDefinition extends AnnotatedBeanDefinition {

  public ConfigBeanDefinition(MethodMetadata componentMethod, AnnotationMetadata annotationMetadata) {
    super(annotationMetadata, componentMethod);
    setBeanClass(null);
  }

  @Override
  public boolean isFactoryMethod(Method candidate) {
    return super.isFactoryMethod(candidate)
            && BeanAnnotationHelper.isBeanAnnotated(candidate)
            && BeanAnnotationHelper.determineBeanNameFor(candidate).equals(getBeanName());
  }

  @NonNull
  @Override
  public MethodMetadata getFactoryMethodMetadata() {
    return super.getFactoryMethodMetadata();
  }

  @Override
  public BeanDefinition cloneDefinition() {
    AnnotatedBeanDefinition definition = new ConfigBeanDefinition(getFactoryMethodMetadata(), getMetadata());
    definition.copyFrom(this);
    return definition;
  }
}

