/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context;

/**
 * {@code BootstrapContext} encapsulates the context in which the <em>Infra
 * TestContext Framework</em> is bootstrapped.
 *
 * @author Sam Brannen
 * @see BootstrapWith
 * @see TestContextBootstrapper
 * @since 4.0
 */
public interface BootstrapContext {

  /**
   * Get the {@linkplain Class test class} for this bootstrap context.
   *
   * @return the test class (never {@code null})
   */
  Class<?> getTestClass();

  /**
   * Get the {@link CacheAwareContextLoaderDelegate} to use for transparent
   * interaction with the {@code ContextCache}.
   *
   * @return the context loader delegate (never {@code null})
   */
  CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate();

}
