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
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.app.test.mock.mockito.example.ExampleServiceCaller;
import infra.app.test.mock.mockito.example.SimpleExampleService;
import infra.test.annotation.DirtiesContext;
import infra.test.annotation.DirtiesContext.ClassMode;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.mockito.BDDMockito.then;

/**
 * Integration tests for using {@link SpyBean @SpyBean} with
 * {@link DirtiesContext @DirtiesContext} and {@link ClassMode#BEFORE_EACH_TEST_METHOD}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(InfraExtension.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class SpyBeanWithDirtiesContextClassModeBeforeMethodIntegrationTests {

  @SpyBean
  private SimpleExampleService exampleService;

  @Autowired
  private ExampleServiceCaller caller;

  @Test
  void testSpying() {
    this.caller.sayGreeting();
    then(this.exampleService).should().greeting();
  }

  @Configuration(proxyBeanMethods = false)
  @Import(ExampleServiceCaller.class)
  static class Config {

  }

}
