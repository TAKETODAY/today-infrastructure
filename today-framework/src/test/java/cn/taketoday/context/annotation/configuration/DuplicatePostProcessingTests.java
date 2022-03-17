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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.support.StandardApplicationContext;

/**
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 */
public class DuplicatePostProcessingTests {

  @Test
  public void testWithFactoryBeanAndEventListener() {
    new StandardApplicationContext(Config.class).getBean(ExampleBean.class);
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
