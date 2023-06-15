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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;

import static cn.taketoday.test.context.CacheAwareContextLoaderDelegate.CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME;
import static cn.taketoday.test.context.CacheAwareContextLoaderDelegate.DEFAULT_CONTEXT_FAILURE_THRESHOLD;
import static cn.taketoday.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static cn.taketoday.test.context.cache.ContextCacheTestUtils.resetContextCache;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests for context failure threshold support.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/15 21:21
 */
class ContextFailureThresholdTests {

  private static final AtomicInteger passingLoadCount = new AtomicInteger();
  private static final AtomicInteger failingLoadCount = new AtomicInteger();

  @BeforeEach
  @AfterEach
  void resetTestFixtures() {
    resetContextCache();
    passingLoadCount.set(0);
    failingLoadCount.set(0);
    TodayStrategies.setProperty(CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME, null);
  }

  @Test
  void defaultThreshold() {
    runTests();
    assertThat(passingLoadCount.get()).isEqualTo(1);
    assertThat(failingLoadCount.get()).isEqualTo(DEFAULT_CONTEXT_FAILURE_THRESHOLD);
  }

  @Test
  void customThreshold() {
    int customThreshold = 2;
    TodayStrategies.setProperty(CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME, Integer.toString(customThreshold));

    runTests();
    assertThat(passingLoadCount.get()).isEqualTo(1);
    assertThat(failingLoadCount.get()).isEqualTo(customThreshold);
  }

  @Test
  void thresholdEffectivelyDisabled() {
    TodayStrategies.setProperty(CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME, "999999");

    runTests();
    assertThat(passingLoadCount.get()).isEqualTo(1);
    assertThat(failingLoadCount.get()).isEqualTo(6);
  }

  private static void runTests() {
    EngineTestKit.engine("junit-jupiter")//
            .selectors(//
                    selectClass(PassingTestCase.class), // 3 passing
                    selectClass(FailingConfigTestCase.class), // 3 failing
                    selectClass(SharedFailingConfigTestCase.class) // 3 failing
            )//
            .execute()//
            .testEvents()//
            .assertStatistics(stats -> stats.started(9).succeeded(3).failed(6));
    assertContextCacheStatistics(1, 2, (1 + 3 + 3));
  }

  @TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
  static abstract class BaseTestCase {

    @Test
    void test1() { }

    @Test
    void test2() { }

    @Test
    void test3() { }
  }

  @JUnitConfig(PassingConfig.class)
  static class PassingTestCase extends BaseTestCase {
  }

  @JUnitConfig(FailingConfig.class)
  static class FailingConfigTestCase extends BaseTestCase {
  }

  @JUnitConfig(FailingConfig.class)
  static class SharedFailingConfigTestCase extends BaseTestCase {
  }

  @Configuration
  static class PassingConfig {

    PassingConfig() {
      passingLoadCount.incrementAndGet();
    }
  }

  @Configuration
  static class FailingConfig {

    FailingConfig() {
      failingLoadCount.incrementAndGet();
    }

    @Bean
    String explosiveString() {
      throw new RuntimeException("Boom!");
    }
  }

}
