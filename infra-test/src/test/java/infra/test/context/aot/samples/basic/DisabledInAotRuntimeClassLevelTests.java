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

package infra.test.context.aot.samples.basic;

import org.junit.jupiter.api.Test;

import infra.aot.AotDetector;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.TestPropertySource;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.aot.samples.common.DefaultMessageService;
import infra.test.context.aot.samples.common.MessageService;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DisabledInAotMode} class-level tests.
 *
 * <p>This test class differs from {@link DisabledInAotProcessingTests} whose
 * {@code ApplicationContext} will simply fail during AOT processing. Whereas,
 * the {@code ApplicationContext} for this test class can be properly processed
 * for AOT optimizations, but we want to ensure that we can also disable such a
 * test class in AOT mode if desired.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DisabledInAotRuntimeMethodLevelTests
 * @see DisabledInAotProcessingTests
 * @since 4.0
 */
@JUnitConfig
@TestPropertySource(properties = "disabledInAotMode = class-level")
@DisabledInAotMode
public class DisabledInAotRuntimeClassLevelTests {

  @Test
  void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
          @Value("${disabledInAotMode}") String disabledInAotMode) {

    assertThat(AotDetector.useGeneratedArtifacts()).as("Should be disabled in AOT mode").isFalse();
    assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
    assertThat(disabledInAotMode).isEqualTo("class-level");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    MessageService defaultMessageService() {
      return new DefaultMessageService();
    }
  }

}
