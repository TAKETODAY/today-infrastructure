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

package infra.beans.factory.xml;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 */
public class SimplePropertyNamespaceHandlerTests {

  @Test
  public void simpleBeanConfigured() throws Exception {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
            new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
    ITestBean rob = (TestBean) beanFactory.getBean("rob");
    ITestBean sally = (TestBean) beanFactory.getBean("sally");
    assertThat(rob.getName()).isEqualTo("Rob Harrop");
    assertThat(rob.getAge()).isEqualTo(24);
    assertThat(sally).isEqualTo(rob.getSpouse());
  }

  @Test
  public void innerBeanConfigured() throws Exception {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
            new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
    TestBean sally = (TestBean) beanFactory.getBean("sally2");
    ITestBean rob = sally.getSpouse();
    assertThat(rob.getName()).isEqualTo("Rob Harrop");
    assertThat(rob.getAge()).isEqualTo(24);
    assertThat(sally).isEqualTo(rob.getSpouse());
  }

  @Test
  public void withPropertyDefinedTwice() throws Exception {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
            new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
                    new ClassPathResource("simplePropertyNamespaceHandlerTestsWithErrors.xml", getClass())));
  }

  @Test
  public void propertyWithNameEndingInRef() throws Exception {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
            new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
    ITestBean sally = (TestBean) beanFactory.getBean("derivedSally");
    assertThat(sally.getSpouse().getName()).isEqualTo("r");
  }

}
