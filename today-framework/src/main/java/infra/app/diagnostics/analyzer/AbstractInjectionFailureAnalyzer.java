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
