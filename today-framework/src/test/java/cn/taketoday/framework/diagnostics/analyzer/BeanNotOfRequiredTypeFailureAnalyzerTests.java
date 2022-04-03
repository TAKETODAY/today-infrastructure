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

package cn.taketoday.framework.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.framework.diagnostics.FailureAnalyzer;
import cn.taketoday.scheduling.annotation.Async;
import cn.taketoday.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link BeanNotOfRequiredTypeFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class BeanNotOfRequiredTypeFailureAnalyzerTests {

  private final FailureAnalyzer analyzer = new BeanNotOfRequiredTypeFailureAnalyzer();

  @Test
  void jdkProxyCausesInjectionFailure() {
    FailureAnalysis analysis = performAnalysis(JdkProxyConfiguration.class);
    assertThat(analysis.getDescription()).startsWith("The bean 'asyncBean'");
    assertThat(analysis.getDescription())
            .containsPattern("The bean is of type '" + AsyncBean.class.getPackage().getName() + ".\\$Proxy.*'");
    assertThat(analysis.getDescription())
            .contains(String.format("and implements:%n\t") + SomeInterface.class.getName());
    assertThat(analysis.getDescription()).contains("Expected a bean of type '" + AsyncBean.class.getName() + "'");
    assertThat(analysis.getDescription())
            .contains(String.format("which implements:%n\t") + SomeInterface.class.getName());
  }

  private FailureAnalysis performAnalysis(Class<?> configuration) {
    FailureAnalysis analysis = this.analyzer.analyze(createFailure(configuration));
    assertThat(analysis).isNotNull();
    return analysis;
  }

  private Exception createFailure(Class<?> configuration) {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configuration)) {
      fail("Expected failure did not occur");
      return null;
    }
    catch (Exception ex) {
      return ex;
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAsync
  @Import(UserConfiguration.class)
  static class JdkProxyConfiguration {

    @Bean
    AsyncBean asyncBean() {
      return new AsyncBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UserConfiguration {

    @Bean
    AsyncBeanUser user(AsyncBean bean) {
      return new AsyncBeanUser(bean);
    }

  }

  static class AsyncBean implements SomeInterface {

    @Async
    void foo() {

    }

    @Override
    public void bar() {

    }

  }

  interface SomeInterface {

    void bar();

  }

  static class AsyncBeanUser {

    AsyncBeanUser(AsyncBean asyncBean) {
    }

  }

}
