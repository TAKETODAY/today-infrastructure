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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.FactoryMethods;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class FactoryMethodTests {

  @Test
  public void testFactoryMethodsSingletonOnTargetClass() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    TestBean tb = (TestBean) xbf.getBean("defaultTestBean");
    assertThat(tb.getName()).isEqualTo("defaultInstance");
    assertThat(tb.getAge()).isEqualTo(1);

    FactoryMethods fm = (FactoryMethods) xbf.getBean("default");
    assertThat(fm.getNum()).isEqualTo(0);
    assertThat(fm.getName()).isEqualTo("default");
    assertThat(fm.getTestBean().getName()).isEqualTo("defaultInstance");
    assertThat(fm.getStringValue()).isEqualTo("setterString");

    fm = (FactoryMethods) xbf.getBean("testBeanOnly");
    assertThat(fm.getNum()).isEqualTo(0);
    assertThat(fm.getName()).isEqualTo("default");
    // This comes from the test bean
    assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");

    fm = (FactoryMethods) xbf.getBean("full");
    assertThat(fm.getNum()).isEqualTo(27);
    assertThat(fm.getName()).isEqualTo("gotcha");
    assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");

    FactoryMethods fm2 = (FactoryMethods) xbf.getBean("full");
    assertThat(fm2).isSameAs(fm);

    xbf.destroySingletons();
    assertThat(tb.wasDestroyed()).isTrue();
  }

  @Test
  public void testFactoryMethodsWithInvalidDestroyMethod() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            xbf.getBean("defaultTestBeanWithInvalidDestroyMethod"));
  }

  @Test
  public void testFactoryMethodsWithNullInstance() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    assertThat(xbf.getBean("null")).isNull();
    assertThat(xbf.getBean("nullWithProperty")).isNull();
  }

  @Test
  public void testFactoryMethodsWithNullValue() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    FactoryMethods fm = (FactoryMethods) xbf.getBean("fullWithNull");
    assertThat(fm.getNum()).isEqualTo(27);
    assertThat(fm.getName()).isNull();
    assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");

    fm = (FactoryMethods) xbf.getBean("fullWithGenericNull");
    assertThat(fm.getNum()).isEqualTo(27);
    assertThat(fm.getName()).isNull();
    assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");

    fm = (FactoryMethods) xbf.getBean("fullWithNamedNull");
    assertThat(fm.getNum()).isEqualTo(27);
    assertThat(fm.getName()).isNull();
    assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");
  }

  @Test
  public void testFactoryMethodsWithAutowire() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    FactoryMethods fm = (FactoryMethods) xbf.getBean("fullWithAutowire");
    assertThat(fm.getNum()).isEqualTo(27);
    assertThat(fm.getName()).isEqualTo("gotchaAutowired");
    assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");
  }

  @Test
  public void testProtectedFactoryMethod() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    TestBean tb = (TestBean) xbf.getBean("defaultTestBean.protected");
    assertThat(tb.getAge()).isEqualTo(1);
  }

  @Test
  public void testPrivateFactoryMethod() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    TestBean tb = (TestBean) xbf.getBean("defaultTestBean.private");
    assertThat(tb.getAge()).isEqualTo(1);
  }

  @Test
  public void testFactoryMethodsPrototypeOnTargetClass() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
    FactoryMethods fm = (FactoryMethods) xbf.getBean("defaultPrototype");
    FactoryMethods fm2 = (FactoryMethods) xbf.getBean("defaultPrototype");
    assertThat(fm.getNum()).isEqualTo(0);
    assertThat(fm.getName()).isEqualTo("default");
    assertThat(fm.getTestBean().getName()).isEqualTo("defaultInstance");
    assertThat(fm.getStringValue()).isEqualTo("setterString");
    assertThat(fm2.getNum()).isEqualTo(fm.getNum());
    assertThat(fm2.getStringValue()).isEqualTo(fm.getStringValue());
    // The TestBean is created separately for each bean
    assertThat(fm2.getTestBean()).isNotSameAs(fm.getTestBean());
    assertThat(fm2).isNotSameAs(fm);

    fm = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype");
    fm2 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype");
    assertThat(fm.getNum()).isEqualTo(0);
    assertThat(fm.getName()).isEqualTo("default");
    // This comes from the test bean
    assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");
    assertThat(fm2.getNum()).isEqualTo(fm.getNum());
    assertThat(fm2.getStringValue()).isEqualTo(fm.getStringValue());
    // The TestBean reference is resolved to a prototype in the factory
    assertThat(fm2.getTestBean()).isSameAs(fm.getTestBean());
    assertThat(fm2).isNotSameAs(fm);

    fm = (FactoryMethods) xbf.getBean("fullPrototype");
    fm2 = (FactoryMethods) xbf.getBean("fullPrototype");
    assertThat(fm.getNum()).isEqualTo(27);
    assertThat(fm.getName()).isEqualTo("gotcha");
    assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");
    assertThat(fm2.getNum()).isEqualTo(fm.getNum());
    assertThat(fm2.getStringValue()).isEqualTo(fm.getStringValue());
    // The TestBean reference is resolved to a prototype in the factory
    assertThat(fm2.getTestBean()).isSameAs(fm.getTestBean());
    assertThat(fm2).isNotSameAs(fm);
  }

  /**
   * Tests where the static factory method is on a different class.
   */
  @Test
  public void testFactoryMethodsOnExternalClass() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    assertThat(xbf.getType("externalFactoryMethodWithoutArgs")).isEqualTo(TestBean.class);
    assertThat(xbf.getType("externalFactoryMethodWithArgs")).isEqualTo(TestBean.class);
    String[] names = StringUtils.toStringArray(xbf.getBeanNamesForType(TestBean.class));
    assertThat(Arrays.asList(names).contains("externalFactoryMethodWithoutArgs")).isTrue();
    assertThat(Arrays.asList(names).contains("externalFactoryMethodWithArgs")).isTrue();

    TestBean tb = (TestBean) xbf.getBean("externalFactoryMethodWithoutArgs");
    assertThat(tb.getAge()).isEqualTo(2);
    assertThat(tb.getName()).isEqualTo("Tristan");
    tb = (TestBean) xbf.getBean("externalFactoryMethodWithArgs");
    assertThat(tb.getAge()).isEqualTo(33);
    assertThat(tb.getName()).isEqualTo("Rod");

    assertThat(xbf.getType("externalFactoryMethodWithoutArgs")).isEqualTo(TestBean.class);
    assertThat(xbf.getType("externalFactoryMethodWithArgs")).isEqualTo(TestBean.class);
    names = StringUtils.toStringArray(xbf.getBeanNamesForType(TestBean.class));
    assertThat(Arrays.asList(names).contains("externalFactoryMethodWithoutArgs")).isTrue();
    assertThat(Arrays.asList(names).contains("externalFactoryMethodWithArgs")).isTrue();
  }

  @Test
  public void testInstanceFactoryMethodWithoutArgs() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    InstanceFactory.count = 0;
    xbf.preInstantiateSingletons();
    assertThat(InstanceFactory.count).isEqualTo(1);
    FactoryMethods fm = (FactoryMethods) xbf.getBean("instanceFactoryMethodWithoutArgs");
    assertThat(fm.getTestBean().getName()).isEqualTo("instanceFactory");
    assertThat(InstanceFactory.count).isEqualTo(1);
    FactoryMethods fm2 = (FactoryMethods) xbf.getBean("instanceFactoryMethodWithoutArgs");
    assertThat(fm2.getTestBean().getName()).isEqualTo("instanceFactory");
    assertThat(fm).isSameAs(fm2);
    assertThat(InstanceFactory.count).isEqualTo(1);
  }

  @Test
  public void testFactoryMethodNoMatchingStaticMethod() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
    assertThatExceptionOfType(BeanCreationException.class).as("No static method matched").isThrownBy(() ->
            xbf.getBean("noMatchPrototype"));
  }

  @Test
  public void testNonExistingFactoryMethod() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    xbf.getBean("invalidPrototype"))
            .withMessageContaining("nonExisting(TestBean)");
  }

  @Test
  public void testFactoryMethodArgumentsForNonExistingMethod() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    xbf.getBean("invalidPrototype", new TestBean()))
            .withMessageContaining("nonExisting(TestBean)");
  }

  @Test
  public void testCanSpecifyFactoryMethodArgumentsOnFactoryMethodPrototype() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
    TestBean tbArg = new TestBean();
    tbArg.setName("arg1");
    TestBean tbArg2 = new TestBean();
    tbArg2.setName("arg2");

    FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg);
    assertThat(fm1.getNum()).isEqualTo(0);
    assertThat(fm1.getName()).isEqualTo("default");
    // This comes from the test bean
    assertThat(fm1.getTestBean().getName()).isEqualTo("arg1");

    FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg2);
    assertThat(fm2.getTestBean().getName()).isEqualTo("arg2");
    assertThat(fm2.getNum()).isEqualTo(fm1.getNum());
    assertThat("testBeanOnlyPrototypeDISetterString").isEqualTo(fm2.getStringValue());
    assertThat(fm2.getStringValue()).isEqualTo(fm2.getStringValue());
    // The TestBean reference is resolved to a prototype in the factory
    assertThat(fm2.getTestBean()).isSameAs(fm2.getTestBean());
    assertThat(fm2).isNotSameAs(fm1);

    FactoryMethods fm3 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg2, 1, "myName");
    assertThat(fm3.getNum()).isEqualTo(1);
    assertThat(fm3.getName()).isEqualTo("myName");
    assertThat(fm3.getTestBean().getName()).isEqualTo("arg2");

    FactoryMethods fm4 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg);
    assertThat(fm4.getNum()).isEqualTo(0);
    assertThat(fm4.getName()).isEqualTo("default");
    assertThat(fm4.getTestBean().getName()).isEqualTo("arg1");
  }

  @Test
  public void testCanSpecifyFactoryMethodArgumentsOnSingleton() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    // First getBean call triggers actual creation of the singleton bean
    TestBean tb = new TestBean();
    FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnly", tb);
    assertThat(fm1.getTestBean()).isSameAs(tb);
    FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnly", new TestBean());
    assertThat(fm2).isSameAs(fm1);
    assertThat(fm2.getTestBean()).isSameAs(tb);
  }

  @Test
  public void testCannotSpecifyFactoryMethodArgumentsOnSingletonAfterCreation() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    // First getBean call triggers actual creation of the singleton bean
    FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnly");
    TestBean tb = fm1.getTestBean();
    FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnly", new TestBean());
    assertThat(fm2).isSameAs(fm1);
    assertThat(fm2.getTestBean()).isSameAs(tb);
  }

  @Test
  public void testFactoryMethodWithDifferentReturnType() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    // Check that listInstance is not considered a bean of type FactoryMethods.
    assertThat(List.class.isAssignableFrom(xbf.getType("listInstance"))).isTrue();
    String[] names = StringUtils.toStringArray(xbf.getBeanNamesForType(FactoryMethods.class));
    boolean condition1 = !Arrays.asList(names).contains("listInstance");
    assertThat(condition1).isTrue();
    names = StringUtils.toStringArray(xbf.getBeanNamesForType(List.class));
    assertThat(Arrays.asList(names).contains("listInstance")).isTrue();

    xbf.preInstantiateSingletons();
    assertThat(List.class.isAssignableFrom(xbf.getType("listInstance"))).isTrue();
    names = StringUtils.toStringArray(xbf.getBeanNamesForType(FactoryMethods.class));
    boolean condition = !Arrays.asList(names).contains("listInstance");
    assertThat(condition).isTrue();
    names = StringUtils.toStringArray(xbf.getBeanNamesForType(List.class));
    assertThat(Arrays.asList(names).contains("listInstance")).isTrue();
    List<?> list = (List<?>) xbf.getBean("listInstance");
    assertThat(list).isEqualTo(Collections.EMPTY_LIST);
  }

  @Test
  public void testFactoryMethodForJavaMailSession() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

    MailSession session = (MailSession) xbf.getBean("javaMailSession");
    assertThat(session.getProperty("mail.smtp.user")).isEqualTo("someuser");
    assertThat(session.getProperty("mail.smtp.password")).isEqualTo("somepw");
  }
}

