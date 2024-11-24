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

package infra.ejb.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.naming.NoInitialContextException;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.RuntimeBeanReference;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.ITestBean;
import infra.context.support.GenericApplicationContext;
import infra.core.io.ClassPathResource;
import infra.jndi.JndiObjectFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Oliver Gierke
 */
public class JeeNamespaceHandlerTests {

  private ConfigurableBeanFactory beanFactory;


  @BeforeEach
  public void setup() {
    GenericApplicationContext ctx = new GenericApplicationContext();
    new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(
            new ClassPathResource("jeeNamespaceHandlerTests.xml", getClass()));
    ctx.refresh();
    this.beanFactory = ctx.getBeanFactory();
    this.beanFactory.getBeanNamesForType(ITestBean.class);
  }


  @Test
  public void testSimpleDefinition() {
    BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("simple");
    assertThat(beanDefinition.getBeanClassName()).isEqualTo(JndiObjectFactoryBean.class.getName());
    assertPropertyValue(beanDefinition, "jndiName", "jdbc/MyDataSource");
    assertPropertyValue(beanDefinition, "resourceRef", "true");
  }

  @Test
  public void testComplexDefinition() {
    BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("complex");
    assertThat(beanDefinition.getBeanClassName()).isEqualTo(JndiObjectFactoryBean.class.getName());
    assertPropertyValue(beanDefinition, "jndiName", "jdbc/MyDataSource");
    assertPropertyValue(beanDefinition, "resourceRef", "true");
    assertPropertyValue(beanDefinition, "cache", "true");
    assertPropertyValue(beanDefinition, "lookupOnStartup", "true");
    assertPropertyValue(beanDefinition, "exposeAccessContext", "true");
    assertPropertyValue(beanDefinition, "expectedType", "com.myapp.DefaultFoo");
    assertPropertyValue(beanDefinition, "proxyInterface", "com.myapp.Foo");
    assertPropertyValue(beanDefinition, "defaultObject", "myValue");
  }

  @Test
  public void testWithEnvironment() {
    BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("withEnvironment");
    assertPropertyValue(beanDefinition, "jndiEnvironment", "foo=bar");
    assertPropertyValue(beanDefinition, "defaultObject", new RuntimeBeanReference("myBean"));
  }

  @Test
  public void testWithReferencedEnvironment() {
    BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("withReferencedEnvironment");
    assertPropertyValue(beanDefinition, "jndiEnvironment", new RuntimeBeanReference("myEnvironment"));
    assertThat(beanDefinition.getPropertyValues().contains("environmentRef")).isFalse();
  }

  @Test
  public void testSimpleLocalSlsb() {
    BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("simpleLocalEjb");
    assertThat(beanDefinition.getBeanClassName()).isEqualTo(JndiObjectFactoryBean.class.getName());
    assertPropertyValue(beanDefinition, "jndiName", "ejb/MyLocalBean");

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> this.beanFactory.getBean("simpleLocalEjb"))
            .withCauseInstanceOf(NoInitialContextException.class);
  }

  @Test
  public void testSimpleRemoteSlsb() {
    BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("simpleRemoteEjb");
    assertThat(beanDefinition.getBeanClassName()).isEqualTo(JndiObjectFactoryBean.class.getName());
    assertPropertyValue(beanDefinition, "jndiName", "ejb/MyRemoteBean");

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> this.beanFactory.getBean("simpleRemoteEjb"))
            .withCauseInstanceOf(NoInitialContextException.class);
  }

  @Test
  public void testComplexLocalSlsb() {
    BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("complexLocalEjb");
    assertThat(beanDefinition.getBeanClassName()).isEqualTo(JndiObjectFactoryBean.class.getName());
    assertPropertyValue(beanDefinition, "jndiName", "ejb/MyLocalBean");

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> this.beanFactory.getBean("complexLocalEjb"))
            .withCauseInstanceOf(NoInitialContextException.class);
  }

  @Test
  public void testComplexRemoteSlsb() {
    BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("complexRemoteEjb");
    assertThat(beanDefinition.getBeanClassName()).isEqualTo(JndiObjectFactoryBean.class.getName());
    assertPropertyValue(beanDefinition, "jndiName", "ejb/MyRemoteBean");

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> this.beanFactory.getBean("complexRemoteEjb"))
            .withCauseInstanceOf(NoInitialContextException.class);
  }

  @Test
  public void testLazyInitJndiLookup() {
    BeanDefinition definition = this.beanFactory.getMergedBeanDefinition("lazyDataSource");
    assertThat(definition.isLazyInit()).isTrue();
    definition = this.beanFactory.getMergedBeanDefinition("lazyLocalBean");
    assertThat(definition.isLazyInit()).isTrue();
    definition = this.beanFactory.getMergedBeanDefinition("lazyRemoteBean");
    assertThat(definition.isLazyInit()).isTrue();
  }

  private void assertPropertyValue(BeanDefinition beanDefinition, String propertyName, Object expectedValue) {
    assertThat(beanDefinition.getPropertyValues().getPropertyValue(propertyName)).as("Property '" + propertyName + "' incorrect").isEqualTo(expectedValue);
  }

}
