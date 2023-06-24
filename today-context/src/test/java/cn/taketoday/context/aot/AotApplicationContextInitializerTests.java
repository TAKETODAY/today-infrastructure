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

package cn.taketoday.context.aot;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AotApplicationContextInitializer}.
 *
 * @author Stephane Nicoll
 */
class AotApplicationContextInitializerTests {

  @Test
  void initializeInvokesApplicationContextInitializer() {
    GenericApplicationContext context = new GenericApplicationContext();
    AotApplicationContextInitializer.forInitializerClasses(
                    TestApplicationContextInitializer.class.getName())
            .initialize(context);
    assertThat(context.getBeanDefinitionNames()).containsExactly("test");
  }

  @Test
  void initializeInvokesApplicationContextInitializersInOrder() {
    GenericApplicationContext context = new GenericApplicationContext();
    AotApplicationContextInitializer.forInitializerClasses(
                    AnotherApplicationContextInitializer.class.getName(),
                    TestApplicationContextInitializer.class.getName())
            .initialize(context);
    assertThat(context.getBeanDefinitionNames()).containsExactly("another", "test");
  }

  @Test
  void initializeWhenClassIsNotApplicationContextInitializerThrowsException() {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> AotApplicationContextInitializer.forInitializerClasses("java.lang.String")
                      .initialize(context))
              .withMessageContaining("not assignable")
              .withMessageContaining("ApplicationContextInitializer")
              .withMessageContaining("java.lang.String");
    }
  }

  @Test
  void initializeWhenInitializerHasNoDefaultConstructorThrowsException() {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> AotApplicationContextInitializer.forInitializerClasses(
                      ConfigurableApplicationContextInitializer.class.getName()).initialize(context))
              .withMessageContaining("Failed to instantiate ApplicationContextInitializer: ")
              .withMessageContaining(ConfigurableApplicationContextInitializer.class.getName());
    }
  }

  static class TestApplicationContextInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      applicationContext.unwrap(GenericApplicationContext.class)
              .registerBeanDefinition("test", new RootBeanDefinition());
    }

  }

  static class AnotherApplicationContextInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      applicationContext.unwrap(GenericApplicationContext.class)
              .registerBeanDefinition("another", new RootBeanDefinition());
    }

  }

  static class ConfigurableApplicationContextInitializer implements ApplicationContextInitializer {

    @SuppressWarnings("unused")
    public ConfigurableApplicationContextInitializer(ClassLoader classLoader) {
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

    }
  }

}
