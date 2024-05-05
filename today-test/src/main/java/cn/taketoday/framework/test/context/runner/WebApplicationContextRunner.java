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

package cn.taketoday.framework.test.context.runner;

import java.util.function.Supplier;

import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.web.mock.AnnotationConfigMockWebApplicationContext;
import cn.taketoday.framework.test.context.assertj.AssertableWebApplicationContext;
import cn.taketoday.web.server.context.ConfigurableWebServerApplicationContext;
import cn.taketoday.web.mock.ConfigurableWebApplicationContext;
import cn.taketoday.web.mock.WebApplicationContext;

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
