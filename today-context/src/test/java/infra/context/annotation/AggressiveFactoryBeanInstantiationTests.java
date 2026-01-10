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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Andy Wilkinson
 * @author Liu Dongmiao
 */
public class AggressiveFactoryBeanInstantiationTests {

  @Test
  public void directlyRegisteredFactoryBean() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(SimpleFactoryBean.class);
    context.addBeanFactoryPostProcessor(factory ->
            BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory, String.class)
    );
    context.refresh();
    context.close();
  }

  @Test
  public void beanMethodFactoryBean() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(BeanMethodConfiguration.class);
    context.addBeanFactoryPostProcessor(factory ->
            BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory, String.class)
    );
    context.refresh();
  }

  @Test
  public void checkLinkageError() {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
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
