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

package infra.app.context.config;

import org.junit.jupiter.api.Test;

import infra.app.context.config.DelegatingApplicationContextInitializer;
import infra.context.ApplicationContextException;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.StaticApplicationContext;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DelegatingApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
class DelegatingApplicationContextInitializerTests {

  private final DelegatingApplicationContextInitializer initializer = new DelegatingApplicationContextInitializer();

  @Test
  void orderedInitialize() {
    StaticApplicationContext context = new StaticApplicationContext();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
            "context.initializer.classes=" + MockInitB.class.getName() + "," + MockInitA.class.getName());
    this.initializer.initialize(context);
    assertThat(context.getBeanFactory().getSingleton("a")).isEqualTo("a");
    assertThat(context.getBeanFactory().getSingleton("b")).isEqualTo("b");
  }

  @Test
  void noInitializers() {
    StaticApplicationContext context = new StaticApplicationContext();
    this.initializer.initialize(context);
  }

  @Test
  void emptyInitializers() {
    StaticApplicationContext context = new StaticApplicationContext();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "context.initializer.classes:");
    this.initializer.initialize(context);
  }

  @Test
  void noSuchInitializerClass() {
    StaticApplicationContext context = new StaticApplicationContext();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
            "context.initializer.classes=missing.madeup.class");
    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(() -> this.initializer.initialize(context));
  }

  @Test
  void notAnInitializerClass() {
    StaticApplicationContext context = new StaticApplicationContext();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
            "context.initializer.classes=" + Object.class.getName());
    assertThatIllegalArgumentException().isThrownBy(() -> this.initializer.initialize(context));
  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class MockInitA implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      applicationContext.getBeanFactory().registerSingleton("a", "a");
    }

  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  static class MockInitB implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      assertThat(applicationContext.getBeanFactory().getSingleton("a")).isEqualTo("a");
      applicationContext.getBeanFactory().registerSingleton("b", "b");
    }

  }

  static class NotSuitableInit implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
    }

  }

}
