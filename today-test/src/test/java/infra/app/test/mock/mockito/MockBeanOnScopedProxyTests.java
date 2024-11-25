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

import infra.app.test.mock.mockito.example.ExampleService;
import infra.app.test.mock.mockito.example.ExampleServiceCaller;
import infra.app.test.mock.mockito.example.FailingExampleService;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.Scope;
import infra.context.annotation.ScopedProxyMode;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Test {@link MockBean @MockBean} when used in combination with scoped proxy targets.
 *
 * @author Phillip Webb
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/5724">gh-5724</a>
 */
@ExtendWith(InfraExtension.class)
class MockBeanOnScopedProxyTests {

  @MockBean
  private ExampleService exampleService;

  @Autowired
  private ExampleServiceCaller caller;

  @Test
  void testMocking() {
    given(this.caller.getService().greeting()).willReturn("Boot");
    assertThat(this.caller.sayGreeting()).isEqualTo("I say Boot");
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ ExampleServiceCaller.class })
  static class Config {

    @Bean
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    ExampleService exampleService() {
      return new FailingExampleService();
    }

  }

}