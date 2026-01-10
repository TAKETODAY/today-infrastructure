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

import java.util.function.Supplier;

import infra.app.diagnostics.FailureAnalysis;
import infra.beans.factory.support.BeanDefinitionOverrideException;
import infra.context.ApplicationContextInitializer;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionOverrideFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class BeanDefinitionOverrideFailureAnalyzerTests {

  @Test
  void analyzeBeanDefinitionOverrideException() {
    FailureAnalysis analysis = performAnalysis(BeanOverrideConfiguration.class);
    String description = analysis.getDescription();
    assertThat(description).contains("The bean 'testBean', defined in " + SecondConfiguration.class.getName()
            + ", could not be registered.");
    assertThat(description).contains(FirstConfiguration.class.getName());
  }

  @Test
  void analyzeBeanDefinitionOverrideExceptionWithDefinitionsWithNoResourceDescription() {
    FailureAnalysis analysis = performAnalysis((context) -> {
      if (context instanceof AnnotationConfigApplicationContext configContext) {
        configContext.registerBean("testBean", String.class, (Supplier<String>) String::new);
        configContext.registerBean("testBean", String.class, (Supplier<String>) String::new);
      }
    });
    String description = analysis.getDescription();
    assertThat(description)
            .isEqualTo("The bean 'testBean' could not be registered. A bean with that name has already"
                    + " been defined and overriding is disabled.");
  }

  private FailureAnalysis performAnalysis(Class<?> configuration) {
    BeanDefinitionOverrideException failure = createFailure(configuration);
    assertThat(failure).isNotNull();
    return new BeanDefinitionOverrideFailureAnalyzer().analyze(failure);
  }

  private BeanDefinitionOverrideException createFailure(Class<?> configuration) {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      context.setAllowBeanDefinitionOverriding(false);
      context.register(configuration);
      context.refresh();
      context.close();
      return null;
    }
    catch (BeanDefinitionOverrideException ex) {
      ex.printStackTrace();
      return ex;
    }
  }

  private FailureAnalysis performAnalysis(
          ApplicationContextInitializer initializer) {
    BeanDefinitionOverrideException failure = createFailure(initializer);
    assertThat(failure).isNotNull();
    return new BeanDefinitionOverrideFailureAnalyzer().analyze(failure);
  }

  private BeanDefinitionOverrideException createFailure(
          ApplicationContextInitializer initializer) {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      context.setAllowBeanDefinitionOverriding(false);
      initializer.initialize(context);
      context.refresh();
      context.close();
      return null;
    }
    catch (BeanDefinitionOverrideException ex) {
      return ex;
    }
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ FirstConfiguration.class, SecondConfiguration.class })
  static class BeanOverrideConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class FirstConfiguration {

    @Bean
    String testBean() {
      return "test";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SecondConfiguration {

    @Bean
    String testBean() {
      return "test";
    }

  }

}
