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

package infra.app.test.context.assertj;

import java.util.function.Supplier;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;

/**
 * An {@link ApplicationContext} that additionally supports AssertJ style assertions. Can
 * be used to decorate an existing application context or an application context that
 * failed to start.
 * <p>
 * See {@link ApplicationContextAssertProvider} for more details.
 *
 * @author Phillip Webb
 * @see ApplicationContextRunner
 * @see ApplicationContext
 * @since 4.0
 */
public interface AssertableApplicationContext
        extends ApplicationContextAssertProvider<ConfigurableApplicationContext>, ConfigurableApplicationContext {

  /**
   * Factory method to create a new {@link AssertableApplicationContext} instance.
   *
   * @param contextSupplier a supplier that will either return a fully configured
   * {@link ConfigurableApplicationContext} or throw an exception if the context fails
   * to start.
   * @return an {@link AssertableApplicationContext} instance
   */
  static AssertableApplicationContext get(Supplier<? extends ConfigurableApplicationContext> contextSupplier) {
    return ApplicationContextAssertProvider.get(AssertableApplicationContext.class,
            ConfigurableApplicationContext.class, contextSupplier);
  }

}
