/*
 * Copyright 2002-present the original author or authors.
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

package infra.context;

import infra.core.Ordered;
import infra.core.annotation.Order;

/**
 * Callback interface for initializing a {@link ConfigurableApplicationContext}
 * prior to being {@linkplain ConfigurableApplicationContext#refresh() refreshed}.
 *
 * <p>Typically used within web applications that require some programmatic initialization
 * of the application context. For example, registering property sources or activating
 * profiles against the {@linkplain ConfigurableApplicationContext#getEnvironment()
 * context's environment}.
 *
 * <p>{@code ApplicationContextInitializer} processors are encouraged to detect
 * whether {@link Ordered Ordered} interface has been
 * implemented or if the {@link Order @Order} annotation is
 * present and to sort instances accordingly if so prior to invocation.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/5 10:29
 */
@FunctionalInterface
public interface ApplicationContextInitializer {

  /**
   * Initialize the given application context.
   * <p>
   * before using {@code applicationContext} you must test if context is you want.
   * like this:
   * <pre>{@code
   *  if (applicationContext instanceof GenericApplicationContext generic) {
   *
   *  }
   * }</pre>
   *
   * @param context the application context to bootstrap
   */
  void initialize(ConfigurableApplicationContext context);

}
