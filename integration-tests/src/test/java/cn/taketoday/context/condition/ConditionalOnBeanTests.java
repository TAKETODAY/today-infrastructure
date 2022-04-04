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

package cn.taketoday.context.condition;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Date;
import java.util.function.Consumer;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.ImportResource;
import cn.taketoday.context.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.scheduling.annotation.EnableScheduling;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnBean @ConditionalOnBean}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class ConditionalOnBeanTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void testNameOnBeanCondition() {
    this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameConfiguration.class)
            .run(this::hasBarBean);
  }

  @Test
  void testNameAndTypeOnBeanCondition() {
    this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameAndTypeConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("bar"));
  }

  @Test
  void testNameOnBeanConditionReverseOrder() {
    // Ideally this should be true
    this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class, FooConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("bar"));
  }

  @Test
  void testClassOnBeanCondition() {
    this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanClassConfiguration.class)
            .run(this::hasBarBean);
  }

  @Test
  void testClassOnBeanClassNameCondition() {
    this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanClassNameConfiguration.class)
            .run(this::hasBarBean);
  }

  @Test
  void testOnBeanConditionWithXml() {
    this.contextRunner.withUserConfiguration(XmlConfiguration.class, OnBeanNameConfiguration.class)
            .run(this::hasBarBean);
  }

  @Test
  void testOnBeanConditionWithCombinedXml() {
    // Ideally this should be true
    this.contextRunner.withUserConfiguration(CombinedXmlConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("bar"));
  }

  @Test
  void testAnnotationOnBeanCondition() {
    this.contextRunner.withUserConfiguration(FooConfiguration.class, OnAnnotationConfiguration.class)
            .run(this::hasBarBean);
  }

  @Test
  void testOnMissingBeanType() {
    this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanMissingClassConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("bar"));
  }

  @Test
  void withPropertyPlaceholderClassName() {
    this.contextRunner
            .withUserConfiguration(PropertySourcesPlaceholderConfigurer.class,
                    WithPropertyPlaceholderClassName.class, OnBeanClassConfiguration.class)
            .withPropertyValues("mybeanclass=java.lang.String")
            .run((context) -> assertThat(context).hasNotFailed());
  }

  @Test
  void beanProducedByFactoryBeanIsConsideredWhenMatchingOnAnnotation() {
    this.contextRunner
            .withUserConfiguration(FactoryBeanConfiguration.class, OnAnnotationWithFactoryBeanConfiguration.class)
            .run((context) -> {
              assertThat(context).hasBean("bar");
              assertThat(context).hasSingleBean(ExampleBean.class);
            });
  }

  private void hasBarBean(AssertableApplicationContext context) {
    assertThat(context).hasBean("bar");
    assertThat(context.getBean("bar")).isEqualTo("bar");
  }

  @Test
  void onBeanConditionOutputShouldNotContainConditionalOnMissingBeanClassInMessage() {
    this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class).run((context) -> {
      Collection<ConditionAndOutcomes> conditionAndOutcomes = ConditionEvaluationReport
              .get(context.getSourceApplicationContext().getBeanFactory()).getConditionAndOutcomesBySource()
              .values();
      String message = conditionAndOutcomes.iterator().next().iterator().next().getOutcome().getMessage();
      assertThat(message).doesNotContain("@ConditionalOnMissingBean");
    });
  }

  @Test
  void conditionEvaluationConsidersChangeInTypeWhenBeanIsOverridden() {
    this.contextRunner.withAllowBeanDefinitionOverriding(true).withUserConfiguration(OriginalDefinition.class,
            OverridingDefinition.class, ConsumingConfiguration.class).run((context) -> {
      assertThat(context).hasBean("testBean");
      assertThat(context).hasSingleBean(Integer.class);
      assertThat(context).doesNotHaveBean(ConsumingConfiguration.class);
    });
  }

  @Test
  void parameterizedContainerWhenValueIsOfMissingBeanDoesNotMatch() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithoutCustomConfig.class,
                    ParameterizedConditionWithValueConfig.class)
            .run((context) -> assertThat(context).satisfies(exampleBeanRequirement("otherExampleBean")));
  }

  @Test
  void parameterizedContainerWhenValueIsOfExistingBeanMatches() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomConfig.class, ParameterizedConditionWithValueConfig.class)
            .run((context) -> assertThat(context)
                    .satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
  }

  @Test
  void parameterizedContainerWhenValueIsOfMissingBeanRegistrationDoesNotMatch() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithoutCustomContainerConfig.class,
                    ParameterizedConditionWithValueConfig.class)
            .run((context) -> assertThat(context).satisfies(exampleBeanRequirement("otherExampleBean")));
  }

  @Test
  void parameterizedContainerWhenValueIsOfExistingBeanRegistrationMatches() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
                    ParameterizedConditionWithValueConfig.class)
            .run((context) -> assertThat(context)
                    .satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
  }

  @Test
  void parameterizedContainerWhenReturnTypeIsOfExistingBeanMatches() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomConfig.class,
                    ParameterizedConditionWithReturnTypeConfig.class)
            .run((context) -> assertThat(context)
                    .satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
  }

  @Test
  void parameterizedContainerWhenReturnTypeIsOfExistingBeanRegistrationMatches() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
                    ParameterizedConditionWithReturnTypeConfig.class)
            .run((context) -> assertThat(context)
                    .satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
  }

  @Test
  void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanMatches() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomConfig.class,
                    ParameterizedConditionWithReturnRegistrationTypeConfig.class)
            .run((context) -> assertThat(context)
                    .satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
  }

  @Test
  void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanRegistrationMatches() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
                    ParameterizedConditionWithReturnRegistrationTypeConfig.class)
            .run((context) -> assertThat(context)
                    .satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
  }

  private Consumer<ConfigurableApplicationContext> exampleBeanRequirement(String... names) {
    return (context) -> {
      String[] beans = StringUtils.toStringArray(context.getBeanNamesForType(ExampleBean.class));
      String[] containers = StringUtils.toStringArray(context.getBeanNamesForType(TestParameterizedContainer.class));
      assertThat(StringUtils.concatenateStringArrays(beans, containers)).containsOnly(names);
    };
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(name = "foo")
  static class OnBeanNameConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(name = "foo", value = Date.class)
  static class OnBeanNameAndTypeConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(annotation = EnableScheduling.class)
  static class OnAnnotationConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(String.class)
  static class OnBeanClassConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(type = "java.lang.String")
  static class OnBeanClassNameConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(type = "some.type.Missing")
  static class OnBeanMissingClassConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class FooConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ImportResource("cn/taketoday/framework/autoconfigure/condition/foo.xml")
  static class XmlConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @ImportResource("cn/taketoday/framework/autoconfigure/condition/foo.xml")
  @Import(OnBeanNameConfiguration.class)
  static class CombinedXmlConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @Import(WithPropertyPlaceholderClassNameRegistrar.class)
  static class WithPropertyPlaceholderClassName {

  }

  @Configuration(proxyBeanMethods = false)
  static class FactoryBeanConfiguration {

    @Bean
    ExampleFactoryBean exampleBeanFactoryBean() {
      return new ExampleFactoryBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(annotation = TestAnnotation.class)
  static class OnAnnotationWithFactoryBeanConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  static class WithPropertyPlaceholderClassNameRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
      RootBeanDefinition bd = new RootBeanDefinition();
      bd.setBeanClassName("${mybeanclass}");
      context.registerBeanDefinition("mybean", bd);
    }

  }

  static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

    @Override
    public ExampleBean getObject() {
      return new ExampleBean("fromFactory");
    }

    @Override
    public Class<?> getObjectType() {
      return ExampleBean.class;
    }

    @Override
    public boolean isSingleton() {
      return false;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class OriginalDefinition {

    @Bean
    String testBean() {
      return "test";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(String.class)
  static class OverridingDefinition {

    @Bean
    Integer testBean() {
      return 1;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(String.class)
  static class ConsumingConfiguration {

    ConsumingConfiguration(String testBean) {
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParameterizedWithCustomConfig {

    @Bean
    CustomExampleBean customExampleBean() {
      return new CustomExampleBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParameterizedWithoutCustomConfig {

    @Bean
    OtherExampleBean otherExampleBean() {
      return new OtherExampleBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParameterizedWithoutCustomContainerConfig {

    @Bean
    TestParameterizedContainer<OtherExampleBean> otherExampleBean() {
      return new TestParameterizedContainer<>();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParameterizedWithCustomContainerConfig {

    @Bean
    TestParameterizedContainer<CustomExampleBean> customExampleBean() {
      return new TestParameterizedContainer<>();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParameterizedConditionWithValueConfig {

    @Bean
    @ConditionalOnBean(value = CustomExampleBean.class, parameterizedContainer = TestParameterizedContainer.class)
    CustomExampleBean conditionalCustomExampleBean() {
      return new CustomExampleBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParameterizedConditionWithReturnTypeConfig {

    @Bean
    @ConditionalOnBean(parameterizedContainer = TestParameterizedContainer.class)
    CustomExampleBean conditionalCustomExampleBean() {
      return new CustomExampleBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParameterizedConditionWithReturnRegistrationTypeConfig {

    @Bean
    @ConditionalOnBean(parameterizedContainer = TestParameterizedContainer.class)
    TestParameterizedContainer<CustomExampleBean> conditionalCustomExampleBean() {
      return new TestParameterizedContainer<>();
    }

  }

  @TestAnnotation
  static class ExampleBean {

    private String value;

    ExampleBean(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }

  }

  static class CustomExampleBean extends ExampleBean {

    CustomExampleBean() {
      super("custom subclass");
    }

  }

  static class OtherExampleBean extends ExampleBean {

    OtherExampleBean() {
      super("other subclass");
    }

  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface TestAnnotation {

  }

}
