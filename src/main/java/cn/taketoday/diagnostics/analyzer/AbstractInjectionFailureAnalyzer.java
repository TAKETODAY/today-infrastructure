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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.diagnostics.analyzer;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.boot.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.boot.diagnostics.FailureAnalysis;
import cn.taketoday.boot.diagnostics.FailureAnalyzer;
import cn.taketoday.util.ClassUtils;

/**
 * Abstract base class for a {@link FailureAnalyzer} that handles some kind of injection
 * failure.
 *
 * @param <T> the type of exception to analyze
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.4.1
 */
public abstract class AbstractInjectionFailureAnalyzer<T extends Throwable> extends AbstractFailureAnalyzer<T> {

  @Override
  protected final FailureAnalysis analyze(Throwable rootFailure, T cause) {
    return analyze(rootFailure, cause, getDescription(rootFailure));
  }

  private String getDescription(Throwable rootFailure) {
    UnsatisfiedDependencyException unsatisfiedDependency = findMostNestedCause(rootFailure,
            UnsatisfiedDependencyException.class);
    if (unsatisfiedDependency != null) {
      return getDescription(unsatisfiedDependency);
    }
    BeanInstantiationException beanInstantiationException = findMostNestedCause(rootFailure,
            BeanInstantiationException.class);
    if (beanInstantiationException != null) {
      return getDescription(beanInstantiationException);
    }
    return null;
  }

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

  private String getDescription(UnsatisfiedDependencyException ex) {
    InjectionPoint injectionPoint = ex.getInjectionPoint();
    if (injectionPoint != null) {
      if (injectionPoint.getField() != null) {
        return String.format("Field %s in %s", injectionPoint.getField().getName(),
                injectionPoint.getField().getDeclaringClass().getName());
      }
      if (injectionPoint.getMethodParameter() != null) {
        if (injectionPoint.getMethodParameter().getConstructor() != null) {
          return String.format("Parameter %d of constructor in %s",
                  injectionPoint.getMethodParameter().getParameterIndex(),
                  injectionPoint.getMethodParameter().getDeclaringClass().getName());
        }
        return String.format("Parameter %d of method %s in %s",
                injectionPoint.getMethodParameter().getParameterIndex(),
                injectionPoint.getMethodParameter().getMethod().getName(),
                injectionPoint.getMethodParameter().getDeclaringClass().getName());
      }
    }
    return ex.getResourceDescription();
  }

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
  protected abstract FailureAnalysis analyze(Throwable rootFailure, T cause, String description);

}
