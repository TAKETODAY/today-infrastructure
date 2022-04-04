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
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.ComponentScan.Filter;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.FilterType;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.ImportResource;
import cn.taketoday.context.condition.scan.ScanBean;
import cn.taketoday.context.condition.scan.ScannedFactoryBeanConfiguration;
import cn.taketoday.context.condition.scan.ScannedFactoryBeanWithBeanMethodArgumentsConfiguration;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.framework.annotation.PropertyPlaceholderAutoConfiguration;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.lang.Assert;
import cn.taketoday.scheduling.annotation.EnableScheduling;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnMissingBean @ConditionalOnMissingBean}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Jakub Kubrynski
 * @author Andy Wilkinson
 */
@SuppressWarnings("resource")
class ConditionalOnMissingBeanTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void testNameOnMissingBeanCondition() {
    this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameConfiguration.class)
            .run((context) -> {
              assertThat(context).doesNotHaveBean("bar");
              assertThat(context.getBean("foo")).isEqualTo("foo");
            });
  }

  @Test
  void testNameOnMissingBeanConditionReverseOrder() {
    this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class, FooConfiguration.class)
            .run((context) -> {
              // Ideally this would be doesNotHaveBean, but the ordering is a
              // problem
              assertThat(context).hasBean("bar");
              assertThat(context.getBean("foo")).isEqualTo("foo");
            });
  }

  @Test
  void testNameAndTypeOnMissingBeanCondition() {
    // Arguably this should be hasBean, but as things are implemented the conditions
    // specified in the different attributes of @ConditionalOnBean are combined with
    // logical OR (not AND) so if any of them match the condition is true.
    this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameAndTypeConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("bar"));
  }

  @Test
  void hierarchyConsidered() {
    this.contextRunner.withUserConfiguration(FooConfiguration.class)
            .run((parent) -> new ApplicationContextRunner().withParent(parent)
                    .withUserConfiguration(HierarchyConsidered.class)
                    .run((context) -> assertThat(context.containsLocalBean("bar")).isFalse()));
  }

  @Test
  void hierarchyNotConsidered() {
    this.contextRunner.withUserConfiguration(FooConfiguration.class)
            .run((parent) -> new ApplicationContextRunner().withParent(parent)
                    .withUserConfiguration(HierarchyNotConsidered.class)
                    .run((context) -> assertThat(context.containsLocalBean("bar")).isTrue()));
  }

  @Test
  void impliedOnBeanMethod() {
    this.contextRunner.withUserConfiguration(ExampleBeanConfiguration.class, ImpliedOnBeanMethod.class)
            .run((context) -> assertThat(context).hasSingleBean(ExampleBean.class));
  }

  @Test
  void testAnnotationOnMissingBeanCondition() {
    this.contextRunner.withUserConfiguration(FooConfiguration.class, OnAnnotationConfiguration.class)
            .run((context) -> {
              assertThat(context).doesNotHaveBean("bar");
              assertThat(context.getBean("foo")).isEqualTo("foo");
            });
  }

  @Test
  void testAnnotationOnMissingBeanConditionWithEagerFactoryBean() {
    // Rigorous test for SPR-11069
    this.contextRunner
            .withUserConfiguration(FooConfiguration.class, OnAnnotationConfiguration.class,
                    FactoryBeanXmlConfiguration.class, PropertyPlaceholderAutoConfiguration.class)
            .run((context) -> {
              assertThat(context).doesNotHaveBean("bar");
              assertThat(context).hasBean("example");
              assertThat(context.getBean("foo")).isEqualTo("foo");
            });
  }

  @Test
  void testOnMissingBeanConditionOutputShouldNotContainConditionalOnBeanClassInMessage() {
    this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class).run((context) -> {
      Collection<ConditionEvaluationReport.ConditionAndOutcomes> conditionAndOutcomes = ConditionEvaluationReport
              .get(context.getSourceApplicationContext().getBeanFactory()).getConditionAndOutcomesBySource()
              .values();
      String message = conditionAndOutcomes.iterator().next().iterator().next().getOutcome().getMessage();
      assertThat(message).doesNotContain("@ConditionalOnBean");
    });
  }

  @Test
  void testOnMissingBeanConditionWithFactoryBean() {
    this.contextRunner
            .withUserConfiguration(FactoryBeanConfiguration.class, ConditionalOnFactoryBean.class,
                    PropertyPlaceholderAutoConfiguration.class)
            .run((context) -> assertThat(context.getBean(ExampleBean.class).toString()).isEqualTo("fromFactory"));
  }

  @Test
  void testOnMissingBeanConditionWithComponentScannedFactoryBean() {
    this.contextRunner
            .withUserConfiguration(ComponentScannedFactoryBeanBeanMethodConfiguration.class,
                    ConditionalOnFactoryBean.class, PropertyPlaceholderAutoConfiguration.class)
            .run((context) -> assertThat(context.getBean(ScanBean.class).toString()).isEqualTo("fromFactory"));
  }

  @Test
  void testOnMissingBeanConditionWithComponentScannedFactoryBeanWithBeanMethodArguments() {
    this.contextRunner
            .withUserConfiguration(ComponentScannedFactoryBeanBeanMethodWithArgumentsConfiguration.class,
                    ConditionalOnFactoryBean.class, PropertyPlaceholderAutoConfiguration.class)
            .run((context) -> assertThat(context.getBean(ScanBean.class).toString()).isEqualTo("fromFactory"));
  }

  @Test
  void testOnMissingBeanConditionWithFactoryBeanWithBeanMethodArguments() {
    this.contextRunner
            .withUserConfiguration(FactoryBeanWithBeanMethodArgumentsConfiguration.class,
                    ConditionalOnFactoryBean.class, PropertyPlaceholderAutoConfiguration.class)
            .withPropertyValues("theValue=foo")
            .run((context) -> assertThat(context.getBean(ExampleBean.class).toString()).isEqualTo("fromFactory"));
  }

  @Test
  void testOnMissingBeanConditionWithConcreteFactoryBean() {
    this.contextRunner
            .withUserConfiguration(ConcreteFactoryBeanConfiguration.class, ConditionalOnFactoryBean.class,
                    PropertyPlaceholderAutoConfiguration.class)
            .run((context) -> assertThat(context.getBean(ExampleBean.class).toString()).isEqualTo("fromFactory"));
  }

  @Test
  void testOnMissingBeanConditionWithUnhelpfulFactoryBean() {
    // We could not tell that the FactoryBean would ultimately create an ExampleBean
    this.contextRunner
            .withUserConfiguration(UnhelpfulFactoryBeanConfiguration.class, ConditionalOnFactoryBean.class,
                    PropertyPlaceholderAutoConfiguration.class)
            .run((context) -> assertThat(context).getBeans(ExampleBean.class).hasSize(2));
  }

  @Test
  void testOnMissingBeanConditionWithRegisteredFactoryBean() {
    this.contextRunner
            .withUserConfiguration(RegisteredFactoryBeanConfiguration.class, ConditionalOnFactoryBean.class,
                    PropertyPlaceholderAutoConfiguration.class)
            .run((context) -> assertThat(context.getBean(ExampleBean.class).toString()).isEqualTo("fromFactory"));
  }

  @Test
  void testOnMissingBeanConditionWithNonspecificFactoryBeanWithClassAttribute() {
    this.contextRunner
            .withUserConfiguration(NonspecificFactoryBeanClassAttributeConfiguration.class,
                    ConditionalOnFactoryBean.class, PropertyPlaceholderAutoConfiguration.class)
            .run((context) -> assertThat(context.getBean(ExampleBean.class).toString()).isEqualTo("fromFactory"));
  }

  @Test
  void testOnMissingBeanConditionWithNonspecificFactoryBeanWithStringAttribute() {
    contextRunner.withUserConfiguration(NonspecificFactoryBeanStringAttributeConfiguration.class,
                    ConditionalOnFactoryBean.class, PropertyPlaceholderAutoConfiguration.class)
            .run((context) -> assertThat(context.getBean(ExampleBean.class).toString()).isEqualTo("fromFactory"));
  }

  @Test
  void testOnMissingBeanConditionWithFactoryBeanInXml() {
    this.contextRunner.withUserConfiguration(FactoryBeanXmlConfiguration.class, ConditionalOnFactoryBean.class,
                    PropertyPlaceholderAutoConfiguration.class)
            .run((context) -> {
              assertThat(context.getBean(ExampleBean.class).toString()).isEqualTo("fromFactory");
            });
  }

  @Test
  void testOnMissingBeanConditionWithIgnoredSubclass() {
    this.contextRunner.withUserConfiguration(CustomExampleBeanConfiguration.class,
            ConditionalOnIgnoredSubclass.class, PropertyPlaceholderAutoConfiguration.class).run((context) -> {
      assertThat(context).getBeans(ExampleBean.class).hasSize(2);
      assertThat(context).getBeans(CustomExampleBean.class).hasSize(1);
    });
  }

  @Test
  void testOnMissingBeanConditionWithIgnoredSubclassByName() {
    this.contextRunner.withUserConfiguration(CustomExampleBeanConfiguration.class,
            ConditionalOnIgnoredSubclassByName.class, PropertyPlaceholderAutoConfiguration.class).run((context) -> {
      assertThat(context).getBeans(ExampleBean.class).hasSize(2);
      assertThat(context).getBeans(CustomExampleBean.class).hasSize(1);
    });
  }

  @Test
  void grandparentIsConsideredWhenUsingAncestorsStrategy() {
    this.contextRunner.withUserConfiguration(ExampleBeanConfiguration.class)
            .run((grandparent) -> new ApplicationContextRunner().withParent(grandparent)
                    .run((parent) -> new ApplicationContextRunner().withParent(parent)
                            .withUserConfiguration(ExampleBeanConfiguration.class,
                                    OnBeanInAncestorsConfiguration.class)
                            .run((context) -> assertThat(context).getBeans(ExampleBean.class).hasSize(1))));
  }

  @Test
  void currentContextIsIgnoredWhenUsingAncestorsStrategy() {
    this.contextRunner.run((parent) -> new ApplicationContextRunner().withParent(parent)
            .withUserConfiguration(ExampleBeanConfiguration.class, OnBeanInAncestorsConfiguration.class)
            .run((context) -> assertThat(context).getBeans(ExampleBean.class).hasSize(2)));
  }

  @Test
  void beanProducedByFactoryBeanIsConsideredWhenMatchingOnAnnotation() {
    this.contextRunner.withUserConfiguration(ConcreteFactoryBeanConfiguration.class,
            OnAnnotationWithFactoryBeanConfiguration.class).run((context) -> {
      assertThat(context).doesNotHaveBean("bar");
      assertThat(context).hasSingleBean(ExampleBean.class);
    });
  }

  @Test
  void parameterizedContainerWhenValueIsOfMissingBeanMatches() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithoutCustomConfig.class,
                    ParameterizedConditionWithValueConfig.class)
            .run((context) -> assertThat(context)
                    .satisfies(exampleBeanRequirement("otherExampleBean", "conditionalCustomExampleBean")));
  }

  @Test
  void parameterizedContainerWhenValueIsOfExistingBeanDoesNotMatch() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomConfig.class, ParameterizedConditionWithValueConfig.class)
            .run((context) -> assertThat(context).satisfies(exampleBeanRequirement("customExampleBean")));
  }

  @Test
  void parameterizedContainerWhenValueIsOfMissingBeanRegistrationMatches() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithoutCustomContainerConfig.class,
                    ParameterizedConditionWithValueConfig.class)
            .run((context) -> assertThat(context)
                    .satisfies(exampleBeanRequirement("otherExampleBean", "conditionalCustomExampleBean")));
  }

  @Test
  void parameterizedContainerWhenValueIsOfExistingBeanRegistrationDoesNotMatch() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
                    ParameterizedConditionWithValueConfig.class)
            .run((context) -> assertThat(context).satisfies(exampleBeanRequirement("customExampleBean")));
  }

  @Test
  void parameterizedContainerWhenReturnTypeIsOfExistingBeanDoesNotMatch() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomConfig.class,
                    ParameterizedConditionWithReturnTypeConfig.class)
            .run((context) -> assertThat(context).satisfies(exampleBeanRequirement("customExampleBean")));
  }

  @Test
  void parameterizedContainerWhenReturnTypeIsOfExistingBeanRegistrationDoesNotMatch() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
                    ParameterizedConditionWithReturnTypeConfig.class)
            .run((context) -> assertThat(context).satisfies(exampleBeanRequirement("customExampleBean")));
  }

  @Test
  void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanDoesNotMatch() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomConfig.class,
                    ParameterizedConditionWithReturnRegistrationTypeConfig.class)
            .run((context) -> assertThat(context).satisfies(exampleBeanRequirement("customExampleBean")));
  }

  @Test
  void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanRegistrationDoesNotMatch() {
    this.contextRunner
            .withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
                    ParameterizedConditionWithReturnRegistrationTypeConfig.class)
            .run((context) -> assertThat(context).satisfies(exampleBeanRequirement("customExampleBean")));
  }

  private Consumer<ConfigurableApplicationContext> exampleBeanRequirement(String... names) {
    return (context) -> {
      String[] beans = StringUtils.toStringArray(context.getBeanNamesForType(ExampleBean.class));
      String[] containers = StringUtils.toStringArray(context.getBeanNamesForType(TestParameterizedContainer.class));
      assertThat(StringUtils.concatenateStringArrays(beans, containers)).containsOnly(names);
    };
  }

  @Configuration(proxyBeanMethods = false)
  static class OnBeanInAncestorsConfiguration {

    @Bean
    @ConditionalOnMissingBean(search = SearchStrategy.ANCESTORS)
    ExampleBean exampleBean2() {
      return new ExampleBean("test");
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(name = "foo")
  static class OnBeanNameConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(name = "foo", value = Date.class)
  @ConditionalOnBean(name = "foo", value = Date.class)
  static class OnBeanNameAndTypeConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class FactoryBeanConfiguration {

    @Bean
    FactoryBean<ExampleBean> exampleBeanFactoryBean() {
      return new ExampleFactoryBean("foo");
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ComponentScan(basePackages = "cn.taketoday.context.condition.scan", useDefaultFilters = false,
                 includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                                          classes = ScannedFactoryBeanConfiguration.class))
  static class ComponentScannedFactoryBeanBeanMethodConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @ComponentScan(basePackages = "cn.taketoday.context.condition.scan", useDefaultFilters = false,
                 includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                                          classes = ScannedFactoryBeanWithBeanMethodArgumentsConfiguration.class))
  static class ComponentScannedFactoryBeanBeanMethodWithArgumentsConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class FactoryBeanWithBeanMethodArgumentsConfiguration {

    @Bean
    FactoryBean<ExampleBean> exampleBeanFactoryBean(@Value("${theValue}") String value) {
      return new ExampleFactoryBean(value);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ConcreteFactoryBeanConfiguration {

    @Bean
    ExampleFactoryBean exampleBeanFactoryBean() {
      return new ExampleFactoryBean("foo");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UnhelpfulFactoryBeanConfiguration {

    @Bean
    @SuppressWarnings("rawtypes")
    FactoryBean exampleBeanFactoryBean() {
      return new ExampleFactoryBean("foo");
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(NonspecificFactoryBeanClassAttributeRegistrar.class)
  static class NonspecificFactoryBeanClassAttributeConfiguration {

  }

  static class NonspecificFactoryBeanClassAttributeRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata meta, BootstrapContext registry) {
      BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(NonspecificFactoryBean.class);
      builder.addConstructorArgValue("foo");
      builder.getBeanDefinition().setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, ExampleBean.class);
      registry.registerBeanDefinition("exampleBeanFactoryBean", builder.getBeanDefinition());
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(NonspecificFactoryBeanClassAttributeRegistrar.class)
  static class NonspecificFactoryBeanStringAttributeConfiguration {

  }

  static class NonspecificFactoryBeanStringAttributeRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata meta, BootstrapContext registry) {
      BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(NonspecificFactoryBean.class);
      builder.addConstructorArgValue("foo");
      builder.getBeanDefinition().setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, ExampleBean.class.getName());
      registry.registerBeanDefinition("exampleBeanFactoryBean", builder.getBeanDefinition());
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(FactoryBeanRegistrar.class)
  static class RegisteredFactoryBeanConfiguration {

  }

  static class FactoryBeanRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata meta, BootstrapContext registry) {
      BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ExampleFactoryBean.class);
      builder.addConstructorArgValue("foo");
      registry.registerBeanDefinition("exampleBeanFactoryBean", builder.getBeanDefinition());
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ImportResource("cn/taketoday/context/condition/factorybean.xml")
  static class FactoryBeanXmlConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class ConditionalOnFactoryBean {

    @Bean
    @ConditionalOnMissingBean(ExampleBean.class)
    ExampleBean createExampleBean() {
      return new ExampleBean("direct");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ConditionalOnIgnoredSubclass {

    @Bean
    @ConditionalOnMissingBean(value = ExampleBean.class, ignored = CustomExampleBean.class)
    ExampleBean exampleBean() {
      return new ExampleBean("test");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ConditionalOnIgnoredSubclassByName {

    @Bean
    @ConditionalOnMissingBean(
            value = ExampleBean.class,
            ignoredType = "cn.taketoday.context.condition.ConditionalOnMissingBeanTests$CustomExampleBean")
    ExampleBean exampleBean() {
      return new ExampleBean("test");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomExampleBeanConfiguration {

    @Bean
    CustomExampleBean customExampleBean() {
      return new CustomExampleBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(annotation = EnableScheduling.class)
  static class OnAnnotationConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(annotation = TestAnnotation.class)
  static class OnAnnotationWithFactoryBeanConfiguration {

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
  @ConditionalOnMissingBean(name = "foo")
  static class HierarchyConsidered {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(name = "foo", search = SearchStrategy.CURRENT)
  static class HierarchyNotConsidered {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ExampleBeanConfiguration {

    @Bean
    ExampleBean exampleBean() {
      return new ExampleBean("test");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ImpliedOnBeanMethod {

    @Bean
    @ConditionalOnMissingBean
    ExampleBean exampleBean2() {
      return new ExampleBean("test");
    }

  }

  static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

    ExampleFactoryBean(String value) {
      Assert.state(!value.contains("$"), "value should not contain '$'");
    }

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

  static class NonspecificFactoryBean implements FactoryBean<Object> {

    NonspecificFactoryBean(String value) {
      Assert.state(!value.contains("$"), "value should not contain '$'");
    }

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
    @ConditionalOnMissingBean(value = CustomExampleBean.class,
                              parameterizedContainer = TestParameterizedContainer.class)
    CustomExampleBean conditionalCustomExampleBean() {
      return new CustomExampleBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParameterizedConditionWithReturnTypeConfig {

    @Bean
    @ConditionalOnMissingBean(parameterizedContainer = TestParameterizedContainer.class)
    CustomExampleBean conditionalCustomExampleBean() {
      return new CustomExampleBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParameterizedConditionWithReturnRegistrationTypeConfig {

    @Bean
    @ConditionalOnMissingBean(parameterizedContainer = TestParameterizedContainer.class)
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
