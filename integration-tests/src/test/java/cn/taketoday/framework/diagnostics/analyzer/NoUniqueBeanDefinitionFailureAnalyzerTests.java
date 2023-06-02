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

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ImportResource;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.framework.diagnostics.analyzer.nounique.TestBean;
import cn.taketoday.framework.diagnostics.analyzer.nounique.TestBeanConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoUniqueBeanDefinitionFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class NoUniqueBeanDefinitionFailureAnalyzerTests {

  private NoUniqueBeanDefinitionFailureAnalyzer analyzer;

  @Test
  void failureAnalysisForFieldConsumer() {
    FailureAnalysis failureAnalysis = analyzeFailure(createFailure(FieldConsumer.class));
    assertThat(failureAnalysis.getDescription()).startsWith(
            "Field testBean in " + FieldConsumer.class.getName() + " required a single bean, but 6 were found:");
    assertFoundBeans(failureAnalysis);
  }

  @Test
  void failureAnalysisForMethodConsumer() {
    FailureAnalysis failureAnalysis = analyzeFailure(createFailure(MethodConsumer.class));
    assertThat(failureAnalysis.getDescription()).startsWith("Parameter 0 of method consumer in "
            + MethodConsumer.class.getName() + " required a single bean, but 6 were found:");
    assertFoundBeans(failureAnalysis);
  }

  @Test
  void failureAnalysisForConstructorConsumer() {
    FailureAnalysis failureAnalysis = analyzeFailure(createFailure(ConstructorConsumer.class));
    assertThat(failureAnalysis.getDescription()).startsWith("Parameter 0 of constructor in "
            + ConstructorConsumer.class.getName() + " required a single bean, but 6 were found:");
    assertFoundBeans(failureAnalysis);
  }

  @Test
  void failureAnalysisForObjectProviderMethodConsumer() {
    FailureAnalysis failureAnalysis = analyzeFailure(createFailure(ObjectProviderMethodConsumer.class));
    assertThat(failureAnalysis.getDescription()).startsWith("Method consumer in "
            + ObjectProviderMethodConsumer.class.getName() + " required a single bean, but 6 were found:");
    assertFoundBeans(failureAnalysis);
  }

  @Test
  void failureAnalysisForXmlConsumer() {
    FailureAnalysis failureAnalysis = analyzeFailure(createFailure(XmlConsumer.class));
    assertThat(failureAnalysis.getDescription()).startsWith("Parameter 0 of constructor in "
            + TestBeanConsumer.class.getName() + " required a single bean, but 6 were found:");
    assertFoundBeans(failureAnalysis);
  }

  @Test
  void failureAnalysisForObjectProviderConstructorConsumer() {
    FailureAnalysis failureAnalysis = analyzeFailure(createFailure(ObjectProviderConstructorConsumer.class));
    assertThat(failureAnalysis.getDescription()).startsWith("Constructor in "
            + ObjectProviderConstructorConsumer.class.getName() + " required a single bean, but 6 were found:");
    assertFoundBeans(failureAnalysis);
  }

  private BeanCreationException createFailure(Class<?> consumer) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(DuplicateBeansProducer.class, consumer);
    context.setParent(new AnnotationConfigApplicationContext(ParentProducer.class));
    try {
      context.refresh();
    }
    catch (BeanCreationException ex) {
      analyzer = new NoUniqueBeanDefinitionFailureAnalyzer(context.getBeanFactory());
      return ex;
    }
    context.close();
    return null;
  }

  private FailureAnalysis analyzeFailure(BeanCreationException failure) {
    return this.analyzer.analyze(failure);
  }

  private void assertFoundBeans(FailureAnalysis analysis) {
    assertThat(analysis.getDescription())
            .contains("beanOne: defined by method 'beanOne' in " + DuplicateBeansProducer.class.getName());
    assertThat(analysis.getDescription())
            .contains("beanTwo: defined by method 'beanTwo' in " + DuplicateBeansProducer.class.getName());
    assertThat(analysis.getDescription())
            .contains("beanThree: defined by method 'beanThree' in " + ParentProducer.class.getName());
    assertThat(analysis.getDescription()).contains("barTestBean");
    assertThat(analysis.getDescription()).contains("fooTestBean");
    assertThat(analysis.getDescription()).contains("xmlTestBean");
  }

  @Configuration(proxyBeanMethods = false)
  @ComponentScan(basePackageClasses = TestBean.class)
  @ImportResource("/cn/taketoday/framework/diagnostics/analyzer/nounique/producer.xml")
  static class DuplicateBeansProducer {

    @Bean
    TestBean beanOne() {
      return new TestBean();
    }

    @Bean
    TestBean beanTwo() {
      return new TestBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParentProducer {

    @Bean
    TestBean beanThree() {
      return new TestBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class FieldConsumer {

    @SuppressWarnings("unused")
    @Autowired
    private TestBean testBean;

  }

  @Configuration(proxyBeanMethods = false)
  static class ObjectProviderConstructorConsumer {

    ObjectProviderConstructorConsumer(ObjectProvider<TestBean> objectProvider) {
      objectProvider.getIfAvailable();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ConstructorConsumer {

    ConstructorConsumer(TestBean testBean) {

    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MethodConsumer {

    @Bean
    String consumer(TestBean testBean) {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ObjectProviderMethodConsumer {

    @Bean
    String consumer(ObjectProvider<TestBean> testBeanProvider) {
      testBeanProvider.getIfAvailable();
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ImportResource("/cn/taketoday/framework/diagnostics/analyzer/nounique/consumer.xml")
  static class XmlConsumer {

  }

}
