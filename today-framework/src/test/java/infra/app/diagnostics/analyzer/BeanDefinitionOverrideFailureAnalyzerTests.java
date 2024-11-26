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
