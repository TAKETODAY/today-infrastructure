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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import infra.app.diagnostics.FailureAnalysis;
import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.support.AbstractAutowireCapableBeanFactory;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link BeanCurrentlyInCreationFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class BeanCurrentlyInCreationFailureAnalyzerTests {

  private BeanCurrentlyInCreationFailureAnalyzer analyzer = new BeanCurrentlyInCreationFailureAnalyzer(null);

  @Test
  void cyclicBeanMethods() throws IOException {
    FailureAnalysis analysis = performAnalysis(CyclicBeanMethodsConfiguration.class);
    List<String> lines = readDescriptionLines(analysis);
    assertThat(lines).hasSize(9);
    assertThat(lines.get(0))
            .isEqualTo("The dependencies of some of the beans in the application context form a cycle:");
    assertThat(lines.get(1)).isEqualTo("");
    assertThat(lines.get(2)).isEqualTo("┌─────┐");
    assertThat(lines.get(3)).startsWith("|  one defined in " + CyclicBeanMethodsConfiguration.InnerConfiguration.InnerInnerConfiguration.class.getName());
    assertThat(lines.get(4)).isEqualTo("↑     ↓");
    assertThat(lines.get(5)).startsWith("|  two defined in " + CyclicBeanMethodsConfiguration.InnerConfiguration.class.getName());
    assertThat(lines.get(6)).isEqualTo("↑     ↓");
    assertThat(lines.get(7)).startsWith("|  three defined in " + CyclicBeanMethodsConfiguration.class.getName());
    assertThat(lines.get(8)).isEqualTo("└─────┘");
    assertThat(analysis.getAction()).isNotNull();
  }

  @Test
  void cycleWithAutowiredFields() throws IOException {
    FailureAnalysis analysis = performAnalysis(CycleWithAutowiredFields.class);
    assertThat(analysis.getDescription())
            .startsWith("The dependencies of some of the beans in the application context form a cycle:");
    List<String> lines = readDescriptionLines(analysis);
    assertThat(lines).hasSize(9);
    assertThat(lines.get(0))
            .isEqualTo("The dependencies of some of the beans in the application context form a cycle:");
    assertThat(lines.get(1)).isEqualTo("");
    assertThat(lines.get(2)).isEqualTo("┌─────┐");
    assertThat(lines.get(3)).startsWith("|  three defined in " + CycleWithAutowiredFields.BeanThreeConfiguration.class.getName());
    assertThat(lines.get(4)).isEqualTo("↑     ↓");
    assertThat(lines.get(5)).startsWith("|  one defined in " + CycleWithAutowiredFields.class.getName());
    assertThat(lines.get(6)).isEqualTo("↑     ↓");
    assertThat(lines.get(7)).startsWith(
            "|  " + CycleWithAutowiredFields.BeanTwoConfiguration.class.getName() + " (field private " + BeanThree.class.getName());
    assertThat(lines.get(8)).isEqualTo("└─────┘");
    assertThat(analysis.getAction()).isNotNull();
  }

  @Test
  void cycleReferencedViaOtherBeans() throws IOException {
    FailureAnalysis analysis = performAnalysis(CycleReferencedViaOtherBeansConfiguration.class);
    List<String> lines = readDescriptionLines(analysis);
    assertThat(lines).hasSize(12);
    assertThat(lines.get(0))
            .isEqualTo("The dependencies of some of the beans in the application context form a cycle:");
    assertThat(lines.get(1)).isEqualTo("");
    assertThat(lines.get(2)).contains("refererOne (field " + RefererTwo.class.getName());
    assertThat(lines.get(3)).isEqualTo("      ↓");
    assertThat(lines.get(4)).contains("refererTwo (field " + BeanOne.class.getName());
    assertThat(lines.get(5)).isEqualTo("┌─────┐");
    assertThat(lines.get(6))
            .startsWith("|  one defined in " + CycleReferencedViaOtherBeansConfiguration.class.getName());
    assertThat(lines.get(7)).isEqualTo("↑     ↓");
    assertThat(lines.get(8))
            .startsWith("|  two defined in " + CycleReferencedViaOtherBeansConfiguration.class.getName());
    assertThat(lines.get(9)).isEqualTo("↑     ↓");
    assertThat(lines.get(10))
            .startsWith("|  three defined in " + CycleReferencedViaOtherBeansConfiguration.class.getName());
    assertThat(lines.get(11)).isEqualTo("└─────┘");
    assertThat(analysis.getAction()).isNotNull();
  }

  @Test
  void testSelfReferenceCycle() throws IOException {
    FailureAnalysis analysis = performAnalysis(SelfReferenceBeanConfiguration.class);
    List<String> lines = readDescriptionLines(analysis);
    assertThat(lines).hasSize(5);
    assertThat(lines.get(0))
            .isEqualTo("The dependencies of some of the beans in the application context form a cycle:");
    assertThat(lines.get(1)).isEqualTo("");
    assertThat(lines.get(2)).isEqualTo("┌──->──┐");
    assertThat(lines.get(3)).startsWith("|  bean defined in " + SelfReferenceBeanConfiguration.class.getName());
    assertThat(lines.get(4)).isEqualTo("└──<-──┘");
    assertThat(analysis.getAction()).isNotNull();
  }

  @Test
  void cycleWithAnUnknownStartIsNotAnalyzed() {
    assertThat(this.analyzer.analyze(new BeanCurrentlyInCreationException("test"))).isNull();
  }

  @Test
  void cycleWithCircularReferencesAllowed() throws IOException {
    FailureAnalysis analysis = performAnalysis(CyclicBeanMethodsConfiguration.class, true);
    assertThat(analysis.getAction()).contains("Despite circular references being allowed");
  }

  @Test
  void cycleWithCircularReferencesProhibited() throws IOException {
    FailureAnalysis analysis = performAnalysis(CyclicBeanMethodsConfiguration.class, false);
    assertThat(analysis.getAction()).contains("As a last resort");
  }

  private List<String> readDescriptionLines(FailureAnalysis analysis) throws IOException {
    try (BufferedReader reader = new BufferedReader(new StringReader(analysis.getDescription()))) {
      return reader.lines().collect(Collectors.toList());
    }
  }

  private FailureAnalysis performAnalysis(Class<?> configuration) {
    return performAnalysis(configuration, true);
  }

  private FailureAnalysis performAnalysis(Class<?> configuration, boolean allowCircularReferences) {
    Exception failure = createFailure(configuration, allowCircularReferences);
    FailureAnalysis analysis = this.analyzer.analyze(failure);
    assertThat(analysis).isNotNull();
    return analysis;
  }

  private Exception createFailure(Class<?> configuration, boolean allowCircularReferences) {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      context.register(configuration);
      AbstractAutowireCapableBeanFactory beanFactory = context.getBeanFactory();
      this.analyzer = new BeanCurrentlyInCreationFailureAnalyzer(beanFactory);
      beanFactory.setAllowCircularReferences(allowCircularReferences);
      context.refresh();
      fail("Expected failure did not occur");
      context.close();
      return null;
    }
    catch (Exception ex) {
      return ex;
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CyclicBeanMethodsConfiguration {

    @Bean
    BeanThree three(BeanOne one) {
      return new BeanThree();
    }

    @Configuration(proxyBeanMethods = false)
    static class InnerConfiguration {

      @Bean
      BeanTwo two(BeanThree three) {
        return new BeanTwo();
      }

      @Configuration(proxyBeanMethods = false)
      static class InnerInnerConfiguration {

        @Bean
        BeanOne one(BeanTwo two) {
          return new BeanOne();
        }

      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CycleReferencedViaOtherBeansConfiguration {

    @Bean
    BeanOne one(BeanTwo two) {
      return new BeanOne();
    }

    @Bean
    BeanTwo two(BeanThree three) {
      return new BeanTwo();
    }

    @Bean
    BeanThree three(BeanOne beanOne) {
      return new BeanThree();
    }

    @Configuration(proxyBeanMethods = false)
    static class InnerConfiguration {

      @Bean
      RefererTwo refererTwo() {
        return new RefererTwo();
      }

      @Configuration(proxyBeanMethods = false)
      static class InnerInnerConfiguration {

        @Bean
        RefererOne refererOne() {
          return new RefererOne();
        }

      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CycleWithAutowiredFields {

    @Bean
    BeanOne one(BeanTwo two) {
      return new BeanOne();
    }

    @Configuration(proxyBeanMethods = false)
    static class BeanTwoConfiguration {

      @SuppressWarnings("unused")
      @Autowired
      private BeanThree three;

      @Bean
      BeanTwo two() {
        return new BeanTwo();
      }

    }

    @Configuration(proxyBeanMethods = false)
    static class BeanThreeConfiguration {

      @Bean
      BeanThree three(BeanOne one) {
        return new BeanThree();
      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SelfReferenceBeanConfiguration {

    @Bean
    SelfReferenceBean bean(SelfReferenceBean bean) {
      return new SelfReferenceBean();
    }

  }

  static class RefererOne {

    @Autowired
    RefererTwo refererTwo;

  }

  static class RefererTwo {

    @Autowired
    BeanOne beanOne;

  }

  static class BeanOne {

  }

  static class BeanTwo {

  }

  static class BeanThree {

  }

  static class SelfReferenceBean {

    @Autowired
    SelfReferenceBean bean;

  }

}
