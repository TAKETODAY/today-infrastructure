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
