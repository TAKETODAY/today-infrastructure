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

package infra.test.context.async;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.scheduling.annotation.Async;
import infra.scheduling.annotation.EnableAsync;
import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;

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
