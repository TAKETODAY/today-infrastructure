/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.app.diagnostics;

import infra.beans.factory.BeanFactory;
import infra.context.ApplicationContext;
import infra.core.env.Environment;
import infra.lang.Nullable;

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
