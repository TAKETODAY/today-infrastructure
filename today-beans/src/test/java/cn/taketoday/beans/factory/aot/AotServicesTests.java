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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.test.io.support.MockTodayStrategies;
import cn.taketoday.lang.TodayStrategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AotServices}.
 *
 * @author Phillip Webb
 */
class AotServicesTests {

  @Test
  void factoriesLoadsFromAotFactoriesFiles() {
    AotServices<?> loaded = AotServices.factories()
            .load(BeanFactoryInitializationAotProcessor.class);
    assertThat(loaded)
            .anyMatch(BeanFactoryInitializationAotProcessor.class::isInstance);
  }

  @Test
  void factoriesWithClassLoaderLoadsFromAotFactoriesFile() {
    TestFactoriesClassLoader classLoader = new TestFactoriesClassLoader(
            "aot-services.factories");
    AotServices<?> loaded = AotServices.factories(classLoader)
            .load(TestService.class);
    assertThat(loaded).anyMatch(TestServiceImpl.class::isInstance);
  }

  @Test
  void factoriesWithTodayStrategiesWhenTodayStrategiesIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AotServices.factories((TodayStrategies) null))
            .withMessage("'strategies' is required");
  }

  @Test
  void factoriesWithTodayStrategiesLoadsFromTodayStrategies() {
    MockTodayStrategies loader = new MockTodayStrategies();
    loader.addInstance(TestService.class, new TestServiceImpl());
    AotServices<?> loaded = AotServices.factories(loader).load(TestService.class);
    assertThat(loaded).anyMatch(TestServiceImpl.class::isInstance);
  }

  @Test
  void factoriesAndBeansWhenBeanFactoryIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AotServices.factoriesAndBeans(null))
            .withMessage("'beanFactory' is required");
  }

  @Test
  void factoriesAndBeansLoadsFromFactoriesAndBeanFactory() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.setBeanClassLoader(
            new TestFactoriesClassLoader("aot-services.factories"));
    beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
    AotServices<?> loaded = AotServices.factoriesAndBeans(beanFactory).load(TestService.class);
    assertThat(loaded).anyMatch(TestServiceImpl.class::isInstance);
    assertThat(loaded).anyMatch(TestBean.class::isInstance);
  }

  @Test
  void factoriesAndBeansWithTodayStrategiesLoadsFromTodayStrategiesAndBeanFactory() {
    MockTodayStrategies loader = new MockTodayStrategies();
    loader.addInstance(TestService.class, new TestServiceImpl());
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
    AotServices<?> loaded = AotServices.factoriesAndBeans(loader, beanFactory).load(TestService.class);
    assertThat(loaded).anyMatch(TestServiceImpl.class::isInstance);
    assertThat(loaded).anyMatch(TestBean.class::isInstance);
  }

  @Test
  void factoriesAndBeansWithTodayStrategiesWhenTodayStrategiesIsNullThrowsException() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AotServices.factoriesAndBeans(null, beanFactory))
            .withMessage("'strategies' is required");
  }

  @Test
  void iteratorReturnsServicesIterator() {
    AotServices<?> loaded = AotServices
            .factories(new TestFactoriesClassLoader("aot-services.factories"))
            .load(TestService.class);
    assertThat(loaded.iterator().next()).isInstanceOf(TestServiceImpl.class);
  }

  @Test
  void streamReturnsServicesStream() {
    AotServices<?> loaded = AotServices
            .factories(new TestFactoriesClassLoader("aot-services.factories"))
            .load(TestService.class);
    assertThat(loaded.stream()).anyMatch(TestServiceImpl.class::isInstance);
  }

  @Test
  void asListReturnsServicesList() {
    AotServices<?> loaded = AotServices
            .factories(new TestFactoriesClassLoader("aot-services.factories"))
            .load(TestService.class);
    assertThat(loaded.asList()).anyMatch(TestServiceImpl.class::isInstance);
  }

  @Test
  void findByBeanNameWhenMatchReturnsService() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
    AotServices<?> loaded = AotServices.factoriesAndBeans(beanFactory).load(TestService.class);
    assertThat(loaded.findByBeanName("test")).isInstanceOf(TestBean.class);
  }

  @Test
  void findByBeanNameWhenNoMatchReturnsNull() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
    AotServices<?> loaded = AotServices.factoriesAndBeans(beanFactory).load(TestService.class);
    assertThat(loaded.findByBeanName("missing")).isNull();
  }

  @Test
  void loadLoadsFromBeanFactoryAndTodayStrategiesInOrder() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("b1", new TestServiceImpl(0, "b1"));
    beanFactory.registerSingleton("b2", new TestServiceImpl(2, "b2"));
    MockTodayStrategies todayStrategies = new MockTodayStrategies();
    todayStrategies.addInstance(TestService.class,
            new TestServiceImpl(1, "l1"));
    todayStrategies.addInstance(TestService.class,
            new TestServiceImpl(3, "l2"));
    Iterable<TestService> loaded = AotServices
            .factoriesAndBeans(todayStrategies, beanFactory)
            .load(TestService.class);
    assertThat(loaded).map(Object::toString).containsExactly("b1", "l1", "b2", "l2");
  }

  @Test
  void getSourceReturnsSource() {
    MockTodayStrategies loader = new MockTodayStrategies();
    loader.addInstance(TestService.class, new TestServiceImpl());
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
    AotServices<TestService> loaded = AotServices.factoriesAndBeans(loader, beanFactory).load(TestService.class);
    assertThat(loaded.getSource(loaded.asList().get(0))).isEqualTo(AotServices.Source.INFRA_SPI);
    assertThat(loaded.getSource(loaded.asList().get(1))).isEqualTo(AotServices.Source.BEAN_FACTORY);
    TestService missing = mock();
    assertThatIllegalStateException().isThrownBy(() -> loaded.getSource(missing));
  }

  @Test
  void getSourceWhenMissingThrowsException() {
    AotServices<TestService> loaded = AotServices.factories().load(TestService.class);
    TestService missing = mock();
    assertThatIllegalStateException().isThrownBy(() -> loaded.getSource(missing));
  }

  interface TestService {
  }

  static class TestServiceImpl implements TestService, Ordered {

    private final int order;

    private final String name;

    TestServiceImpl() {
      this(0, "test");
    }

    TestServiceImpl(int order, String name) {
      this.order = order;
      this.name = name;
    }

    @Override
    public int getOrder() {
      return this.order;
    }

    @Override
    public String toString() {
      return this.name;
    }

  }

  static class TestBean implements TestService {

  }

  static class TestFactoriesClassLoader extends ClassLoader {

    private final String factoriesName;

    TestFactoriesClassLoader(String factoriesName) {
      super(Thread.currentThread().getContextClassLoader());
      this.factoriesName = factoriesName;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
      return (!"META-INF/config/aot.factories".equals(name) ?
              super.getResources(name) :
              super.getResources("cn/taketoday/beans/factory/aot/" + this.factoriesName));
    }

  }

}
