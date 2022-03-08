/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import cn.taketoday.beans.factory.support.AbstractBeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AbstractBeanFactory} type inference from
 * {@link FactoryBean FactoryBeans} defined in the configuration.
 *
 * @author Phillip Webb
 */
public class ConfigurationWithFactoryBeanBeanEarlyDeductionTests {

  @Test
  public void preFreezeDirect() {
    assertPreFreeze(DirectConfiguration.class);
  }

  @Test
  public void postFreezeDirect() {
    assertPostFreeze(DirectConfiguration.class);
  }

  @Test
  public void preFreezeGenericMethod() {
    assertPreFreeze(GenericMethodConfiguration.class);
  }

  @Test
  public void postFreezeGenericMethod() {
    assertPostFreeze(GenericMethodConfiguration.class);
  }

  @Test
  public void preFreezeGenericClass() {
    assertPreFreeze(GenericClassConfiguration.class);
  }

  @Test
  public void postFreezeGenericClass() {
    assertPostFreeze(GenericClassConfiguration.class);
  }

  @Test
  public void preFreezeAttribute() {
    assertPreFreeze(AttributeClassConfiguration.class);
  }

  @Test
  public void postFreezeAttribute() {
    assertPostFreeze(AttributeClassConfiguration.class);
  }

  @Test
  public void preFreezeUnresolvedGenericFactoryBean() {
    // Covers the case where a @Configuration is picked up via component scanning
    // and its bean definition only has a String bean class. In such cases
    // beanDefinition.hasBeanClass() returns false so we need to actually
    // call determineTargetType ourselves
    BeanDefinition factoryBeanDefinition = new BeanDefinition();
    factoryBeanDefinition.setBeanClassName(GenericClassConfiguration.class.getName());
    BeanDefinition beanDefinition = new BeanDefinition();
    beanDefinition.setBeanClass(FactoryBean.class);
    beanDefinition.setFactoryBeanName("factoryBean");
    beanDefinition.setFactoryMethodName("myBean");
    GenericApplicationContext context = new GenericApplicationContext();
    try {
      context.registerBeanDefinition("factoryBean", factoryBeanDefinition);
      context.registerBeanDefinition("myBean", beanDefinition);
      NameCollectingBeanFactoryPostProcessor postProcessor = new NameCollectingBeanFactoryPostProcessor();
      context.addBeanFactoryPostProcessor(postProcessor);
      context.refresh();
      assertContainsMyBeanName(postProcessor.getNames());
    }
    finally {
      context.close();
    }
  }

  private void assertPostFreeze(Class<?> configurationClass) {
    StandardApplicationContext context = new StandardApplicationContext(
            configurationClass);
    assertContainsMyBeanName(context);
  }

  private void assertPreFreeze(Class<?> configurationClass,
                               BeanFactoryPostProcessor... postProcessors) {
    NameCollectingBeanFactoryPostProcessor postProcessor = new NameCollectingBeanFactoryPostProcessor();
    StandardApplicationContext context = new StandardApplicationContext();
    try {
      Arrays.stream(postProcessors).forEach(context::addBeanFactoryPostProcessor);
      context.addBeanFactoryPostProcessor(postProcessor);
      context.register(configurationClass);
      context.refresh();
      assertContainsMyBeanName(postProcessor.getNames());
    }
    finally {
      context.close();
    }
  }

  private void assertContainsMyBeanName(StandardApplicationContext context) {
    assertContainsMyBeanName(context.getBeanNamesForType(MyBean.class, true, false));
  }

  private void assertContainsMyBeanName(Set<String> names) {
    assertThat(names).containsExactly("myBean");
  }

  private static class NameCollectingBeanFactoryPostProcessor
          implements BeanFactoryPostProcessor {

    private Set<String> names;

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory)
            throws BeansException {
      this.names = beanFactory.getBeanNamesForType(MyBean.class, true, false);
    }

    public Set<String> getNames() {
      return this.names;
    }

  }

  @Configuration
  static class DirectConfiguration {

    @Bean
    MyBean myBean() {
      return new MyBean();
    }

  }

  @Configuration
  static class GenericMethodConfiguration {

    @Bean
    FactoryBean<MyBean> myBean() {
      return new TestFactoryBean<>(new MyBean());
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
      BeanDefinition definition = new BeanDefinition(RawWithAbstractObjectTypeFactoryBean.class);
      definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, MyBean.class);
      context.registerBeanDefinition("myBean", definition);
    }
  }

  abstract static class AbstractMyBean {
  }

  static class MyBean extends AbstractMyBean {
  }

  static class TestFactoryBean<T> implements FactoryBean<T> {

    private final T instance;

    public TestFactoryBean(T instance) {
      this.instance = instance;
    }

    @Override
    public T getObject() throws Exception {
      return this.instance;
    }

    @Override
    public Class<?> getObjectType() {
      return this.instance.getClass();
    }

  }

  static class MyFactoryBean extends TestFactoryBean<MyBean> {

    public MyFactoryBean() {
      super(new MyBean());
    }

  }

  static class RawWithAbstractObjectTypeFactoryBean implements FactoryBean<Object> {

    private final Object object = new MyBean();

    @Override
    public Object getObject() throws Exception {
      return object;
    }

    @Override
    public Class<?> getObjectType() {
      return MyBean.class;
    }

  }

}
