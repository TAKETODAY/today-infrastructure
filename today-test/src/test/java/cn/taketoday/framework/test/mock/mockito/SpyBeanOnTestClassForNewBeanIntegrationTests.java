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
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.test.mock.mockito.example.ExampleServiceCaller;
import cn.taketoday.framework.test.mock.mockito.example.SimpleExampleService;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Test {@link SpyBean @SpyBean} on a test class can be used to inject new spy instances.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@SpyBean(SimpleExampleService.class)
class SpyBeanOnTestClassForNewBeanIntegrationTests {

  @Autowired
  private ExampleServiceCaller caller;

  @Test
  void testSpying() {
    assertThat(this.caller.sayGreeting()).isEqualTo("I say simple");
    then(this.caller.getService()).should().greeting();
  }

  @Configuration(proxyBeanMethods = false)
  @Import(ExampleServiceCaller.class)
  static class Config {

  }

}
