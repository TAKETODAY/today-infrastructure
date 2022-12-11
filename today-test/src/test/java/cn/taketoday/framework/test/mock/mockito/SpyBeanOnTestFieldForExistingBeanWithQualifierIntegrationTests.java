/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.test.mock.mockito.example.CustomQualifier;
import cn.taketoday.framework.test.mock.mockito.example.CustomQualifierExampleService;
import cn.taketoday.framework.test.mock.mockito.example.ExampleService;
import cn.taketoday.framework.test.mock.mockito.example.ExampleServiceCaller;
import cn.taketoday.framework.test.mock.mockito.example.RealExampleService;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Test {@link SpyBean @SpyBean} on a test class field can be used to replace existing
 * bean while preserving qualifiers.
 *
 * @author Andreas Neiser
 */
@ExtendWith(InfraExtension.class)
class SpyBeanOnTestFieldForExistingBeanWithQualifierIntegrationTests {

  @SpyBean
  @CustomQualifier
  private ExampleService service;

  @Autowired
  private ExampleServiceCaller caller;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void testMocking() {
    this.caller.sayGreeting();
    then(this.service).should().greeting();
  }

  @Test
  void onlyQualifiedBeanIsReplaced() {
    assertThat(this.applicationContext.getBean("service")).isSameAs(this.service);
    ExampleService anotherService = this.applicationContext.getBean("anotherService", ExampleService.class);
    assertThat(anotherService.greeting()).isEqualTo("Another");
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    CustomQualifierExampleService service() {
      return new CustomQualifierExampleService();
    }

    @Bean
    ExampleService anotherService() {
      return new RealExampleService("Another");
    }

    @Bean
    ExampleServiceCaller controller(@CustomQualifier ExampleService service) {
      return new ExampleServiceCaller(service);
    }

  }

}
