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

package infra.app.test.context.runner;

import java.util.function.Supplier;

import infra.app.test.context.assertj.AssertableReactiveWebApplicationContext;
import infra.web.server.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import infra.web.server.reactive.context.ConfigurableReactiveWebApplicationContext;

/**
 * An {@link AbstractApplicationContextRunner ApplicationContext runner} for a
 * {@link ConfigurableReactiveWebApplicationContext}.
 * <p>
 * See {@link AbstractApplicationContextRunner} for details.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
public final class ReactiveWebApplicationContextRunner extends
        AbstractApplicationContextRunner<ReactiveWebApplicationContextRunner, ConfigurableReactiveWebApplicationContext, AssertableReactiveWebApplicationContext> {

  /**
   * Create a new {@link ReactiveWebApplicationContextRunner} instance using a
   * {@link AnnotationConfigReactiveWebApplicationContext} as the underlying source.
   */
  public ReactiveWebApplicationContextRunner() {
    this(AnnotationConfigReactiveWebApplicationContext::new);
  }

  /**
   * Create a new {@link ApplicationContextRunner} instance using the specified
   * {@code contextFactory} as the underlying source.
   *
   * @param contextFactory a supplier that returns a new instance on each call
   */
  public ReactiveWebApplicationContextRunner(Supplier<ConfigurableReactiveWebApplicationContext> contextFactory) {
    super(contextFactory, ReactiveWebApplicationContextRunner::new);
  }

  private ReactiveWebApplicationContextRunner(
          RunnerConfiguration<ConfigurableReactiveWebApplicationContext> configuration) {
    super(configuration, ReactiveWebApplicationContextRunner::new);
  }

}
