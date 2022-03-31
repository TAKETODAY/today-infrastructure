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

package cn.taketoday.test.context.cache;

import cn.taketoday.test.context.cache.ContextCache;
import cn.taketoday.test.context.cache.DefaultCacheAwareContextLoaderDelegate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Collection of utility methods for testing scenarios involving the
 * {@link ContextCache}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class ContextCacheTestUtils {

	/**
	 * Reset the state of the static context cache in {@link DefaultCacheAwareContextLoaderDelegate}.
	 */
	public static final void resetContextCache() {
		DefaultCacheAwareContextLoaderDelegate.defaultContextCache.reset();
	}

	/**
	 * Assert the statistics of the static context cache in {@link DefaultCacheAwareContextLoaderDelegate}.
	 *
	 * @param usageScenario the scenario in which the statistics are used
	 * @param expectedSize the expected number of contexts in the cache
	 * @param expectedHitCount the expected hit count
	 * @param expectedMissCount the expected miss count
	 */
	public static final void assertContextCacheStatistics(String usageScenario, int expectedSize, int expectedHitCount,
			int expectedMissCount) {
		assertContextCacheStatistics(DefaultCacheAwareContextLoaderDelegate.defaultContextCache, usageScenario,
			expectedSize, expectedHitCount, expectedMissCount);
	}

	/**
	 * Assert the statistics of the supplied context cache.
	 *
	 * @param contextCache the cache to assert against
	 * @param usageScenario the scenario in which the statistics are used
	 * @param expectedSize the expected number of contexts in the cache
	 * @param expectedHitCount the expected hit count
	 * @param expectedMissCount the expected miss count
	 */
	public static final void assertContextCacheStatistics(ContextCache contextCache, String usageScenario,
			int expectedSize, int expectedHitCount, int expectedMissCount) {

		assertThat(contextCache.size()).as("Verifying number of contexts in cache (" + usageScenario + ").").isEqualTo(expectedSize);
		assertThat(contextCache.getHitCount()).as("Verifying number of cache hits (" + usageScenario + ").").isEqualTo(expectedHitCount);
		assertThat(contextCache.getMissCount()).as("Verifying number of cache misses (" + usageScenario + ").").isEqualTo(expectedMissCount);
	}

}
