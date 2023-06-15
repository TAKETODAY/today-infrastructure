/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Collection of utility methods for testing scenarios involving the
 * {@link ContextCache}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ContextCacheTestUtils {

  /**
   * Reset the state of the static context cache in {@link DefaultCacheAwareContextLoaderDelegate}.
   */
  public static void resetContextCache() {
    DefaultCacheAwareContextLoaderDelegate.defaultContextCache.reset();
  }

  /**
   * Assert the statistics of the static context cache in {@link DefaultCacheAwareContextLoaderDelegate}.
   *
   * @param expectedSize the expected number of contexts in the cache
   * @param expectedHitCount the expected hit count
   * @param expectedMissCount the expected miss count
   */
  public static void assertContextCacheStatistics(int expectedSize, int expectedHitCount, int expectedMissCount) {
    assertContextCacheStatistics(null, expectedSize, expectedHitCount, expectedMissCount);
  }

  /**
   * Assert the statistics of the static context cache in {@link DefaultCacheAwareContextLoaderDelegate}.
   *
   * @param usageScenario the scenario in which the statistics are used
   * @param expectedSize the expected number of contexts in the cache
   * @param expectedHitCount the expected hit count
   * @param expectedMissCount the expected miss count
   */
  public static void assertContextCacheStatistics(String usageScenario, int expectedSize, int expectedHitCount,
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
  public static void assertContextCacheStatistics(ContextCache contextCache, String usageScenario,
          int expectedSize, int expectedHitCount, int expectedMissCount) {

    String context = (StringUtils.hasText(usageScenario) ? " (" + usageScenario + ")" : "");

    assertSoftly(softly -> {
      softly.assertThat(contextCache.size()).as("contexts in cache" + context).isEqualTo(expectedSize);
      softly.assertThat(contextCache.getHitCount()).as("cache hits" + context).isEqualTo(expectedHitCount);
      softly.assertThat(contextCache.getMissCount()).as("cache misses" + context).isEqualTo(expectedMissCount);
    });
  }

}
