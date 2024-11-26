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
