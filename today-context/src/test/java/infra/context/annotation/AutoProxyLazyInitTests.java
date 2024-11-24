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

package infra.context.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import infra.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import infra.aop.framework.autoproxy.target.LazyInitTargetSourceCreator;
import infra.aop.target.AbstractBeanFactoryTargetSource;
import infra.context.ConfigurableApplicationContext;
import infra.context.event.ApplicationContextEvent;
import infra.context.ApplicationListener;
import jakarta.annotation.PreDestroy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BeanNameAutoProxyCreator} and
 * {@link LazyInitTargetSourceCreator}.
 *
 * @author Juergen Hoeller
 * @author Arrault Fabien
 * @author Sam Brannen
 */
@Execution(ExecutionMode.SAME_THREAD)
class AutoProxyLazyInitTests {

  @BeforeEach
  void resetBeans() {
    MyBeanImpl.initialized = false;
  }

  @Test
  void withStaticBeanMethod() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithStatic.class);
    MyBean bean = ctx.getBean(MyBean.class);

    assertThat(MyBeanImpl.initialized).isFalse();
    bean.doIt();
    assertThat(MyBeanImpl.initialized).isTrue();

    ctx.close();
  }

  @Test
  void withStaticBeanMethodAndInterface() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithStaticAndInterface.class);
    MyBean bean = ctx.getBean(MyBean.class);

    assertThat(MyBeanImpl.initialized).isFalse();
    bean.doIt();
    assertThat(MyBeanImpl.initialized).isTrue();

    ctx.close();
  }

  @Test
  void withNonStaticBeanMethod() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithNonStatic.class);
    MyBean bean = ctx.getBean(MyBean.class);

    assertThat(MyBeanImpl.initialized).isFalse();
    bean.doIt();
    assertThat(MyBeanImpl.initialized).isTrue();

    ctx.close();
  }

  @Test
  void withNonStaticBeanMethodAndInterface() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithNonStaticAndInterface.class);
    MyBean bean = ctx.getBean(MyBean.class);

    assertThat(MyBeanImpl.initialized).isFalse();
    bean.doIt();
    assertThat(MyBeanImpl.initialized).isTrue();

    ctx.close();
  }

  interface MyBean {

    String doIt();
  }

  static class MyBeanImpl implements MyBean {

    static boolean initialized = false;

    MyBeanImpl() {
      initialized = true;
    }

    @Override
    public String doIt() {
      return "From implementation";
    }

    @PreDestroy
    public void destroy() {
    }
  }

  @Configuration
  static class ConfigWithStatic {

    @Bean
    BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
      BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
      autoProxyCreator.setBeanNames("*");
      autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
      return autoProxyCreator;
    }

    @Bean
    LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
      return new StrictLazyInitTargetSourceCreator();
    }

    @Bean
    @Lazy
    static MyBean myBean() {
      return new MyBeanImpl();
    }
  }

  @Configuration
  static class ConfigWithStaticAndInterface implements ApplicationListener<ApplicationContextEvent> {

    @Bean
    BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
      BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
      autoProxyCreator.setBeanNames("*");
      autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
      return autoProxyCreator;
    }

    @Bean
    LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
      return new StrictLazyInitTargetSourceCreator();
    }

    @Bean
    @Lazy
    static MyBean myBean() {
      return new MyBeanImpl();
    }

    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
    }
  }

  @Configuration
  static class ConfigWithNonStatic {

    @Bean
    BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
      BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
      autoProxyCreator.setBeanNames("*");
      autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
      return autoProxyCreator;
    }

    @Bean
    LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
      return new StrictLazyInitTargetSourceCreator();
    }

    @Bean
    @Lazy
    MyBean myBean() {
      return new MyBeanImpl();
    }
  }

  @Configuration
  static class ConfigWithNonStaticAndInterface implements ApplicationListener<ApplicationContextEvent> {

    @Bean
    BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
      BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
      autoProxyCreator.setBeanNames("*");
      autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
      return autoProxyCreator;
    }

    @Bean
    LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
      return new StrictLazyInitTargetSourceCreator();
    }

    @Bean
    @Lazy
    MyBean myBean() {
      return new MyBeanImpl();
    }

    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
    }
  }

  private static class StrictLazyInitTargetSourceCreator extends LazyInitTargetSourceCreator {

    @Override
    protected AbstractBeanFactoryTargetSource createBeanFactoryTargetSource(Class<?> beanClass, String beanName) {
      if ("myBean".equals(beanName)) {
        assertThat(beanClass).isEqualTo(MyBean.class);
      }
      return super.createBeanFactoryTargetSource(beanClass, beanName);
    }
  }

}
