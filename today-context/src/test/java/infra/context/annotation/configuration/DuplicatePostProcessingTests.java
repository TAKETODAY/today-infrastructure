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
