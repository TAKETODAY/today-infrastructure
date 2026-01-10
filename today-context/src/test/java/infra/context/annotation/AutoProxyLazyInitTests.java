/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import infra.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import infra.aop.framework.autoproxy.target.LazyInitTargetSourceCreator;
import infra.aop.target.AbstractBeanFactoryTargetSource;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.context.event.ApplicationContextEvent;
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
