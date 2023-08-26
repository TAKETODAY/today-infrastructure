/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.context.support;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextCustomizerFactory;
import cn.taketoday.test.context.DynamicPropertySource;
import cn.taketoday.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} to support
 * {@link DynamicPropertySource @DynamicPropertySource} methods.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see DynamicPropertiesContextCustomizer
 * @since 4.0
 */
class DynamicPropertiesContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  @Nullable
  public DynamicPropertiesContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {

    Set<Method> methods = new LinkedHashSet<>();
    findMethods(testClass, methods);
    if (methods.isEmpty()) {
      return null;
    }
    return new DynamicPropertiesContextCustomizer(methods);
  }

  private void findMethods(Class<?> testClass, Set<Method> methods) {
    // Beginning with Java 16, inner classes may contain static members.
    // We therefore need to search for @DynamicPropertySource methods in the
    // current class after searching enclosing classes so that a local
    // @DynamicPropertySource method can override properties registered in
    // an enclosing class.
    if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
      findMethods(testClass.getEnclosingClass(), methods);
    }
    methods.addAll(MethodIntrospector.filterMethods(testClass, this::isAnnotated));
  }

  private boolean isAnnotated(Method method) {
    return MergedAnnotations.from(method).isPresent(DynamicPropertySource.class);
  }

}
