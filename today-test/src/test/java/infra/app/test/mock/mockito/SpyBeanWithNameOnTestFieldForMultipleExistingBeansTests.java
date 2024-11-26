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
