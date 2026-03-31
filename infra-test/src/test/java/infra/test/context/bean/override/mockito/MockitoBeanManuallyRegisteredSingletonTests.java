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

package infra.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies support for overriding a manually registered singleton bean with
 * {@link MockitoBean @MockitoBean}.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 5.0
 */
@JUnitConfig(initializers = MockitoBeanManuallyRegisteredSingletonTests.SingletonRegistrar.class)
class MockitoBeanManuallyRegisteredSingletonTests {

  @MockitoBean
  MessageService messageService;

  @Test
  void test() {
    when(messageService.getMessage()).thenReturn("override");
    assertThat(messageService.getMessage()).isEqualTo("override");
  }

  static class SingletonRegistrar implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      applicationContext.getBeanFactory().registerSingleton("messageService", new MessageService());
    }
  }

  static class MessageService {

    String getMessage() {
      return "production";
    }
  }

}
