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

package infra.app.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.scheduling.annotation.Async;
import infra.scheduling.annotation.EnableAsync;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for a mock bean where the mocked interface has an async method.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(InfraExtension.class)
class MockBeanWithAsyncInterfaceMethodIntegrationTests {

  @MockBean
  private Transformer transformer;

  @Autowired
  private MyService service;

  @Test
  void mockedMethodsAreNotAsync() {
    given(this.transformer.transform("foo")).willReturn("bar");
    assertThat(this.service.transform("foo")).isEqualTo("bar");
  }

  interface Transformer {

    @Async
    String transform(String input);

  }

  static class MyService {

    private final Transformer transformer;

    MyService(Transformer transformer) {
      this.transformer = transformer;
    }

    String transform(String input) {
      return this.transformer.transform(input);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableAsync
  static class MyConfiguration {

    @Bean
    MyService myService(Transformer transformer) {
      return new MyService(transformer);
    }

  }

}
