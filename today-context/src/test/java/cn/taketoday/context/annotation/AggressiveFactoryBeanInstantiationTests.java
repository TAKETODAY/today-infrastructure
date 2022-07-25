/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Andy Wilkinson
 * @author Liu Dongmiao
 */
public class AggressiveFactoryBeanInstantiationTests {

  @Test
  public void directlyRegisteredFactoryBean() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(SimpleFactoryBean.class);
      context.addBeanFactoryPostProcessor(factory ->
              BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory, String.class)
      );
      context.refresh();
    }
  }

  @Test
  public void beanMethodFactoryBean() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(BeanMethodConfiguration.class);
      context.addBeanFactoryPostProcessor(factory ->
              BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory, String.class)
      );
      context.refresh();
    }
  }

  @Test
  public void checkLinkageError() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(BeanMethodConfigurationWithExceptionInInitializer.class);
      context.refresh();
      fail("Should have thrown BeanCreationException");
    }
    catch (BeanCreationException ex) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintWriter pw = new PrintWriter(baos);
      ex.printStackTrace(pw);
      pw.flush();
      String stackTrace = baos.toString();
      assertThat(stackTrace.contains(".<clinit>")).isTrue();
      assertThat(stackTrace.contains("java.lang.NoClassDefFoundError")).isFalse();
    }
  }

  @Configuration
  static class BeanMethodConfiguration {

    @Bean
    public String foo() {
      return "foo";
    }

    @Bean
    public AutowiredBean autowiredBean() {
      return new AutowiredBean();
    }

    @Bean
    @DependsOn("autowiredBean")
    public SimpleFactoryBean simpleFactoryBean(ApplicationContext applicationContext) {
      return new SimpleFactoryBean(applicationContext);
    }
  }

  @Configuration
  static class BeanMethodConfigurationWithExceptionInInitializer extends BeanMethodConfiguration {

    @Bean
    @DependsOn("autowiredBean")
    @Override
    public SimpleFactoryBean simpleFactoryBean(ApplicationContext applicationContext) {
      new ExceptionInInitializer();
      return new SimpleFactoryBean(applicationContext);
    }
  }

  static class AutowiredBean {

    @Autowired
    String foo;
  }

  static class SimpleFactoryBean implements FactoryBean<Object> {

    public SimpleFactoryBean(ApplicationContext applicationContext) {
    }

    @Override
    public Object getObject() {
      return new Object();
    }

    @Override
    public Class<?> getObjectType() {
      return Object.class;
    }
  }

  static class ExceptionInInitializer {

    @SuppressWarnings("unused")
    private static final int ERROR = callInClinit();

    private static int callInClinit() {
      throw new UnsupportedOperationException();
    }
  }

}
