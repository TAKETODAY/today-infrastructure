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

package infra.app.test.context.assertj;

import java.util.function.Supplier;

import infra.app.test.context.runner.WebApplicationContextRunner;
import infra.web.mock.ConfigurableWebApplicationContext;
import infra.web.server.context.ConfigurableWebServerApplicationContext;
import infra.web.server.context.WebServerApplicationContext;

/**
 * A {@link WebServerApplicationContext} that additionally supports AssertJ style assertions.
 * Can be used to decorate an existing mock web application context or an application
 * context that failed to start.
 * <p>
 * See {@link ApplicationContextAssertProvider} for more details.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebApplicationContextRunner
 * @since 4.0
 */
public interface AssertableWebApplicationContext
        extends ApplicationContextAssertProvider<ConfigurableWebApplicationContext>, ConfigurableWebApplicationContext {

  /**
   * Factory method to create a new {@link AssertableWebApplicationContext} instance.
   *
   * @param contextSupplier a supplier that will either return a fully configured
   * {@link ConfigurableWebServerApplicationContext} or throw an exception if the context
   * fails to start.
   * @return a {@link AssertableWebApplicationContext} instance
   */
  static AssertableWebApplicationContext get(Supplier<? extends ConfigurableWebApplicationContext> contextSupplier) {
    return ApplicationContextAssertProvider.get(AssertableWebApplicationContext.class,
            ConfigurableWebApplicationContext.class, contextSupplier);
  }

}
