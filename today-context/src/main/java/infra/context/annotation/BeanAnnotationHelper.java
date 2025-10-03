/*
 * Copyright 2017 - 2025 the original author or authors.
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

import java.lang.reflect.Method;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.annotation.AnnotationAttributes;
import infra.core.type.MethodMetadata;
import infra.stereotype.Component;
import infra.util.ConcurrentReferenceHashMap;

/**
 * Utilities for processing {@link Component}-annotated methods.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class BeanAnnotationHelper {

  private static final ConcurrentReferenceHashMap<Method, String> beanNameCache = new ConcurrentReferenceHashMap<>();

  private static final ConcurrentReferenceHashMap<Method, Boolean> scopedProxyCache = new ConcurrentReferenceHashMap<>();

  public static boolean isBeanAnnotated(Method method) {
    return AnnotatedElementUtils.hasAnnotation(method, Component.class);
  }

  public static String determineBeanNameFor(Method beanMethod, ConfigurableBeanFactory beanFactory) {
    String beanName = retrieveBeanNameFor(beanMethod);
    if (beanFactory.getSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR) instanceof ConfigurationBeanNameGenerator cbng) {
      return cbng.deriveBeanName(MethodMetadata.introspect(beanMethod), !beanName.isEmpty() ? beanName : null);
    }
    return determineBeanNameFrom(beanName, beanMethod);
  }

  public static String determineBeanNameFor(Method beanMethod) {
    return determineBeanNameFrom(retrieveBeanNameFor(beanMethod), beanMethod);
  }

  private static String retrieveBeanNameFor(Method beanMethod) {
    String beanName = beanNameCache.get(beanMethod);
    if (beanName == null) {
      // By default, the bean name is empty (indicating a name to be derived from the method name)
      beanName = "";
      // Check to see if the user has explicitly set a custom bean name...
      AnnotationAttributes bean = AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod,
              Component.class, false, false);
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

  private static String determineBeanNameFrom(String derivedBeanName, Method beanMethod) {
    return (!derivedBeanName.isEmpty() ? derivedBeanName : beanMethod.getName());
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

  static void clearCaches() {
    scopedProxyCache.clear();
    beanNameCache.clear();
  }

}
