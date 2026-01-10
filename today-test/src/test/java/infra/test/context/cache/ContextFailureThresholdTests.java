/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.concurrent.atomic.AtomicInteger;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.lang.TodayStrategies;
import infra.test.context.TestExecutionListeners;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.support.DependencyInjectionTestExecutionListener;

import static infra.test.context.CacheAwareContextLoaderDelegate.CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME;
import static infra.test.context.CacheAwareContextLoaderDelegate.DEFAULT_CONTEXT_FAILURE_THRESHOLD;
import static infra.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static infra.test.context.cache.ContextCacheTestUtils.resetContextCache;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests for context failure threshold support.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/15 21:21
 */
@Disabled
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
    EngineTestKit.engine("junit-jupiter")
            .selectors(
                    selectClass(PassingTestCase.class), // 3 passing
                    selectClass(FailingConfigTestCase.class), // 3 failing
                    selectClass(SharedFailingConfigTestCase.class) // 3 failing
            )
            .execute()
            .testEvents()
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
