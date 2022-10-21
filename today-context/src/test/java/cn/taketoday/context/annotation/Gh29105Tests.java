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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for gh-29105.
 *
 * @author Stephane Nicoll
 */
public class Gh29105Tests {

  @Test
  void beanProviderWithParentContextReuseOrder() {
    AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
    parent.register(DefaultConfiguration.class);
    parent.register(CustomConfiguration.class);
    parent.refresh();

    AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
    child.setParent(parent);
    child.register(DefaultConfiguration.class);
    child.refresh();

    List<Class<?>> orderedTypes = child.getBeanProvider(MyService.class)
            .orderedStream().map(Object::getClass).collect(Collectors.toList());
    assertThat(orderedTypes).containsExactly(CustomService.class, DefaultService.class);
  }

  interface MyService { }

  static class CustomService implements MyService { }

  static class DefaultService implements MyService { }

  @Configuration
  static class CustomConfiguration {

    @Bean
    @Order(-1)
    CustomService customService() {
      return new CustomService();
    }

  }

  @Configuration
  static class DefaultConfiguration {

    @Bean
    @Order(0)
    DefaultService defaultService() {
      return new DefaultService();
    }

  }
}
