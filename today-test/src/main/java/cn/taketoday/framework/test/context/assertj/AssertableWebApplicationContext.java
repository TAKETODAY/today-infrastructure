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

package cn.taketoday.framework.test.context.assertj;

import java.util.function.Supplier;

import cn.taketoday.framework.test.context.runner.WebApplicationContextRunner;
import cn.taketoday.framework.web.context.ConfigurableWebServerApplicationContext;
import cn.taketoday.framework.web.context.WebServerApplicationContext;

/**
 * A {@link WebServerApplicationContext} that additionally supports AssertJ style assertions.
 * Can be used to decorate an existing servlet web application context or an application
 * context that failed to start.
 * <p>
 * See {@link ApplicationContextAssertProvider} for more details.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebApplicationContextRunner
 * @since 4.0
 */
public interface AssertableWebApplicationContext extends ApplicationContextAssertProvider<ConfigurableWebServerApplicationContext>, WebServerApplicationContext {

  /**
   * Factory method to create a new {@link AssertableWebApplicationContext} instance.
   *
   * @param contextSupplier a supplier that will either return a fully configured
   * {@link ConfigurableWebServerApplicationContext} or throw an exception if the context
   * fails to start.
   * @return a {@link AssertableWebApplicationContext} instance
   */
  static AssertableWebApplicationContext get(Supplier<? extends ConfigurableWebServerApplicationContext> contextSupplier) {
    return ApplicationContextAssertProvider.get(AssertableWebApplicationContext.class,
            ConfigurableWebServerApplicationContext.class, contextSupplier);
  }

}
