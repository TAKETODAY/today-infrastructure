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
import org.mockito.Mockito;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.Primary;
import infra.app.test.mock.mockito.example.ExampleGenericStringServiceCaller;
import infra.app.test.mock.mockito.example.SimpleExampleStringGenericService;
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
