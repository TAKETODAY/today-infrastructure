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

package infra.app.diagnostics;

import org.jspecify.annotations.Nullable;

import infra.core.ResolvableType;
import infra.lang.Assert;

/**
 * Abstract base class for most {@code FailureAnalyzer} implementations.
 *
 * @param <T> the type of exception to analyze
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractFailureAnalyzer<T extends Throwable> implements FailureAnalyzer {

  @Nullable
  @Override
  public FailureAnalysis analyze(Throwable failure) {
    T cause = findCause(failure, getCauseType());
    if (cause != null) {
      return analyze(failure, cause);
    }
    return null;
  }

  /**
   * Returns an analysis of the given {@code rootFailure}, or {@code null} if no
   * analysis was possible.
   *
   * @param rootFailure the root failure passed to the analyzer
   * @param cause the actual found cause
   * @return the analysis or {@code null}
   */
  @Nullable
  protected abstract FailureAnalysis analyze(Throwable rootFailure, T cause);

  /**
   * Return the cause type being handled by the analyzer. By default, the class generic
   * is used.
   *
   * @return the cause type
   */
  @SuppressWarnings("unchecked")
  protected Class<? extends T> getCauseType() {
    Class<?> generic = ResolvableType.forClass(AbstractFailureAnalyzer.class, getClass()).resolveGeneric();
    Assert.state(generic != null, "Generic type is required");
    return (Class<? extends T>) generic;
  }

  @Nullable
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
