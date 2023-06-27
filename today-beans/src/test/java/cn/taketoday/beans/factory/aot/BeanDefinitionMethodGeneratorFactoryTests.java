/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.aot;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.test.io.support.MockTodayStrategies;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BeanDefinitionMethodGeneratorFactory}.
 *
 * @author Phillip Webb
 */
class BeanDefinitionMethodGeneratorFactoryTests {

  @Test
  void createWhenBeanRegistrationExcludeFilterBeanIsNotAotProcessorThrowsException() {
    BeanRegistrationExcludeFilter filter = registeredBean -> false;
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("filter", filter);
    assertThatIllegalStateException()
            .isThrownBy(() -> new BeanDefinitionMethodGeneratorFactory(beanFactory))
            .withMessageContaining("also implement an AOT processor interface");
  }

  @Test
  void createWhenBeanRegistrationExcludeFilterFactoryIsNotAotProcessorLoads() {
    BeanRegistrationExcludeFilter filter = registeredBean -> false;
    MockTodayStrategies loader = new MockTodayStrategies();
    loader.addInstance(BeanRegistrationExcludeFilter.class, filter);
    assertThatNoException().isThrownBy(() -> new BeanDefinitionMethodGeneratorFactory(
            AotServices.factories(loader)));
  }

  @Test
  void getBeanDefinitionMethodGeneratorWhenExcludedByBeanRegistrationExcludeFilterReturnsNull() {
    MockTodayStrategies todayStrategies = new MockTodayStrategies();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    todayStrategies.addInstance(BeanRegistrationExcludeFilter.class,
            new MockBeanRegistrationExcludeFilter(true, 0));
    RegisteredBean registeredBean = registerTestBean(beanFactory);
    BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
            AotServices.factoriesAndBeans(todayStrategies, beanFactory));
    assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean)).isNull();
  }

  @Test
  void getBeanDefinitionMethodGeneratorWhenExcludedByBeanRegistrationExcludeFilterBeanReturnsNull() {
    MockTodayStrategies todayStrategies = new MockTodayStrategies();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RegisteredBean registeredBean = registerTestBean(beanFactory);
    beanFactory.registerSingleton("filter",
            new MockBeanRegistrationExcludeFilter(true, 0));
    BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
            AotServices.factoriesAndBeans(todayStrategies, beanFactory));
    assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean)).isNull();
  }

  @Test
  void getBeanDefinitionMethodGeneratorConsidersFactoryLoadedExcludeFiltersAndBeansInOrderedOrder() {
    MockBeanRegistrationExcludeFilter filter1 = new MockBeanRegistrationExcludeFilter(false, 1);
    MockBeanRegistrationExcludeFilter filter2 = new MockBeanRegistrationExcludeFilter(false, 2);
    MockBeanRegistrationExcludeFilter filter3 = new MockBeanRegistrationExcludeFilter(false, 3);
    MockBeanRegistrationExcludeFilter filter4 = new MockBeanRegistrationExcludeFilter(true, 4);
    MockBeanRegistrationExcludeFilter filter5 = new MockBeanRegistrationExcludeFilter(true, 5);
    MockBeanRegistrationExcludeFilter filter6 = new MockBeanRegistrationExcludeFilter(true, 6);
    MockTodayStrategies todayStrategies = new MockTodayStrategies();
    todayStrategies.addInstance(BeanRegistrationExcludeFilter.class, filter3, filter1, filter5);
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("filter4", filter4);
    beanFactory.registerSingleton("filter2", filter2);
    beanFactory.registerSingleton("filter6", filter6);
    RegisteredBean registeredBean = registerTestBean(beanFactory);
    BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
            AotServices.factoriesAndBeans(todayStrategies, beanFactory));
    assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean)).isNull();
    assertThat(filter1.wasCalled()).isTrue();
    assertThat(filter2.wasCalled()).isTrue();
    assertThat(filter3.wasCalled()).isTrue();
    assertThat(filter4.wasCalled()).isTrue();
    assertThat(filter5.wasCalled()).isFalse();
    assertThat(filter6.wasCalled()).isFalse();
  }

  @Test
  void getBeanDefinitionMethodGeneratorAddsContributionsFromProcessors() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanRegistrationAotContribution beanContribution = mock();
    BeanRegistrationAotProcessor processorBean = registeredBean -> beanContribution;
    beanFactory.registerSingleton("processorBean", processorBean);
    MockTodayStrategies todayStrategies = new MockTodayStrategies();
    BeanRegistrationAotContribution loaderContribution = mock();
    BeanRegistrationAotProcessor loaderProcessor = registeredBean -> loaderContribution;
    todayStrategies.addInstance(BeanRegistrationAotProcessor.class,
            loaderProcessor);
    RegisteredBean registeredBean = registerTestBean(beanFactory);
    BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
            AotServices.factoriesAndBeans(todayStrategies, beanFactory));
    BeanDefinitionMethodGenerator methodGenerator = methodGeneratorFactory
            .getBeanDefinitionMethodGenerator(registeredBean);
    assertThat(methodGenerator).extracting("aotContributions").asList()
            .containsExactly(beanContribution, loaderContribution);
  }

  @Test
  void getBeanDefinitionMethodGeneratorWhenRegisteredBeanIsAotProcessorFiltersBean() {
    MockTodayStrategies todayStrategies = new MockTodayStrategies();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("test1", BeanDefinitionBuilder
            .rootBeanDefinition(TestBeanFactoryInitializationAotProcessorBean.class).getBeanDefinition());
    RegisteredBean registeredBean1 = RegisteredBean.of(beanFactory, "test1");
    beanFactory.registerBeanDefinition("test2", BeanDefinitionBuilder
            .rootBeanDefinition(TestBeanRegistrationAotProcessorBean.class).getBeanDefinition());
    RegisteredBean registeredBean2 = RegisteredBean.of(beanFactory, "test2");
    BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
            AotServices.factoriesAndBeans(todayStrategies, beanFactory));
    assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean1)).isNull();
    assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean2)).isNull();
  }

  @Test
  void getBeanDefinitionMethodGeneratorWhenRegisteredBeanIsAotProcessorAndIsNotExcludedAndBeanRegistrationExcludeFilterDoesNotFilterBean() {
    MockTodayStrategies todayStrategies = new MockTodayStrategies();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("test", BeanDefinitionBuilder
            .rootBeanDefinition(TestBeanRegistrationAotProcessorAndNotExcluded.class).getBeanDefinition());
    RegisteredBean registeredBean1 = RegisteredBean.of(beanFactory, "test");
    BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
            AotServices.factoriesAndBeans(todayStrategies, beanFactory));
    assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean1)).isNotNull();
  }

  private RegisteredBean registerTestBean(StandardBeanFactory beanFactory) {
    beanFactory.registerBeanDefinition("test", BeanDefinitionBuilder
            .rootBeanDefinition(TestBean.class).getBeanDefinition());
    return RegisteredBean.of(beanFactory, "test");
  }

  static class MockBeanRegistrationExcludeFilter implements
          BeanRegistrationAotProcessor, BeanRegistrationExcludeFilter, Ordered {

    private final boolean excluded;

    private final int order;

    @Nullable
    private RegisteredBean registeredBean;

    MockBeanRegistrationExcludeFilter(boolean excluded, int order) {
      this.excluded = excluded;
      this.order = order;
    }

    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
      return null;
    }

    @Override
    public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
      this.registeredBean = registeredBean;
      return this.excluded;
    }

    @Override
    public int getOrder() {
      return this.order;
    }

    boolean wasCalled() {
      return this.registeredBean != null;
    }

  }

  static class TestBean {

  }

  static class TestBeanFactoryInitializationAotProcessorBean implements BeanFactoryInitializationAotProcessor {

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
      return null;
    }

  }

  static class TestBeanRegistrationAotProcessorBean implements BeanRegistrationAotProcessor {

    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
      return null;
    }

  }

  static class TestBeanRegistrationAotProcessorAndNotExcluded
          extends TestBeanRegistrationAotProcessorBean {

    @Override
    public boolean isBeanExcludedFromAotProcessing() {
      return false;
    }

  }

  @SuppressWarnings("unused")
  static class InnerTestBean {

  }

}
