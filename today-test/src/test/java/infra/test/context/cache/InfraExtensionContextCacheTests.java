/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.cache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.test.annotation.DirtiesContext;
import infra.test.context.TestExecutionListeners;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import infra.test.context.support.DirtiesContextTestExecutionListener;

import static infra.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static infra.test.context.cache.ContextCacheTestUtils.resetContextCache;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests which verify correct {@link ContextCache
 * application context caching} in conjunction with the
 * {@link InfraExtension} and the {@link DirtiesContext
 * &#064;DirtiesContext} annotation at the method level.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see ContextCacheTests
 * @see LruContextCacheTests
 * @since 4.0
 */
@JUnitConfig(locations = "../junit4/JUnit4ClassRunnerAppCtxTests-context.xml")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InfraExtensionContextCacheTests {

  private static ApplicationContext dirtiedApplicationContext;

  @Autowired
  ApplicationContext applicationContext;

  @BeforeAll
  static void verifyInitialCacheState() {
    dirtiedApplicationContext = null;
    resetContextCache();
    assertContextCacheStatistics("BeforeClass", 0, 0, 0);
  }

  @AfterAll
  static void verifyFinalCacheState() {
    assertContextCacheStatistics("AfterClass", 1, 1, 2);
  }

  @Test
  @DirtiesContext
  @Order(1)
  void dirtyContext() {
    assertContextCacheStatistics("dirtyContext()", 1, 0, 1);
    assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
    InfraExtensionContextCacheTests.dirtiedApplicationContext = this.applicationContext;
  }

  @Test
  @Order(2)
  void verifyContextDirty() {
    assertContextCacheStatistics("verifyContextWasDirtied()", 1, 0, 2);
    assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
    assertThat(this.applicationContext).as("The application context should have been 'dirtied'.").isNotSameAs(InfraExtensionContextCacheTests.dirtiedApplicationContext);
    InfraExtensionContextCacheTests.dirtiedApplicationContext = this.applicationContext;
  }

  @Test
  @Order(3)
  void verifyContextNotDirty() {
    assertContextCacheStatistics("verifyContextWasNotDirtied()", 1, 1, 2);
    assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
    assertThat(this.applicationContext).as("The application context should NOT have been 'dirtied'.").isSameAs(InfraExtensionContextCacheTests.dirtiedApplicationContext);
  }

}
