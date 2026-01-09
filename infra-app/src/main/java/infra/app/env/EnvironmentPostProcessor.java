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

package infra.app.env;

import infra.app.Application;
import infra.app.BootstrapContext;
import infra.app.BootstrapRegistry;
import infra.app.ConfigurableBootstrapContext;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;

/**
 * Allows for customization of the application's {@link Environment} prior to the
 * application context being refreshed.
 * <p>
 * EnvironmentPostProcessor implementations have to be registered in
 * {@code META-INF/today.strategies}, using the fully qualified name of this class as the
 * key. Implementations may implement the {@link Ordered Ordered}
 * interface or use an {@link Order @Order} annotation
 * if they wish to be invoked in specific order.
 * <p> {@code EnvironmentPostProcessor} implementations may optionally
 * take the following constructor parameters:
 * <ul>
 * <li>{@link ConfigurableBootstrapContext} - A bootstrap context that can be used to
 * store objects that may be expensive to create, or need to be shared
 * ({@link BootstrapContext} or {@link BootstrapRegistry} may also be used).</li>
 * </ul>
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface EnvironmentPostProcessor {

  /**
   * Post-process the given {@code environment}.
   *
   * @param environment the environment to post-process
   * @param application the application to which the environment belongs
   */
  void postProcessEnvironment(ConfigurableEnvironment environment, Application application);

}
