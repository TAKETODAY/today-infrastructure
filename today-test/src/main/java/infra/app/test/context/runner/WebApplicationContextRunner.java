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

package infra.app.test.context.runner;

import java.util.function.Supplier;

import infra.app.test.context.assertj.AssertableWebApplicationContext;
import infra.mock.web.MockContextImpl;
import infra.web.mock.AnnotationConfigMockWebApplicationContext;
import infra.web.mock.ConfigurableWebApplicationContext;
import infra.web.mock.WebApplicationContext;
import infra.web.server.context.ConfigurableWebServerApplicationContext;

/**
 * An {@link AbstractApplicationContextRunner ApplicationContext runner} for a Mock
 * based {@link ConfigurableWebServerApplicationContext}.
 * <p>
 * See {@link AbstractApplicationContextRunner} for details.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class WebApplicationContextRunner
        extends AbstractApplicationContextRunner<WebApplicationContextRunner, ConfigurableWebApplicationContext, AssertableWebApplicationContext> {

  /**
   * Create a new {@link WebApplicationContextRunner} instance using an
   * {@link AnnotationConfigMockWebApplicationContext} with a
   * {@link MockContextImpl} as the underlying source.
   *
   * @see #withMockContext(Supplier)
   */
  public WebApplicationContextRunner() {
    this(withMockContext(AnnotationConfigMockWebApplicationContext::new));
  }

  /**
   * Create a new {@link WebApplicationContextRunner} instance using the specified
   * {@code contextFactory} as the underlying source.
   *
   * @param contextFactory a supplier that returns a new instance on each call
   */
  public WebApplicationContextRunner(Supplier<ConfigurableWebApplicationContext> contextFactory) {
    super(contextFactory, WebApplicationContextRunner::new);
  }

  private WebApplicationContextRunner(RunnerConfiguration<ConfigurableWebApplicationContext> configuration) {
    super(configuration, WebApplicationContextRunner::new);
  }

  /**
   * Decorate the specified {@code contextFactory} to set a {@link MockContextImpl}
   * on each newly created {@link WebApplicationContext}.
   *
   * @param contextFactory the context factory to decorate
   * @return an updated supplier that will set the {@link MockContextImpl}
   */
  public static Supplier<ConfigurableWebApplicationContext> withMockContext(
          Supplier<ConfigurableWebApplicationContext> contextFactory) {
    return (contextFactory != null) ? () -> {
      ConfigurableWebApplicationContext context = contextFactory.get();
      context.setMockContext(new MockContextImpl());
      return context;
    } : null;
  }

}
