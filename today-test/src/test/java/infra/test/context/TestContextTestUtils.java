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

import infra.test.context.BootstrapContext;
import infra.test.context.BootstrapUtils;
import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.TestContext;
import infra.test.context.TestContextBootstrapper;
import infra.test.context.cache.ContextCache;
import infra.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import infra.test.context.support.DefaultBootstrapContext;

/**
 * Collection of test-related utility methods for working with {@link TestContext TestContexts}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class TestContextTestUtils {

  public static TestContext buildTestContext(Class<?> testClass, ContextCache contextCache) {
    return buildTestContext(testClass, new DefaultCacheAwareContextLoaderDelegate(contextCache));
  }

  public static TestContext buildTestContext(Class<?> testClass,
          CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

    BootstrapContext bootstrapContext = new DefaultBootstrapContext(testClass, cacheAwareContextLoaderDelegate);
    TestContextBootstrapper testContextBootstrapper = BootstrapUtils.resolveTestContextBootstrapper(bootstrapContext);
    return testContextBootstrapper.buildTestContext();
  }

}
