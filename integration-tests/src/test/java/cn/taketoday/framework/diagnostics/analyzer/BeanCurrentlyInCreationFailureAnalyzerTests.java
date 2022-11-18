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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.BeanCurrentlyInCreationException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.support.AbstractAutowireCapableBeanFactory;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.diagnostics.FailureAnalysis;

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
