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

package cn.taketoday.diagnostics;

import cn.taketoday.core.ResolvableType;

/**
 * Abstract base class for most {@code FailureAnalyzer} implementations.
 *
 * @param <T> the type of exception to analyze
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0
 */
public abstract class AbstractFailureAnalyzer<T extends Throwable> implements FailureAnalyzer {

  @Override
  public FailureAnalysis analyze(Throwable failure) {
    T cause = findCause(failure, getCauseType());
    return (cause != null) ? analyze(failure, cause) : null;
  }

  /**
   * Returns an analysis of the given {@code rootFailure}, or {@code null} if no
   * analysis was possible.
   *
   * @param rootFailure the root failure passed to the analyzer
   * @param cause the actual found cause
   * @return the analysis or {@code null}
   */
  protected abstract FailureAnalysis analyze(Throwable rootFailure, T cause);

  /**
   * Return the cause type being handled by the analyzer. By default the class generic
   * is used.
   *
   * @return the cause type
   */
  @SuppressWarnings("unchecked")
  protected Class<? extends T> getCauseType() {
    return (Class<? extends T>) ResolvableType.fromClass(AbstractFailureAnalyzer.class, getClass()).resolveGeneric();
  }

  @SuppressWarnings("unchecked")
  protected final <E extends Throwable> E findCause(Throwable failure, Class<E> type) {
    while (failure != null) {
      if (type.isInstance(failure)) {
        return (E) failure;
      }
      failure = failure.getCause();
    }
    return null;
  }

}
