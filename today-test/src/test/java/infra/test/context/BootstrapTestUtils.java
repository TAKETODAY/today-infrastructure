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

package infra.test.context;

import infra.test.context.support.DefaultBootstrapContext;

/**
 * Collection of test-related utility methods for working with {@link BootstrapContext
 * BootstrapContexts} and {@link TestContextBootstrapper TestContextBootstrappers}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class BootstrapTestUtils {

  private BootstrapTestUtils() {
    /* no-op */
  }

  public static BootstrapContext buildBootstrapContext(Class<?> testClass,
          CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
    return new DefaultBootstrapContext(testClass, cacheAwareContextLoaderDelegate);
  }

  public static TestContextBootstrapper resolveTestContextBootstrapper(BootstrapContext bootstrapContext) {
    return BootstrapUtils.resolveTestContextBootstrapper(bootstrapContext);
  }

}
