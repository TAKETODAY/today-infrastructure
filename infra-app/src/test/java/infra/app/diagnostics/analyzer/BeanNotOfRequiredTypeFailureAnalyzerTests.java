/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import infra.app.diagnostics.FailureAnalysis;
import infra.app.diagnostics.FailureAnalyzer;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.scheduling.annotation.Async;
import infra.scheduling.annotation.EnableAsync;

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
    try {
      ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configuration);
      fail("Expected failure did not occur");
      context.close();
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
