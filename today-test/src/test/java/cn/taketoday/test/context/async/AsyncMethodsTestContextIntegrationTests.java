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

package cn.taketoday.test.context.async;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.scheduling.annotation.Async;
import cn.taketoday.scheduling.annotation.EnableAsync;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

/**
 * Integration tests for applications using {@link Async @Async} methods with
 * {@code @DirtiesContext}.
 *
 * <p>Execute this test class with {@code -Xmx8M} to verify that there are no
 * issues with memory leaks as raised in
 * <a href="https://github.com/spring-projects/spring-framework/issues/23571">gh-23571</a>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@Disabled("Only meant to be executed manually")
class AsyncMethodsTestContextIntegrationTests {

  @RepeatedTest(200)
  @DirtiesContext
  void test() {
    // If we don't run out of memory, then this test is a success.
  }

  @Configuration
  @EnableAsync
  static class Config {

    @Bean
    AsyncService asyncService() {
      return new AsyncService();
    }
  }

  static class AsyncService {

    @Async
    void asyncMethod() {
    }
  }

}
