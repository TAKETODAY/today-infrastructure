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

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.atomic.AtomicInteger;

import infra.beans.factory.annotation.Autowired;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import infra.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import infra.test.context.support.DirtiesContextTestExecutionListener;

import static infra.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test which verifies correct interaction between the
 * {@link DirtiesContextBeforeModesTestExecutionListener},
 * {@link DependencyInjectionTestExecutionListener}, and
 * {@link DirtiesContextTestExecutionListener} when
 * {@link DirtiesContext @DirtiesContext} is used at the method level.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MethodLevelDirtiesContextTests {

  private static final AtomicInteger contextCount = new AtomicInteger();

  @Autowired
  private ConfigurableApplicationContext context;

  @Autowired
  private Integer count;

  @Test
  @Order(1)
  void basics() throws Exception {
    performAssertions(1);
  }

  @Test
  @Order(2)
  @DirtiesContext(methodMode = BEFORE_METHOD)
  void dirtyContextBeforeTestMethod() throws Exception {
    performAssertions(2);
  }

  @Test
  @Order(3)
  @DirtiesContext
  void dirtyContextAfterTestMethod() throws Exception {
    performAssertions(2);
  }

  @Test
  @Order(4)
  void backToBasics() throws Exception {
    performAssertions(3);
  }

  private void performAssertions(int expectedContextCreationCount) throws Exception {
    assertThat(this.context).as("context is required").isNotNull();
    assertThat(this.context.isActive()).as("context must be active").isTrue();

    assertThat(this.count).as("count is required").isNotNull();
    assertThat(this.count.intValue()).as("count: ").isEqualTo(expectedContextCreationCount);

    assertThat(contextCount.get()).as("context creation count: ").isEqualTo(expectedContextCreationCount);
  }

  @Configuration
  static class Config {

    @Bean
    Integer count() {
      return contextCount.incrementAndGet();
    }
  }

}
