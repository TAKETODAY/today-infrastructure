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

package infra.test.context.support;

import infra.test.context.ContextLoader;
import infra.test.context.TestContextBootstrapper;

/**
 * Default implementation of the {@link TestContextBootstrapper} SPI.
 *
 * <p>Uses {@link DelegatingSmartContextLoader} as the default {@link ContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class DefaultTestContextBootstrapper extends AbstractTestContextBootstrapper {

  /**
   * Returns {@link DelegatingSmartContextLoader}.
   */
  @Override
  protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
    return DelegatingSmartContextLoader.class;
  }

}
