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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.AnnotatedBeanDefinition;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.lang.Assert;

/**
 * Abstract base class for handlers of Servlet components discovered via classpath
 * scanning.
 *
 * @author Andy Wilkinson
 */
abstract class ServletComponentHandler {

  private final Class<? extends Annotation> annotationType;

  private final TypeFilter typeFilter;

  protected ServletComponentHandler(Class<? extends Annotation> annotationType) {
    this.typeFilter = new AnnotationTypeFilter(annotationType);
    this.annotationType = annotationType;
  }

  TypeFilter getTypeFilter() {
    return this.typeFilter;
  }

  protected String[] extractUrlPatterns(Map<String, Object> attributes) {
    String[] value = (String[]) attributes.get("value");
    String[] urlPatterns = (String[]) attributes.get("urlPatterns");
    if (urlPatterns.length > 0) {
      Assert.state(value.length == 0, "The urlPatterns and value attributes are mutually exclusive.");
      return urlPatterns;
    }
    return value;
  }

  protected final Map<String, String> extractInitParameters(Map<String, Object> attributes) {
    Map<String, String> initParameters = new HashMap<>();
    for (AnnotationAttributes initParam : (AnnotationAttributes[]) attributes.get("initParams")) {
      String name = (String) initParam.get("name");
      String value = (String) initParam.get("value");
      initParameters.put(name, value);
    }
    return initParameters;
  }

  void handle(AnnotatedBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
    Map<String, Object> attributes = beanDefinition.getMetadata()
            .getAnnotationAttributes(this.annotationType.getName());
    if (attributes != null) {
      doHandle(attributes, beanDefinition, registry);
    }
  }

  protected abstract void doHandle(
          Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition,
          BeanDefinitionRegistry registry);

}
