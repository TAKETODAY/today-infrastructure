/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.UnsatisfiedDependencyException;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.context.ConfigurableApplicationContext;
import infra.core.testfixture.DisabledIfInContinuousIntegration;
import infra.lang.TodayStrategies;
import infra.scheduling.concurrent.ThreadPoolTaskExecutor;

import static infra.stereotype.Component.Bootstrap.BACKGROUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 */
class BackgroundBootstrapTests {

  @Test
  @Timeout(5)
  void bootstrapWithUnmanagedThread() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(UnmanagedThreadBeanConfig.class);
    ctx.getBean("testBean1", TestBean.class);
    ctx.getBean("testBean2", TestBean.class);
    ctx.close();
  }

  @Test
  @Timeout(5)
  void bootstrapWithUnmanagedThreads() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(UnmanagedThreadsBeanConfig.class);
    ctx.getBean("testBean1", TestBean.class);
    ctx.getBean("testBean2", TestBean.class);
    ctx.getBean("testBean3", TestBean.class);
    ctx.getBean("testBean4", TestBean.class);
    ctx.close();
  }

  @Test
  @Timeout(5)
  @DisabledIfInContinuousIntegration
  void bootstrapWithStrictLockingThread() {
    TodayStrategies.setFlag(StandardBeanFactory.STRICT_LOCKING_PROPERTY_NAME);
    try {
      ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(StrictLockingBeanConfig.class);
      assertThat(ctx.getBean("testBean2", TestBean.class).getSpouse()).isSameAs(ctx.getBean("testBean1"));
      ctx.close();
    }
    finally {
      TodayStrategies.setProperty(StandardBeanFactory.STRICT_LOCKING_PROPERTY_NAME, null);
    }
  }

  @Test
  @Timeout(5)
  void bootstrapWithCircularReference() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(CircularReferenceBeanConfig.class);
    ctx.getBean("testBean1", TestBean.class);
    ctx.getBean("testBean2", TestBean.class);
    ctx.close();
  }

  @Test
  @Timeout(5)
  void bootstrapWithCircularReferenceInSameThread() {
    assertThatExceptionOfType(UnsatisfiedDependencyException.class)
            .isThrownBy(() -> new AnnotationConfigApplicationContext(CircularReferenceInSameThreadBeanConfig.class))
            .withRootCauseInstanceOf(BeanCurrentlyInCreationException.class);
  }

  @Test
  @Timeout(5)
  void bootstrapWithCustomExecutor() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(CustomExecutorBeanConfig.class);
    ctx.getBean("testBean1", TestBean.class);
    ctx.getBean("testBean2", TestBean.class);
    ctx.getBean("testBean3", TestBean.class);
    ctx.getBean("testBean4", TestBean.class);
    ctx.close();
  }

  @Configuration(proxyBeanMethods = false)
  static class UnmanagedThreadBeanConfig {

    @Bean
    public TestBean testBean1(ObjectProvider<TestBean> testBean2) {
      new Thread(testBean2::get).start();
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      return new TestBean();
    }

    @Bean
    public TestBean testBean2() {
      try {
        Thread.sleep(2000);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      return new TestBean();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class UnmanagedThreadsBeanConfig {

    @Bean
    public TestBean testBean1(ObjectProvider<TestBean> testBean3, ObjectProvider<TestBean> testBean4) {
      new Thread(testBean3::get).start();
      new Thread(testBean4::get).start();
      new Thread(testBean3::get).start();
      new Thread(testBean4::get).start();
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      return new TestBean();
    }

    @Bean
    public TestBean testBean2(TestBean testBean4) {
      return new TestBean(testBean4);
    }

    @Bean
    public TestBean testBean3(TestBean testBean4) {
      return new TestBean(testBean4);
    }

    @Bean
    public TestBean testBean4() {
      try {
        Thread.sleep(2000);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      return new TestBean();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class StrictLockingBeanConfig {

    @Bean
    public TestBean testBean1(ObjectProvider<TestBean> testBean2) {
      new Thread(testBean2::get).start();
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      return new TestBean("testBean1");
    }

    @Bean
    public TestBean testBean2(ConfigurableBeanFactory beanFactory) {
      return new TestBean((TestBean) beanFactory.getSingleton("testBean1"));
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CircularReferenceBeanConfig {

    @Bean
    public TestBean testBean1(ObjectProvider<TestBean> testBean2) {
      new Thread(testBean2::get).start();
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      return new TestBean();
    }

    @Bean
    public TestBean testBean2(TestBean testBean1) {
      try {
        Thread.sleep(2000);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      return new TestBean();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CircularReferenceInSameThreadBeanConfig {

    @Bean
    public TestBean testBean1(ObjectProvider<TestBean> testBean2) {
      new Thread(testBean2::get).start();
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      return new TestBean();
    }

    @Bean
    public TestBean testBean2(TestBean testBean3) {
      try {
        Thread.sleep(2000);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      return new TestBean();
    }

    @Bean
    public TestBean testBean3(TestBean testBean2) {
      return new TestBean();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomExecutorBeanConfig {

    @Bean
    public ThreadPoolTaskExecutor bootstrapExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setThreadNamePrefix("Custom-");
      executor.setCorePoolSize(2);
      executor.initialize();
      return executor;
    }

    @Bean(bootstrap = BACKGROUND)
    @DependsOn("testBean3")
    public TestBean testBean1(TestBean testBean3) throws InterruptedException {
      Thread.sleep(3000);
      return new TestBean();
    }

    @Bean(bootstrap = BACKGROUND)
    @Lazy
    public TestBean testBean2() throws InterruptedException {
      Thread.sleep(3000);
      return new TestBean();
    }

    @Bean
    @Lazy
    public TestBean testBean3() {
      return new TestBean();
    }

    @Bean
    public TestBean testBean4(@Lazy TestBean testBean1, @Lazy TestBean testBean2, @Lazy TestBean testBean3) {
      return new TestBean();
    }
  }

}
