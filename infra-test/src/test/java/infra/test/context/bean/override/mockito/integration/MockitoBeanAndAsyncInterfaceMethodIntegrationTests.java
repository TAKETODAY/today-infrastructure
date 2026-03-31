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

package infra.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;

import infra.aop.support.AopUtils;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.scheduling.annotation.Async;
import infra.scheduling.annotation.EnableAsync;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.mockito.MockitoAssertions.assertIsMock;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link MockitoBean @MockitoBean} where the mocked interface has an
 * {@link Async @Async} method.
 *
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
public class MockitoBeanAndAsyncInterfaceMethodIntegrationTests {

  @MockitoBean
  Transformer transformer;

  @Autowired
  MyService service;

  @Test
  void mockedMethodsAreNotAsync() throws Exception {
    assertThat(AopUtils.isAopProxy(transformer)).as("is Infra AOP proxy").isFalse();
    assertIsMock(transformer);

    given(transformer.transform("foo")).willReturn(completedFuture("bar"));
    assertThat(service.transform("foo")).isEqualTo("result: bar");
  }

  interface Transformer {

    @Async
    CompletableFuture<String> transform(String input);
  }

  record MyService(Transformer transformer) {

    String transform(String input) throws Exception {
      return "result: " + this.transformer.transform(input).get();
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAsync
  static class Config {

    @Bean
    Transformer transformer() {
      return input -> completedFuture(input.toUpperCase());
    }

    @Bean
    MyService myService(Transformer transformer) {
      return new MyService(transformer);
    }
  }

}
