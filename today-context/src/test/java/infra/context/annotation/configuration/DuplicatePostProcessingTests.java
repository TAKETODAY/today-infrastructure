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

package infra.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;

/**
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 */
public class DuplicatePostProcessingTests {

  @Test
  public void testWithFactoryBeanAndEventListener() {
    new AnnotationConfigApplicationContext(Config.class).getBean(ExampleBean.class);
  }

  static class Config {

    @Bean
    public ExampleFactoryBean exampleFactory() {
      return new ExampleFactoryBean();
    }

    @Bean
    public static ExampleBeanPostProcessor exampleBeanPostProcessor() {
      return new ExampleBeanPostProcessor();
    }

    @Bean
    public ExampleApplicationEventListener exampleApplicationEventListener() {
      return new ExampleApplicationEventListener();
    }
  }

  static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

    private final ExampleBean exampleBean = new ExampleBean();

    @Override
    public ExampleBean getObject() {
      return this.exampleBean;
    }

    @Override
    public Class<?> getObjectType() {
      return ExampleBean.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

  static class ExampleBeanPostProcessor implements InitializationBeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
      if (bean instanceof ExampleBean) {
        this.applicationContext.publishEvent(new ExampleApplicationEvent(this));
      }
      return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }
  }

  @SuppressWarnings("serial")
  static class ExampleApplicationEvent extends ApplicationEvent {

    public ExampleApplicationEvent(Object source) {
      super(source);
    }
  }

  static class ExampleApplicationEventListener implements ApplicationListener<ExampleApplicationEvent>, BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void onApplicationEvent(ExampleApplicationEvent event) {
      this.beanFactory.getBean(ExampleBean.class);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }
  }

  static class ExampleBean {
  }

}
