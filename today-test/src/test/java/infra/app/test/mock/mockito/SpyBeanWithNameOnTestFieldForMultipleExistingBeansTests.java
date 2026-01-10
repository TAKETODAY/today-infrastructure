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
import org.mockito.MockingDetails;
import org.mockito.Mockito;

import infra.app.test.mock.mockito.example.SimpleExampleStringGenericService;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link SpyBean @SpyBean} on a test class field can be used to inject a spy
 * instance when there are multiple candidates and one is chosen using the name attribute.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(InfraExtension.class)
class SpyBeanWithNameOnTestFieldForMultipleExistingBeansTests {

  @SpyBean(name = "two")
  private SimpleExampleStringGenericService spy;

  @Test
  void testSpying() {
    MockingDetails mockingDetails = Mockito.mockingDetails(this.spy);
    assertThat(mockingDetails.isSpy()).isTrue();
    assertThat(mockingDetails.getMockCreationSettings().getMockName().toString()).isEqualTo("two");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    SimpleExampleStringGenericService one() {
      return new SimpleExampleStringGenericService("one");
    }

    @Bean
    SimpleExampleStringGenericService two() {
      return new SimpleExampleStringGenericService("two");
    }

  }

}
