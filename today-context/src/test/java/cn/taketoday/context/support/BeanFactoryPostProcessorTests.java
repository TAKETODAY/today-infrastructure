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

package cn.taketoday.context.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.lang.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the interaction between {@link ApplicationContext} implementations and
 * any registered {@link BeanFactoryPostProcessor} implementations.  Specifically
 * {@link StaticApplicationContext} is used for the tests, but what's represented
 * here is any {@link AbstractApplicationContext} implementation.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 02.10.2003
 */
public class BeanFactoryPostProcessorTests {

  @Test
  public void testRegisteredBeanFactoryPostProcessor() {
    StaticApplicationContext ac = new StaticApplicationContext();
    ac.registerSingleton("tb1", TestBean.class);
    ac.registerSingleton("tb2", TestBean.class);
    TestBeanFactoryPostProcessor bfpp = new TestBeanFactoryPostProcessor();
    ac.addBeanFactoryPostProcessor(bfpp);
    assertThat(bfpp.wasCalled).isFalse();
    ac.refresh();
    assertThat(bfpp.wasCalled).isTrue();
  }

  @Test
  public void testDefinedBeanFactoryPostProcessor() {
    StaticApplicationContext ac = new StaticApplicationContext();
    ac.registerSingleton("tb1", TestBean.class);
    ac.registerSingleton("tb2", TestBean.class);
    ac.registerSingleton("bfpp", TestBeanFactoryPostProcessor.class);
    ac.refresh();
    TestBeanFactoryPostProcessor bfpp = (TestBeanFactoryPostProcessor) ac.getBean("bfpp");
    assertThat(bfpp.wasCalled).isTrue();
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testMultipleDefinedBeanFactoryPostProcessors() {
    StaticApplicationContext ac = new StaticApplicationContext();
    ac.registerSingleton("tb1", TestBean.class);
    ac.registerSingleton("tb2", TestBean.class);
    PropertyValues pvs1 = new PropertyValues();
    pvs1.add("initValue", "${key}");
    ac.registerSingleton("bfpp1", TestBeanFactoryPostProcessor.class, pvs1);
    PropertyValues pvs2 = new PropertyValues();
    pvs2.add("properties", "key=value");
    ac.registerSingleton("bfpp2", cn.taketoday.beans.factory.config.PropertyPlaceholderConfigurer.class, pvs2);
    ac.refresh();
    TestBeanFactoryPostProcessor bfpp = (TestBeanFactoryPostProcessor) ac.getBean("bfpp1");
    assertThat(bfpp.initValue).isEqualTo("value");
    assertThat(bfpp.wasCalled).isTrue();
  }

  @Test
  public void testBeanFactoryPostProcessorNotExecutedByBeanFactory() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("tb1", new RootBeanDefinition(TestBean.class));
    bf.registerBeanDefinition("tb2", new RootBeanDefinition(TestBean.class));
    bf.registerBeanDefinition("bfpp", new RootBeanDefinition(TestBeanFactoryPostProcessor.class));
    TestBeanFactoryPostProcessor bfpp = (TestBeanFactoryPostProcessor) bf.getBean("bfpp");
    assertThat(bfpp.wasCalled).isFalse();
  }

  @Test
  public void testBeanDefinitionRegistryPostProcessor() {
    StaticApplicationContext ac = new StaticApplicationContext();
    ac.registerSingleton("tb1", TestBean.class);
    ac.registerSingleton("tb2", TestBean.class);
    ac.addBeanFactoryPostProcessor(new PrioritizedBeanDefinitionRegistryPostProcessor());
    TestBeanDefinitionRegistryPostProcessor bdrpp = new TestBeanDefinitionRegistryPostProcessor();
    ac.addBeanFactoryPostProcessor(bdrpp);
    assertThat(bdrpp.wasCalled).isFalse();
    ac.refresh();
    assertThat(bdrpp.wasCalled).isTrue();
    assertThat(ac.getBean("bfpp1", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
    assertThat(ac.getBean("bfpp2", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
  }

  @Test
  public void testBeanDefinitionRegistryPostProcessorRegisteringAnother() {
    StaticApplicationContext ac = new StaticApplicationContext();
    ac.registerSingleton("tb1", TestBean.class);
    ac.registerSingleton("tb2", TestBean.class);
    ac.registerBeanDefinition("bdrpp2", new RootBeanDefinition(OuterBeanDefinitionRegistryPostProcessor.class));
    ac.refresh();
    assertThat(ac.getBean("bfpp1", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
    assertThat(ac.getBean("bfpp2", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
  }

  @Test
  public void testPrioritizedBeanDefinitionRegistryPostProcessorRegisteringAnother() {
    StaticApplicationContext ac = new StaticApplicationContext();
    ac.registerSingleton("tb1", TestBean.class);
    ac.registerSingleton("tb2", TestBean.class);
    ac.registerBeanDefinition("bdrpp2", new RootBeanDefinition(PrioritizedOuterBeanDefinitionRegistryPostProcessor.class));
    ac.refresh();
    assertThat(ac.getBean("bfpp1", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
    assertThat(ac.getBean("bfpp2", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
  }

  @Test
  public void testBeanFactoryPostProcessorAsApplicationListener() {
    StaticApplicationContext ac = new StaticApplicationContext();
    ac.registerBeanDefinition("bfpp", new RootBeanDefinition(ListeningBeanFactoryPostProcessor.class));
    ac.refresh();
    boolean condition = ac.getBean(ListeningBeanFactoryPostProcessor.class).received instanceof ContextRefreshedEvent;
    assertThat(condition).isTrue();
  }

  @Test
  public void testBeanFactoryPostProcessorWithInnerBeanAsApplicationListener() {
    StaticApplicationContext ac = new StaticApplicationContext();
    RootBeanDefinition rbd = new RootBeanDefinition(NestingBeanFactoryPostProcessor.class);
    rbd.getPropertyValues().add("listeningBean", new RootBeanDefinition(ListeningBean.class));
    ac.registerBeanDefinition("bfpp", rbd);
    ac.refresh();
    boolean condition = ac.getBean(NestingBeanFactoryPostProcessor.class).getListeningBean().received instanceof ContextRefreshedEvent;
    assertThat(condition).isTrue();
  }

  public static class TestBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    public String initValue;

    public void setInitValue(String initValue) {
      this.initValue = initValue;
    }

    public boolean wasCalled = false;

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
      wasCalled = true;
    }
  }

  public static class PrioritizedBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
      registry.registerBeanDefinition("bfpp1", new RootBeanDefinition(TestBeanFactoryPostProcessor.class));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    }
  }

  public static class TestBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    public boolean wasCalled;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
      assertThat(registry.containsBeanDefinition("bfpp1")).isTrue();
      registry.registerBeanDefinition("bfpp2", new RootBeanDefinition(TestBeanFactoryPostProcessor.class));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
      this.wasCalled = true;
    }
  }

  public static class OuterBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
      registry.registerBeanDefinition("anotherpp", new RootBeanDefinition(TestBeanDefinitionRegistryPostProcessor.class));
      registry.registerBeanDefinition("ppp", new RootBeanDefinition(PrioritizedBeanDefinitionRegistryPostProcessor.class));
    }

  }

  public static class PrioritizedOuterBeanDefinitionRegistryPostProcessor extends OuterBeanDefinitionRegistryPostProcessor
          implements PriorityOrdered {

    @Override
    public int getOrder() {
      return HIGHEST_PRECEDENCE;
    }
  }

  public static class ListeningBeanFactoryPostProcessor implements BeanFactoryPostProcessor, ApplicationListener<ApplicationEvent> {

    public ApplicationEvent received;

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
      Assert.state(this.received == null, "Just one ContextRefreshedEvent expected");
      this.received = event;
    }
  }

  public static class ListeningBean implements ApplicationListener<ApplicationEvent> {

    public ApplicationEvent received;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
      Assert.state(this.received == null, "Just one ContextRefreshedEvent expected");
      this.received = event;
    }
  }

  public static class NestingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private ListeningBean listeningBean;

    public void setListeningBean(ListeningBean listeningBean) {
      this.listeningBean = listeningBean;
    }

    public ListeningBean getListeningBean() {
      return listeningBean;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    }
  }

}
