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

package cn.taketoday.aop.target;

import org.junit.jupiter.api.Test;

import java.util.Set;

import cn.taketoday.aop.testfixture.beans.ITestBean;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.core.io.Resource;

import static cn.taketoday.aop.testfixture.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @since 07.01.2005
 */
public class LazyInitTargetSourceTests {

  private static final Class<?> CLASS = LazyInitTargetSourceTests.class;

  private static final Resource SINGLETON_CONTEXT = qualifiedResource(CLASS, "singleton.xml");
  private static final Resource CUSTOM_TARGET_CONTEXT = qualifiedResource(CLASS, "customTarget.xml");
  private static final Resource FACTORY_BEAN_CONTEXT = qualifiedResource(CLASS, "factoryBean.xml");

  @Test
  public void testLazyInitSingletonTargetSource() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(SINGLETON_CONTEXT);
    bf.preInstantiateSingletons();

    ITestBean tb = (ITestBean) bf.getBean("proxy");
    assertThat(bf.containsSingleton("target")).isFalse();
    assertThat(tb.getAge()).isEqualTo(10);
    assertThat(bf.containsSingleton("target")).isTrue();
  }

  @Test
  public void testCustomLazyInitSingletonTargetSource() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(CUSTOM_TARGET_CONTEXT);
    bf.preInstantiateSingletons();

    ITestBean tb = (ITestBean) bf.getBean("proxy");
    assertThat(bf.containsSingleton("target")).isFalse();
    assertThat(tb.getName()).isEqualTo("Rob Harrop");
    assertThat(bf.containsSingleton("target")).isTrue();
  }

  @Test
  public void testLazyInitFactoryBeanTargetSource() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(FACTORY_BEAN_CONTEXT);
    bf.preInstantiateSingletons();

    Set<?> set1 = (Set<?>) bf.getBean("proxy1");
    assertThat(bf.containsSingleton("target1")).isFalse();
    assertThat(set1.contains("10")).isTrue();
    assertThat(bf.containsSingleton("target1")).isTrue();

    Set<?> set2 = (Set<?>) bf.getBean("proxy2");
    assertThat(bf.containsSingleton("target2")).isFalse();
    assertThat(set2.contains("20")).isTrue();
    assertThat(bf.containsSingleton("target2")).isTrue();
  }

  @SuppressWarnings("serial")
  public static class CustomLazyInitTargetSource extends LazyInitTargetSource {

    @Override
    protected void postProcessTargetObject(Object targetObject) {
      ((ITestBean) targetObject).setName("Rob Harrop");
    }
  }

}
