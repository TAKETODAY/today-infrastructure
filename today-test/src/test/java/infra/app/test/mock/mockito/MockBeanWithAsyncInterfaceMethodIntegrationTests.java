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
