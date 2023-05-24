/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Bean @Bean} 'lite' mode features that are not covered
 * elsewhere in the test suite.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurationClassPostProcessorTests
 */
class BeanLiteModeTests {

  @Test
  void beanMethodsAreFoundWhenInheritedAsInterfaceDefaultMethods() {
    assertBeansAreFound(InterfaceDefaultMethodsConfig.class);
  }

  @Test
  void beanMethodsAreFoundWhenDeclaredLocally() {
    assertBeansAreFound(BaseConfig.class);
  }

  @Test
  void beanMethodsAreFoundWhenDeclaredLocallyAndInSuperclass() {
    assertBeansAreFound(OverridingConfig.class, "foo", "xyz");
  }

  @Test
    // gh-30449
  void beanMethodsAreFoundWhenDeclaredOnlyInSuperclass() {
    assertBeansAreFound(ExtendedConfig.class, "foo", "xyz");
  }

  private static void assertBeansAreFound(Class<?> configClass) {
    assertBeansAreFound(configClass, "foo", "bar");
  }

  private static void assertBeansAreFound(Class<?> configClass, String expected1, String expected2) {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass)) {
      String bean1 = context.getBean("bean1", String.class);
      String bean2 = context.getBean("bean2", String.class);

      assertThat(bean1).isEqualTo(expected1);
      assertThat(bean2).isEqualTo(expected2);
    }
  }

  interface ConfigInterface {

    @Bean
    default String bean1() {
      return "foo";
    }

    @Bean
    default String bean2() {
      return "bar";
    }
  }

  static class InterfaceDefaultMethodsConfig implements ConfigInterface {
  }

  static class BaseConfig {

    @Bean
    String bean1() {
      return "foo";
    }

    @Bean
    String bean2() {
      return "bar";
    }
  }

  static class OverridingConfig extends BaseConfig {

    @Bean
    @Override
    String bean2() {
      return "xyz";
    }
  }

  static class ExtendedConfig extends OverridingConfig {
  }

}
