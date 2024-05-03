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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.AbstractBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AbstractBeanFactory} type inference from
 * {@link FactoryBean FactoryBeans} defined in the configuration.
 *
 * @author Phillip Webb
 */
class ConfigurationWithFactoryBeanEarlyDeductionTests {

  @Test
  void preFreezeDirect() {
    assertPreFreeze(DirectConfiguration.class);
  }

  @Test
  void postFreezeDirect() {
    assertPostFreeze(DirectConfiguration.class);
  }

  @Test
  void preFreezeGenericMethod() {
    assertPreFreeze(GenericMethodConfiguration.class);
  }

  @Test
  void postFreezeGenericMethod() {
    assertPostFreeze(GenericMethodConfiguration.class);
  }

  @Test
  void preFreezeGenericClass() {
    assertPreFreeze(GenericClassConfiguration.class);
  }

  @Test
  void postFreezeGenericClass() {
    assertPostFreeze(GenericClassConfiguration.class);
  }

  @Test
  void preFreezeAttribute() {
    assertPreFreeze(AttributeClassConfiguration.class);
  }

  @Test
  void postFreezeAttribute() {
    assertPostFreeze(AttributeClassConfiguration.class);
  }

  @Test
  void preFreezeTargetType() {
    assertPreFreeze(TargetTypeConfiguration.class);
  }

  @Test
  void postFreezeTargetType() {
    assertPostFreeze(TargetTypeConfiguration.class);
  }

  @Test
  void preFreezeUnresolvedGenericFactoryBean() {
    // Covers the case where a @Configuration is picked up via component scanning
    // and its bean definition only has a String bean class. In such cases
    // beanDefinition.hasBeanClass() returns false so we need to actually
    // call determineTargetType ourselves
    GenericBeanDefinition factoryBeanDefinition = new GenericBeanDefinition();
    factoryBeanDefinition.setBeanClassName(GenericClassConfiguration.class.getName());
    GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
    beanDefinition.setBeanClass(FactoryBean.class);
    beanDefinition.setFactoryBeanName("factoryBean");
    beanDefinition.setFactoryMethodName("myBean");
    GenericApplicationContext context = new GenericApplicationContext();
    try (context) {
      context.registerBeanDefinition("factoryBean", factoryBeanDefinition);
      context.registerBeanDefinition("myBean", beanDefinition);
      NameCollectingBeanFactoryPostProcessor postProcessor = new NameCollectingBeanFactoryPostProcessor();
      context.addBeanFactoryPostProcessor(postProcessor);
      context.refresh();
      assertContainsMyBeanName(postProcessor.getNames());
    }
  }

  private void assertPostFreeze(Class<?> configurationClass) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configurationClass);
    assertContainsMyBeanName(context);
  }

  private void assertPreFreeze(Class<?> configurationClass, BeanFactoryPostProcessor... postProcessors) {
    NameCollectingBeanFactoryPostProcessor postProcessor = new NameCollectingBeanFactoryPostProcessor();
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    try (context) {
      Arrays.stream(postProcessors).forEach(context::addBeanFactoryPostProcessor);
      context.addBeanFactoryPostProcessor(postProcessor);
      context.register(configurationClass);
      context.refresh();
      assertContainsMyBeanName(postProcessor.getNames());
    }
  }

  private void assertContainsMyBeanName(AnnotationConfigApplicationContext context) {
    assertContainsMyBeanName(context.getBeanNamesForType(MyBean.class, true, false).toArray(String[]::new));
  }

  private void assertContainsMyBeanName(String[] names) {
    assertThat(names).containsExactly("myBean");
  }

  private static class NameCollectingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private String[] names;

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
      ResolvableType typeToMatch = ResolvableType.forClassWithGenerics(MyBean.class, String.class);
      this.names = beanFactory.getBeanNamesForType(typeToMatch, true, false).toArray(String[]::new);
    }

    public String[] getNames() {
      return this.names;
    }
  }

  @Configuration
  static class DirectConfiguration {

    @Bean
    MyBean<String> myBean() {
      return new MyBean<>();
    }
  }

  @Configuration
  static class GenericMethodConfiguration {

    @Bean
    FactoryBean<MyBean<String>> myBean() {
      return new TestFactoryBean<>(new MyBean<>());
    }
  }

  @Configuration
  static class GenericClassConfiguration {

    @Bean
    MyFactoryBean myBean() {
      return new MyFactoryBean();
    }
  }

  @Configuration
  @Import(AttributeClassRegistrar.class)
  static class AttributeClassConfiguration {
  }

  static class AttributeClassRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {

      BeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(
              RawWithAbstractObjectTypeFactoryBean.class).getBeanDefinition();
      definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE,
              ResolvableType.forClassWithGenerics(MyBean.class, String.class));
      context.registerBeanDefinition("myBean", definition);
    }

  }

  @Configuration
  @Import(TargetTypeRegistrar.class)
  static class TargetTypeConfiguration {
  }

  static class TargetTypeRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {

      RootBeanDefinition definition = new RootBeanDefinition(RawWithAbstractObjectTypeFactoryBean.class);
      definition.setTargetType(ResolvableType.forClassWithGenerics(FactoryBean.class,
              ResolvableType.forClassWithGenerics(MyBean.class, String.class)));
      context.registerBeanDefinition("myBean", definition);
    }

  }

  abstract static class AbstractMyBean {
  }

  static class MyBean<T> extends AbstractMyBean {
  }

  static class TestFactoryBean<T> implements FactoryBean<T> {

    private final T instance;

    public TestFactoryBean(T instance) {
      this.instance = instance;
    }

    @Override
    public T getObject() {
      return this.instance;
    }

    @Override
    public Class<?> getObjectType() {
      return this.instance.getClass();
    }
  }

  static class MyFactoryBean extends TestFactoryBean<MyBean<String>> {

    public MyFactoryBean() {
      super(new MyBean<>());
    }
  }

  static class RawWithAbstractObjectTypeFactoryBean implements FactoryBean<Object> {

    @Override
    public Object getObject() throws Exception {
      throw new IllegalStateException();
    }

    @Override
    public Class<?> getObjectType() {
      return MyBean.class;
    }
  }

}
