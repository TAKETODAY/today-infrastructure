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

import infra.beans.factory.BeanFactory;
import infra.context.ApplicationContext;
import infra.core.env.Environment;

/**
 * A {@code FailureAnalyzer} is used to analyze a failure and provide diagnostic
 * information that can be displayed to the user.
 * <p>
 * Implementations should be added as a
 * {@code today.strategies} entries. The following constructor parameter types are
 * supported:
 * <ul>
 * <li>{@link BeanFactory}</li>
 * <li>{@link Environment}</li>
 * <li>{@link ApplicationContext}</li>
 * </ul>
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface FailureAnalyzer {

  /**
   * Returns an analysis of the given {@code failure}, or {@code null} if no analysis
   * was possible.
   *
   * @param failure the failure
   * @return the analysis or {@code null}
   */
  @Nullable
  FailureAnalysis analyze(Throwable failure);

}
