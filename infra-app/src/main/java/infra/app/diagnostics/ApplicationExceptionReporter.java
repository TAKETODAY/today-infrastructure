/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.diagnostics;

import infra.app.Application;
import infra.context.ApplicationContextAware;
import infra.context.ConfigurableApplicationContext;
import infra.lang.TodayStrategies;

/**
 * Callback interface used to support custom reporting of {@link Application}
 * startup errors. {@link ApplicationExceptionReporter reporters} are loaded via the
 * {@link TodayStrategies} and must declare a public constructor with a single
 * {@link ConfigurableApplicationContext} parameter.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationContextAware
 * @since 4.0 2022/2/19 22:36
 */
@FunctionalInterface
public interface ApplicationExceptionReporter {

  /**
   * Report a startup failure to the user.
   *
   * @param failure the source failure
   * @return {@code true} if the failure was reported or {@code false} if default
   * reporting should occur.
   */
  boolean reportException(Throwable failure);

}
