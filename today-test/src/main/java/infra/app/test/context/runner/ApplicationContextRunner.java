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

import infra.app.test.context.assertj.AssertableApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;

/**
 * An {@link AbstractApplicationContextRunner ApplicationContext runner} for a standard,
 * non-web environment {@link ConfigurableApplicationContext}.
 * <p>
 * See {@link AbstractApplicationContextRunner} for details.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0
 */
public class ApplicationContextRunner extends AbstractApplicationContextRunner<ApplicationContextRunner,
        ConfigurableApplicationContext, AssertableApplicationContext> {

  /**
   * Create a new {@link ApplicationContextRunner} instance using an
   * {@link AnnotationConfigApplicationContext} as the underlying source.
   */
  public ApplicationContextRunner() {
    this(AnnotationConfigApplicationContext::new);
  }

  /**
   * Create a new {@link ApplicationContextRunner} instance using the specified
   * {@code contextFactory} as the underlying source.
   *
   * @param contextFactory a supplier that returns a new instance on each call
   */
  public ApplicationContextRunner(Supplier<ConfigurableApplicationContext> contextFactory) {
    super(contextFactory, ApplicationContextRunner::new);
  }

  private ApplicationContextRunner(RunnerConfiguration<ConfigurableApplicationContext> runnerConfiguration) {
    super(runnerConfiguration, ApplicationContextRunner::new);
  }

  public static ApplicationContextRunner forDefault() {
    return new ApplicationContextRunner();
  }

  public static ApplicationContextRunner forProvider(Supplier<ConfigurableApplicationContext> contextFactory) {
    return new ApplicationContextRunner(contextFactory);
  }

}
