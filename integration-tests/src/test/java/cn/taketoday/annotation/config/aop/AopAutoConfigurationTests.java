/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.EnableAspectJAutoProxy;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AopAutoConfiguration}.
 *
 * @author Eberhard Wolff
 * @author Stephane Nicoll
 */
class AopAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class));

  @Test
  void aopDisabled() {
    this.contextRunner.withUserConfiguration(TestConfiguration.class).withPropertyValues("infra.aop.auto:false")
            .run((context) -> {
              TestAspect aspect = context.getBean(TestAspect.class);
              assertThat(aspect.isCalled()).isFalse();
              TestBean bean = context.getBean(TestBean.class);
              bean.foo();
              assertThat(aspect.isCalled()).isFalse();
            });
  }

  @Test
  void aopWithDefaultSettings() {
    this.contextRunner.withUserConfiguration(TestConfiguration.class).run(proxyTargetClassEnabled());
  }

  @Test
  void aopWithEnabledProxyTargetClass() {
    this.contextRunner.withUserConfiguration(TestConfiguration.class)
            .withPropertyValues("infra.aop.proxy-target-class:true").run(proxyTargetClassEnabled());
  }

  @Test
  void aopWithDisabledProxyTargetClass() {
    this.contextRunner.withUserConfiguration(TestConfiguration.class)
            .withPropertyValues("infra.aop.proxy-target-class:false").run(proxyTargetClassDisabled());
  }

  @Test
  void customConfigurationWithProxyTargetClassDefaultDoesNotDisableProxying() {
    this.contextRunner.withUserConfiguration(CustomTestConfiguration.class).run(proxyTargetClassEnabled());

  }

  private ContextConsumer<AssertableApplicationContext> proxyTargetClassEnabled() {
    return (context) -> {
      TestAspect aspect = context.getBean(TestAspect.class);
      assertThat(aspect.isCalled()).isFalse();
      TestBean bean = context.getBean(TestBean.class);
      bean.foo();
      assertThat(aspect.isCalled()).isTrue();
    };
  }

  private ContextConsumer<AssertableApplicationContext> proxyTargetClassDisabled() {
    return (context) -> {
      TestAspect aspect = context.getBean(TestAspect.class);
      assertThat(aspect.isCalled()).isFalse();
      TestInterface bean = context.getBean(TestInterface.class);
      bean.foo();
      assertThat(aspect.isCalled()).isTrue();
      assertThat(context).doesNotHaveBean(TestBean.class);
    };
  }

  @EnableAspectJAutoProxy
  @Configuration(proxyBeanMethods = false)
  @Import(TestConfiguration.class)
  static class CustomTestConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {

    @Bean
    TestAspect aspect() {
      return new TestAspect();
    }

    @Bean
    TestInterface bean() {
      return new TestBean();
    }

  }

  static class TestBean implements TestInterface {

    @Override
    public void foo() {
    }

  }

  @Aspect
  static class TestAspect {

    private boolean called;

    boolean isCalled() {
      return this.called;
    }

    @Before("execution(* foo(..))")
    void before() {
      this.called = true;
    }

  }

  interface TestInterface {

    void foo();

  }

}
