/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.framework.test.mock.mockito.example.ExampleService;
import cn.taketoday.framework.test.mock.mockito.example.FailingExampleService;
import cn.taketoday.framework.test.mock.mockito.example.RealExampleService;
import cn.taketoday.lang.Assert;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test for {@link MockitoPostProcessor}. See also the integration tests.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Andreas Neiser
 * @author Madhura Bhave
 */
class MockitoPostProcessorTests {

  @Test
  void cannotMockMultipleBeans() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    MockitoPostProcessor.register(context);
    context.register(MultipleBeans.class);
    assertThatIllegalStateException().isThrownBy(context::refresh)
        .withMessageContaining("Unable to register mock bean " + ExampleService.class.getName()
            + " expected a single matching bean to replace but found [example1, example2]");
  }

  @Test
  void cannotMockMultipleQualifiedBeans() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    MockitoPostProcessor.register(context);
    context.register(MultipleQualifiedBeans.class);
    assertThatIllegalStateException().isThrownBy(context::refresh)
        .withMessageContaining("Unable to register mock bean " + ExampleService.class.getName()
            + " expected a single matching bean to replace but found [example1, example3]");
  }

  @Test
  void canMockBeanProducedByFactoryBeanWithClassObjectTypeAttribute() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    MockitoPostProcessor.register(context);
    RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(TestFactoryBean.class);
    factoryBeanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, SomeInterface.class);
    context.registerBeanDefinition("beanToBeMocked", factoryBeanDefinition);
    context.register(MockedFactoryBean.class);
    context.refresh();
    assertThat(Mockito.mockingDetails(context.getBean("beanToBeMocked")).isMock()).isTrue();
  }

  @Test
  void canMockBeanProducedByFactoryBeanWithResolvableTypeObjectTypeAttribute() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    MockitoPostProcessor.register(context);
    RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(TestFactoryBean.class);
    ResolvableType objectType = ResolvableType.forClass(SomeInterface.class);
    factoryBeanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, objectType);
    context.registerBeanDefinition("beanToBeMocked", factoryBeanDefinition);
    context.register(MockedFactoryBean.class);
    context.refresh();
    assertThat(Mockito.mockingDetails(context.getBean("beanToBeMocked")).isMock()).isTrue();
  }

  @Test
  void canMockPrimaryBean() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    MockitoPostProcessor.register(context);
    context.register(MockPrimaryBean.class);
    context.refresh();
    assertThat(Mockito.mockingDetails(context.getBean(MockPrimaryBean.class).mock).isMock()).isTrue();
    assertThat(Mockito.mockingDetails(context.getBean(ExampleService.class)).isMock()).isTrue();
    assertThat(Mockito.mockingDetails(context.getBean("examplePrimary", ExampleService.class)).isMock()).isTrue();
    assertThat(Mockito.mockingDetails(context.getBean("exampleQualified", ExampleService.class)).isMock())
        .isFalse();
  }

  @Test
  void canMockQualifiedBeanWithPrimaryBeanPresent() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    MockitoPostProcessor.register(context);
    context.register(MockQualifiedBean.class);
    context.refresh();
    assertThat(Mockito.mockingDetails(context.getBean(MockQualifiedBean.class).mock).isMock()).isTrue();
    assertThat(Mockito.mockingDetails(context.getBean(ExampleService.class)).isMock()).isFalse();
    assertThat(Mockito.mockingDetails(context.getBean("examplePrimary", ExampleService.class)).isMock()).isFalse();
    assertThat(Mockito.mockingDetails(context.getBean("exampleQualified", ExampleService.class)).isMock()).isTrue();
  }

  @Test
  void canSpyPrimaryBean() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    MockitoPostProcessor.register(context);
    context.register(SpyPrimaryBean.class);
    context.refresh();
    assertThat(Mockito.mockingDetails(context.getBean(SpyPrimaryBean.class).spy).isSpy()).isTrue();
    assertThat(Mockito.mockingDetails(context.getBean(ExampleService.class)).isSpy()).isTrue();
    assertThat(Mockito.mockingDetails(context.getBean("examplePrimary", ExampleService.class)).isSpy()).isTrue();
    assertThat(Mockito.mockingDetails(context.getBean("exampleQualified", ExampleService.class)).isSpy()).isFalse();
  }

  @Test
  void canSpyQualifiedBeanWithPrimaryBeanPresent() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    MockitoPostProcessor.register(context);
    context.register(SpyQualifiedBean.class);
    context.refresh();
    assertThat(Mockito.mockingDetails(context.getBean(SpyQualifiedBean.class).spy).isSpy()).isTrue();
    assertThat(Mockito.mockingDetails(context.getBean(ExampleService.class)).isSpy()).isFalse();
    assertThat(Mockito.mockingDetails(context.getBean("examplePrimary", ExampleService.class)).isSpy()).isFalse();
    assertThat(Mockito.mockingDetails(context.getBean("exampleQualified", ExampleService.class)).isSpy()).isTrue();
  }

  @Test
  void postProcessorShouldNotTriggerEarlyInitialization() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(FactoryBeanRegisteringPostProcessor.class);
    MockitoPostProcessor.register(context);
    context.register(TestBeanFactoryPostProcessor.class);
    context.register(EagerInitBean.class);
    context.refresh();
  }

  @Configuration(proxyBeanMethods = false)
  @MockBean(SomeInterface.class)
  static class MockedFactoryBean {

    @Bean
    TestFactoryBean testFactoryBean() {
      return new TestFactoryBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @MockBean(ExampleService.class)
  static class MultipleBeans {

    @Bean
    ExampleService example1() {
      return new FailingExampleService();
    }

    @Bean
    ExampleService example2() {
      return new FailingExampleService();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MultipleQualifiedBeans {

    @MockBean
    @Qualifier("test")
    private ExampleService mock;

    @Bean
    @Qualifier("test")
    ExampleService example1() {
      return new FailingExampleService();
    }

    @Bean
    ExampleService example2() {
      return new FailingExampleService();
    }

    @Bean
    @Qualifier("test")
    ExampleService example3() {
      return new FailingExampleService();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MockPrimaryBean {

    @MockBean
    private ExampleService mock;

    @Bean
    @Qualifier("test")
    ExampleService exampleQualified() {
      return new RealExampleService("qualified");
    }

    @Bean
    @Primary
    ExampleService examplePrimary() {
      return new RealExampleService("primary");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MockQualifiedBean {

    @MockBean
    @Qualifier("test")
    private ExampleService mock;

    @Bean
    @Qualifier("test")
    ExampleService exampleQualified() {
      return new RealExampleService("qualified");
    }

    @Bean
    @Primary
    ExampleService examplePrimary() {
      return new RealExampleService("primary");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SpyPrimaryBean {

    @SpyBean
    private ExampleService spy;

    @Bean
    @Qualifier("test")
    ExampleService exampleQualified() {
      return new RealExampleService("qualified");
    }

    @Bean
    @Primary
    ExampleService examplePrimary() {
      return new RealExampleService("primary");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SpyQualifiedBean {

    @SpyBean
    @Qualifier("test")
    private ExampleService spy;

    @Bean
    @Qualifier("test")
    ExampleService exampleQualified() {
      return new RealExampleService("qualified");
    }

    @Bean
    @Primary
    ExampleService examplePrimary() {
      return new RealExampleService("primary");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class EagerInitBean {

    @MockBean
    private ExampleService service;

  }

  static class TestFactoryBean implements FactoryBean<Object> {

    @Override
    public Object getObject() {
      return new TestBean();
    }

    @Override
    public Class<?> getObjectType() {
      return null;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

  }

  static class FactoryBeanRegisteringPostProcessor implements BeanFactoryPostProcessor, Ordered {

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
      RootBeanDefinition beanDefinition = new RootBeanDefinition(TestFactoryBean.class);
      ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("test", beanDefinition);
    }

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE;
    }

  }

  static class TestBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    @SuppressWarnings("unchecked")
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
      Map<String, BeanWrapper> cache = (Map<String, BeanWrapper>) ReflectionTestUtils.getField(beanFactory,
          "factoryBeanInstanceCache");
      Assert.isTrue(cache.isEmpty(), "Early initialization of factory bean triggered.");
    }

  }

  interface SomeInterface {

  }

  static class TestBean implements SomeInterface {

  }

}
