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
package cn.taketoday.context.autowire;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import cn.taketoday.beans.factory.BeanReferencePropertySetter;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Required;
import cn.taketoday.util.ClassUtils;

/**
 * This {@link PropertyValueResolver} supports field that annotated
 * {@link Autowired}, {@link javax.annotation.Resource Resource},
 * {@link javax.inject.Inject Inject} or {@link javax.inject.Named Named}
 *
 * @author TODAY <br>
 * 2018-08-04 15:56
 */
public class AutowiredPropertyResolver implements PropertyValueResolver {

  private static final Class<? extends Annotation> NAMED_CLASS = ClassUtils.load("javax.inject.Named");
  private static final Class<? extends Annotation> INJECT_CLASS = ClassUtils.load("javax.inject.Inject");
  private static final Class<? extends Annotation> RESOURCE_CLASS = ClassUtils.load("javax.annotation.Resource");

  // @since 3.0 Required
  public static boolean isRequired(AnnotatedElement element, @Nullable MergedAnnotation<Autowired> autowired) {
    return (autowired == null || autowired.getBoolean("required"))
            || AnnotatedElementUtils.isAnnotated(element, Required.class);
  }

  @Nullable
  @Override
  public PropertySetter resolveProperty(PropertyResolvingContext context, BeanProperty property) {
    MergedAnnotations annotations = MergedAnnotations.from(property);
    MergedAnnotation<Autowired> autowired = annotations.get(Autowired.class);
    if (autowired.isPresent()) {
      boolean required = isRequired(property, autowired);
      String referenceName = autowired.getString(MergedAnnotation.VALUE);
      return new BeanReferencePropertySetter(referenceName, required, property);
    }
    // @Resource
    MergedAnnotation<? extends Annotation> resource = annotations.get(RESOURCE_CLASS);
    if (resource.isPresent()) {
      String referenceName = resource.getString("name");
      return new BeanReferencePropertySetter(referenceName, isRequired(property, null), property);
    }
    // @Named
    MergedAnnotation<? extends Annotation> named = annotations.get(NAMED_CLASS);
    if (named.isPresent()) {
      // @since 3.0
      String referenceName = named.getString(MergedAnnotation.VALUE);
      return new BeanReferencePropertySetter(referenceName, isRequired(property, null), property);
    }
    return null;
  }

}
