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

package cn.taketoday.context.annotation;

import java.lang.reflect.Method;

import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ConcurrentReferenceHashMap;

/**
 * Utilities for processing {@link Component}-annotated methods.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class BeanAnnotationHelper {

  private static final ConcurrentReferenceHashMap<Method, String> beanNameCache
          = new ConcurrentReferenceHashMap<>();

  private static final ConcurrentReferenceHashMap<Method, Boolean> scopedProxyCache
          = new ConcurrentReferenceHashMap<>();

  public static boolean isBeanAnnotated(Method method) {
    return AnnotatedElementUtils.hasAnnotation(method, Component.class);
  }

  public static String determineBeanNameFor(Method beanMethod) {
    String beanName = beanNameCache.get(beanMethod);
    if (beanName == null) {
      // By default, the bean name is the name of the @Component-annotated method
      beanName = beanMethod.getName();
      // Check to see if the user has explicitly set a custom bean name...
      AnnotationAttributes bean =
              AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod, Component.class, false, false);
      if (bean != null) {
        String[] names = bean.getStringArray("name");
        if (names.length > 0) {
          beanName = names[0];
        }
      }
      beanNameCache.put(beanMethod, beanName);
    }
    return beanName;
  }

  public static boolean isScopedProxy(Method beanMethod) {
    Boolean scopedProxy = scopedProxyCache.get(beanMethod);
    if (scopedProxy == null) {
      AnnotationAttributes scope =
              AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod, Scope.class, false, false);
      scopedProxy = (scope != null && scope.getEnum("proxyMode") != ScopedProxyMode.NO);
      scopedProxyCache.put(beanMethod, scopedProxy);
    }
    return scopedProxy;
  }

}
