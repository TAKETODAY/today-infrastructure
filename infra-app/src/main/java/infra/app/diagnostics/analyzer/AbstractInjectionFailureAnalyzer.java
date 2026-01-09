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

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.app.diagnostics.FailureAnalyzer;
import infra.beans.BeanInstantiationException;
import infra.beans.factory.InjectionPoint;
import infra.beans.factory.UnsatisfiedDependencyException;
import infra.core.MethodParameter;
import infra.util.ClassUtils;

/**
 * Abstract base class for a {@link FailureAnalyzer} that handles some kind of injection
 * failure.
 *
 * @param <T> the type of exception to analyze
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractInjectionFailureAnalyzer<T extends Throwable> extends AbstractFailureAnalyzer<T> {

  @Nullable
  @Override
  protected final FailureAnalysis analyze(Throwable rootFailure, T cause) {
    return analyze(rootFailure, cause, getDescription(rootFailure));
  }

  @Nullable
  private String getDescription(Throwable rootFailure) {
    var unsatisfiedDependency = findMostNestedCause(rootFailure, UnsatisfiedDependencyException.class);
    if (unsatisfiedDependency != null) {
      return getDescription(unsatisfiedDependency);
    }
    var beanInstantiationException = findMostNestedCause(rootFailure, BeanInstantiationException.class);
    if (beanInstantiationException != null) {
      return getDescription(beanInstantiationException);
    }
    return null;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <C extends Exception> C findMostNestedCause(Throwable root, Class<C> type) {
    Throwable candidate = root;
    C result = null;
    while (candidate != null) {
      if (type.isAssignableFrom(candidate.getClass())) {
        result = (C) candidate;
      }
      candidate = candidate.getCause();
    }
    return result;
  }

  @Nullable
  private String getDescription(UnsatisfiedDependencyException ex) {
    String description = getDescription(ex.getInjectionPoint());
    if (description != null) {
      return description;
    }
    return ex.getResourceDescription();
  }

  @Nullable
  @SuppressWarnings("NullAway")
  String getDescription(@Nullable InjectionPoint injectionPoint) {
    if (injectionPoint != null) {
      if (injectionPoint.getField() != null) {
        return String.format("Field %s in %s", injectionPoint.getField().getName(),
                injectionPoint.getField().getDeclaringClass().getName());
      }
      MethodParameter parameter = injectionPoint.getMethodParameter();
      if (parameter != null) {
        if (parameter.getConstructor() != null) {
          return String.format("Parameter %d of constructor in %s",
                  parameter.getParameterIndex(),
                  parameter.getDeclaringClass().getName());
        }
        return String.format("Parameter %d of method %s in %s",
                parameter.getParameterIndex(),
                parameter.getMethod().getName(),
                parameter.getDeclaringClass().getName());
      }
    }
    return null;
  }

  @SuppressWarnings("NullAway")
  private String getDescription(BeanInstantiationException ex) {
    if (ex.getConstructingMethod() != null) {
      return String.format("Method %s in %s", ex.getConstructingMethod().getName(),
              ex.getConstructingMethod().getDeclaringClass().getName());
    }
    if (ex.getConstructor() != null) {
      return String.format("Constructor in %s",
              ClassUtils.getUserClass(ex.getConstructor().getDeclaringClass()).getName());
    }
    return ex.getBeanClass().getName();
  }

  /**
   * Returns an analysis of the given {@code rootFailure}, or {@code null} if no
   * analysis was possible.
   *
   * @param rootFailure the root failure passed to the analyzer
   * @param cause the actual found cause
   * @param description the description of the injection point or {@code null}
   * @return the analysis or {@code null}
   */
  @Nullable
  protected abstract FailureAnalysis analyze(Throwable rootFailure, T cause, @Nullable String description);

}
