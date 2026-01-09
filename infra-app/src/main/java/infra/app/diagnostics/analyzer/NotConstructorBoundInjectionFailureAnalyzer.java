/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.diagnostics.analyzer;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;

import infra.app.diagnostics.FailureAnalysis;
import infra.beans.factory.InjectionPoint;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.UnsatisfiedDependencyException;
import infra.context.properties.ConfigurationProperties;
import infra.context.properties.ConfigurationPropertiesBean;
import infra.context.properties.ConfigurationPropertiesScan;
import infra.context.properties.EnableConfigurationProperties;
import infra.context.properties.bind.BindMethod;
import infra.context.properties.bind.ConstructorBinding;
import infra.core.Ordered;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.lang.Contract;

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

  @Contract("null -> false")
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
