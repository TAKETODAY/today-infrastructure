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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/8 23:51
 */
@Execution(ExecutionMode.SAME_THREAD)
public class ComponentScanParserBeanDefinitionDefaultsTests {

  private static final String TEST_BEAN_NAME = "componentScanParserBeanDefinitionDefaultsTests.DefaultsTestBean";

  private static final String LOCATION_PREFIX = "cn/taketoday/context/annotation/";

  @BeforeEach
  public void setUp() {
    DefaultsTestBean.INIT_COUNT = 0;
  }

  @Test
  public void testDefaultLazyInit() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
    assertThat(context.getBeanDefinition(TEST_BEAN_NAME).isLazyInit()).as("lazy-init should be false").isFalse();
    assertThat(DefaultsTestBean.INIT_COUNT).as("initCount should be 0").isEqualTo(0);
    context.refresh();
    assertThat(DefaultsTestBean.INIT_COUNT).as("bean should have been instantiated").isEqualTo(1);
  }

  @Test
  public void testLazyInitTrue() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultLazyInitTrueTests.xml");
    assertThat(context.getBeanDefinition(TEST_BEAN_NAME).isLazyInit()).as("lazy-init should be true").isTrue();
    assertThat(DefaultsTestBean.INIT_COUNT).as("initCount should be 0").isEqualTo(0);
    context.refresh();
    assertThat(DefaultsTestBean.INIT_COUNT).as("bean should not have been instantiated yet").isEqualTo(0);
    context.getBean(TEST_BEAN_NAME);
    assertThat(DefaultsTestBean.INIT_COUNT).as("bean should have been instantiated").isEqualTo(1);
  }

  @Test
  public void testLazyInitFalse() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultLazyInitFalseTests.xml");
    assertThat(context.getBeanDefinition(TEST_BEAN_NAME).isLazyInit()).as("lazy-init should be false").isFalse();
    assertThat(DefaultsTestBean.INIT_COUNT).as("initCount should be 0").isEqualTo(0);
    context.refresh();
    assertThat(DefaultsTestBean.INIT_COUNT).as("bean should have been instantiated").isEqualTo(1);
  }

  @Test
  public void testDefaultAutowire() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
    context.refresh();
    DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
    assertThat(bean.getConstructorDependency()).as("no dependencies should have been autowired").isNull();
    assertThat(bean.getPropertyDependency1()).as("no dependencies should have been autowired").isNull();
    assertThat(bean.getPropertyDependency2()).as("no dependencies should have been autowired").isNull();
  }

  @Test
  public void testAutowireNo() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireNoTests.xml");
    context.refresh();
    DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
    assertThat(bean.getConstructorDependency()).as("no dependencies should have been autowired").isNull();
    assertThat(bean.getPropertyDependency1()).as("no dependencies should have been autowired").isNull();
    assertThat(bean.getPropertyDependency2()).as("no dependencies should have been autowired").isNull();
  }

  @Test
  public void testAutowireConstructor() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireConstructorTests.xml");
    context.refresh();
    DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
    assertThat(bean.getConstructorDependency()).as("constructor dependency should have been autowired").isNotNull();
    assertThat(bean.getConstructorDependency().getName()).isEqualTo("cd");
    assertThat(bean.getPropertyDependency1()).as("property dependencies should not have been autowired").isNull();
    assertThat(bean.getPropertyDependency2()).as("property dependencies should not have been autowired").isNull();
  }

  @Test
  public void testAutowireByType() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireByTypeTests.xml");
    assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(
            context::refresh);
  }

  @Test
  public void testAutowireByName() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireByNameTests.xml");
    context.refresh();
    DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
    assertThat(bean.getConstructorDependency()).as("constructor dependency should not have been autowired").isNull();
    assertThat(bean.getPropertyDependency1()).as("propertyDependency1 should not have been autowired").isNull();
    assertThat(bean.getPropertyDependency2()).as("propertyDependency2 should have been autowired").isNotNull();
    assertThat(bean.getPropertyDependency2().getName()).isEqualTo("pd2");
  }

  @Test
  public void testDefaultDependencyCheck() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
    context.refresh();
    DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
    assertThat(bean.getConstructorDependency()).as("constructor dependency should not have been autowired").isNull();
    assertThat(bean.getPropertyDependency1()).as("property dependencies should not have been autowired").isNull();
    assertThat(bean.getPropertyDependency2()).as("property dependencies should not have been autowired").isNull();
  }

  @Test
  public void testDefaultInitAndDestroyMethodsNotDefined() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
    context.refresh();
    DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
    assertThat(bean.isInitialized()).as("bean should not have been initialized").isFalse();
    context.close();
    assertThat(bean.isDestroyed()).as("bean should not have been destroyed").isFalse();
  }

  @Test
  public void testDefaultInitAndDestroyMethodsDefined() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultInitAndDestroyMethodsTests.xml");
    context.refresh();
    DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
    assertThat(bean.isInitialized()).as("bean should have been initialized").isTrue();
    context.close();
    assertThat(bean.isDestroyed()).as("bean should have been destroyed").isTrue();
  }

  @Test
  public void testDefaultNonExistingInitAndDestroyMethodsDefined() {
    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultNonExistingInitAndDestroyMethodsTests.xml");
    context.refresh();
    DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
    assertThat(bean.isInitialized()).as("bean should not have been initialized").isFalse();
    context.close();
    assertThat(bean.isDestroyed()).as("bean should not have been destroyed").isFalse();
  }

  @SuppressWarnings("unused")
  private static class DefaultsTestBean {

    static int INIT_COUNT;

    private ConstructorDependencyTestBean constructorDependency;

    private PropertyDependencyTestBean propertyDependency1;

    private PropertyDependencyTestBean propertyDependency2;

    private boolean initialized;

    private boolean destroyed;

    public DefaultsTestBean() {
      INIT_COUNT++;
    }

    public DefaultsTestBean(ConstructorDependencyTestBean cdtb) {
      this();
      this.constructorDependency = cdtb;
    }

    public void init() {
      this.initialized = true;
    }

    public boolean isInitialized() {
      return this.initialized;
    }

    public void destroy() {
      this.destroyed = true;
    }

    public boolean isDestroyed() {
      return this.destroyed;
    }

    public void setPropertyDependency1(PropertyDependencyTestBean pdtb) {
      this.propertyDependency1 = pdtb;
    }

    public void setPropertyDependency2(PropertyDependencyTestBean pdtb) {
      this.propertyDependency2 = pdtb;
    }

    public ConstructorDependencyTestBean getConstructorDependency() {
      return this.constructorDependency;
    }

    public PropertyDependencyTestBean getPropertyDependency1() {
      return this.propertyDependency1;
    }

    public PropertyDependencyTestBean getPropertyDependency2() {
      return this.propertyDependency2;
    }
  }

  @SuppressWarnings("unused")
  private static class PropertyDependencyTestBean {

    private String name;

    public PropertyDependencyTestBean(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

  @SuppressWarnings("unused")
  private static class ConstructorDependencyTestBean {

    private String name;

    public ConstructorDependencyTestBean(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

}
