/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.diagnostics.analyzer;

import java.lang.reflect.Constructor;

import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.ConfigurationPropertiesBean;
import cn.taketoday.context.properties.ConfigurationPropertiesScan;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.context.properties.bind.BindMethod;
import cn.taketoday.context.properties.bind.ConstructorBinding;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.lang.Nullable;

/**
 * An {@link AbstractInjectionFailureAnalyzer} for
 * {@link ConfigurationProperties @ConfigurationProperties} that are intended to use
 * {@link ConstructorBinding constructor binding} but did not.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/21 10:41
 */
class NotConstructorBoundInjectionFailureAnalyzer
        extends AbstractInjectionFailureAnalyzer<NoSuchBeanDefinitionException> implements Ordered {

  @Override
  public int getOrder() {
    return 0;
  }

  @Nullable
  @Override
  protected FailureAnalysis analyze(Throwable rootFailure,
          NoSuchBeanDefinitionException cause, @Nullable String description) {
    InjectionPoint injectionPoint = findInjectionPoint(rootFailure);
    if (isConstructorBindingConfigurationProperties(injectionPoint)) {
      String simpleName = injectionPoint.getMember().getDeclaringClass().getSimpleName();
      String action = String.format("Update your configuration so that %s is defined via @%s or @%s.",
              simpleName,
              ConfigurationPropertiesScan.class.getSimpleName(),
              EnableConfigurationProperties.class.getSimpleName());
      return new FailureAnalysis(simpleName +
              " is annotated with @" + ConstructorBinding.class.getSimpleName()
              + " but it is defined as a regular bean which caused dependency injection to fail.", action, cause);
    }
    return null;
  }

  private boolean isConstructorBindingConfigurationProperties(@Nullable InjectionPoint injectionPoint) {
    return injectionPoint != null && injectionPoint.getMember() instanceof Constructor<?> constructor
            && isConstructorBindingConfigurationProperties(constructor);
  }

  private boolean isConstructorBindingConfigurationProperties(Constructor<?> constructor) {
    Class<?> declaringClass = constructor.getDeclaringClass();
    BindMethod bindMethod = ConfigurationPropertiesBean.deduceBindMethod(declaringClass);
    return MergedAnnotations.from(declaringClass, SearchStrategy.TYPE_HIERARCHY)
            .isPresent(ConfigurationProperties.class) && bindMethod == BindMethod.VALUE_OBJECT;
  }

  @Nullable
  private InjectionPoint findInjectionPoint(Throwable failure) {
    UnsatisfiedDependencyException unsatisfiedDependencyException = findCause(failure,
            UnsatisfiedDependencyException.class);
    if (unsatisfiedDependencyException == null) {
      return null;
    }
    return unsatisfiedDependencyException.getInjectionPoint();
  }

}
