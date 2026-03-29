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

package infra.test.context.bean.override.convention;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Fallback;
import infra.test.annotation.DirtiesContext;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link TestBean @TestBean} can be used to override a bean by-type
 * when there are multiple candidates and only one that is not a fallback.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
@DirtiesContext
class TestBeanWithMultipleExistingBeansAndOneNonFallbackIntegrationTests {

  @TestBean
  ExampleService service;

  @Autowired
  List<ExampleService> services;

  static ExampleService service() {
    return () -> "overridden";
  }

  @Test
  void test() {
    assertThat(service.greeting()).isEqualTo("overridden");
    assertThat(services).extracting(ExampleService::greeting)
            .containsExactlyInAnyOrder("overridden", "two", "three");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    ExampleService one() {
      return () -> "one";
    }

    @Bean
    @Fallback
    ExampleService two() {
      return () -> "two";
    }

    @Bean
    @Fallback
    ExampleService three() {
      return () -> "three";
    }
  }

}
