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
import infra.beans.factory.annotation.Autowired;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Test {@link MockBean @MockBean} on a test class field can be used to replace existing
 * beans.
 *
 * @author Phillip Webb
 * @see MockBeanOnTestFieldForExistingBeanCacheIntegrationTests
 */
@ExtendWith(InfraExtension.class)
@ContextConfiguration(classes = MockBeanOnTestFieldForExistingBeanConfig.class)
class MockBeanOnTestFieldForExistingBeanIntegrationTests {

  @MockBean
  private ExampleService exampleService;

  @Autowired
  private ExampleServiceCaller caller;

  @Test
  void testMocking() {
    given(this.exampleService.greeting()).willReturn("Boot");
    assertThat(this.caller.sayGreeting()).isEqualTo("I say Boot");
  }

}