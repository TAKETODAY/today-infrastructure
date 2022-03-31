/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import cn.taketoday.test.context.BootstrapContext;
import cn.taketoday.test.context.BootstrapUtils;
import cn.taketoday.test.context.CacheAwareContextLoaderDelegate;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextBootstrapper;
import cn.taketoday.test.context.cache.ContextCache;
import cn.taketoday.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import cn.taketoday.test.context.support.DefaultBootstrapContext;

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
