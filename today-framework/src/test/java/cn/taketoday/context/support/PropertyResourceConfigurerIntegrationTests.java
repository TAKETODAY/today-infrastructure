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

package cn.taketoday.context.support;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.beans.factory.config.PropertyPlaceholderConfigurer;
import cn.taketoday.beans.factory.config.PropertyResourceConfigurer;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link PropertyResourceConfigurer} implementations requiring
 * interaction with an {@link ApplicationContext}.  For example, a {@link PropertyPlaceholderConfigurer}
 * that contains ${..} tokens in its 'location' property requires being tested through an ApplicationContext
 * as opposed to using only a BeanFactory during testing.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @see cn.taketoday.beans.factory.config.PropertyResourceConfigurerTests
 */
public class PropertyResourceConfigurerIntegrationTests {

  @Test
  public void testPropertyPlaceholderConfigurerWithSystemPropertyInLocation() {
    StaticApplicationContext ac = new StaticApplicationContext();
    PropertyValues pvs = new PropertyValues();
    pvs.add("spouse", new RuntimeBeanReference("${ref}"));
    ac.registerSingleton("tb", TestBean.class, pvs);
    pvs = new PropertyValues();
    pvs.add("location", "${user.dir}/test");
    ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
    String userDir = getUserDir();
    assertThatExceptionOfType(BeanInitializationException.class)
            .isThrownBy(ac::refresh)
            .withCauseInstanceOf(FileNotFoundException.class)
            .satisfies(ex -> {
              assertThat(ex.getNestedMessage()).contains(userDir);
            });
  }

  @Test
  public void testPropertyPlaceholderConfigurerWithSystemPropertiesInLocation() {
    StaticApplicationContext ac = new StaticApplicationContext();
    PropertyValues pvs = new PropertyValues();
    pvs.add("spouse", new RuntimeBeanReference("${ref}"));
    ac.registerSingleton("tb", TestBean.class, pvs);
    pvs = new PropertyValues();
    pvs.add("location", "${user.dir}/test/${user.dir}");
    ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
    String userDir = getUserDir();
    Assertions.setMaxStackTraceElementsDisplayed(1000);
    assertThatExceptionOfType(BeanInitializationException.class)
            .isThrownBy(ac::refresh)
            .withCauseInstanceOf(FileNotFoundException.class)
            .matches(ex -> ex.getNestedMessage().contains(userDir + "/test/" + userDir) ||
                    ex.getNestedMessage().contains(userDir + "/test//" + userDir));
  }

  private String getUserDir() {
    // slight hack for Linux/Unix systems
    String userDir = StringUtils.cleanPath(System.getProperty("user.dir"));
    if (userDir.startsWith("/")) {
      userDir = userDir.substring(1);
    }
    return userDir;
  }

  @Test
  public void testPropertyPlaceholderConfigurerWithUnresolvableSystemPropertiesInLocation() {
    StaticApplicationContext ac = new StaticApplicationContext();
    PropertyValues pvs = new PropertyValues();
    pvs.add("spouse", new RuntimeBeanReference("${ref}"));
    ac.registerSingleton("tb", TestBean.class, pvs);
    pvs = new PropertyValues();
    pvs.add("location", "${myprop}/test/${myprop}");
    ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
    assertThatExceptionOfType(BeanInitializationException.class)
            .isThrownBy(ac::refresh)
            .satisfies(ex -> {
              assertThat(ex.getNestedMessage()).contains("myprop");
            });
  }

  @Test
  public void testPropertyPlaceholderConfigurerWithMultiLevelCircularReference() {
    StaticApplicationContext ac = new StaticApplicationContext();
    PropertyValues pvs = new PropertyValues();
    pvs.add("name", "name${var}");
    ac.registerSingleton("tb1", TestBean.class, pvs);
    pvs = new PropertyValues();
    pvs.add("properties", "var=${m}var\nm=${var2}\nvar2=${var}");
    ac.registerSingleton("configurer1", PropertyPlaceholderConfigurer.class, pvs);
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(ac::refresh);
  }

  @Test
  public void testPropertyPlaceholderConfigurerWithNestedCircularReference() {
    StaticApplicationContext ac = new StaticApplicationContext();
    PropertyValues pvs = new PropertyValues();
    pvs.add("name", "name${var}");
    ac.registerSingleton("tb1", TestBean.class, pvs);
    pvs = new PropertyValues();
    pvs.add("properties", "var=${m}var\nm=${var2}\nvar2=${m}");
    ac.registerSingleton("configurer1", PropertyPlaceholderConfigurer.class, pvs);
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(ac::refresh);
  }

  @Test
  public void testPropertyPlaceholderConfigurerWithNestedUnresolvableReference() {
    StaticApplicationContext ac = new StaticApplicationContext();
    PropertyValues pvs = new PropertyValues();
    pvs.add("name", "name${var}");
    ac.registerSingleton("tb1", TestBean.class, pvs);
    pvs = new PropertyValues();
    pvs.add("properties", "var=${m}var\nm=${var2}\nvar2=${m2}");
    ac.registerSingleton("configurer1", PropertyPlaceholderConfigurer.class, pvs);
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(ac::refresh);
  }

  @Test
  public void testPropertyPlaceholderConfigurerWithValueFromSystemProperty() {
    final String propertyName = getClass().getName() + ".test";

    try {
      System.setProperty(propertyName, "mytest");

      StaticApplicationContext context = new StaticApplicationContext();

      PropertyValues pvs = new PropertyValues();
      pvs.add("touchy", "${" + propertyName + "}");
      context.registerSingleton("tb", TestBean.class, pvs);

      pvs = new PropertyValues();
      pvs.add("target", new RuntimeBeanReference("tb"));
      context.registerSingleton("tbProxy", cn.taketoday.aop.framework.ProxyFactoryBean.class, pvs);

      context.registerSingleton("configurer", PropertyPlaceholderConfigurer.class);
      context.refresh();

      TestBean testBean = context.getBean("tb", TestBean.class);
      assertThat(testBean.getTouchy()).isEqualTo("mytest");
    }
    finally {
      System.clearProperty(propertyName);
    }
  }

}
