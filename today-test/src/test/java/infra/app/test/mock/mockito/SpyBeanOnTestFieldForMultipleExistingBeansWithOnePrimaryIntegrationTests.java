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
import org.mockito.Mockito;

import infra.app.test.mock.mockito.example.ExampleGenericStringServiceCaller;
import infra.app.test.mock.mockito.example.SimpleExampleStringGenericService;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.Primary;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Test {@link SpyBean @SpyBean} on a test class field can be used to inject a spy
 * instance when there are multiple candidates and one is primary.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
class SpyBeanOnTestFieldForMultipleExistingBeansWithOnePrimaryIntegrationTests {

  @SpyBean
  private SimpleExampleStringGenericService spy;

  @Autowired
  private ExampleGenericStringServiceCaller caller;

  @Test
  void testSpying() {
    assertThat(this.caller.sayGreeting()).isEqualTo("I say two");
    assertThat(Mockito.mockingDetails(this.spy).getMockCreationSettings().getMockName().toString())
            .isEqualTo("two");
    then(this.spy).should().greeting();
  }

  @Configuration(proxyBeanMethods = false)
  @Import(ExampleGenericStringServiceCaller.class)
  static class Config {

    @Bean
    SimpleExampleStringGenericService one() {
      return new SimpleExampleStringGenericService("one");
    }

    @Bean
    @Primary
    SimpleExampleStringGenericService two() {
      return new SimpleExampleStringGenericService("two");
    }

  }

}
