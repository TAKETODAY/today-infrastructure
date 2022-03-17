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

package cn.taketoday.aop.scope;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.testfixture.SimpleMapScope;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ScopedProxyTests {

  private static final Class<?> CLASS = ScopedProxyTests.class;
  private static final String CLASSNAME = CLASS.getSimpleName();

  private static final ClassPathResource LIST_CONTEXT = new ClassPathResource(CLASSNAME + "-list.xml", CLASS);
  private static final ClassPathResource MAP_CONTEXT = new ClassPathResource(CLASSNAME + "-map.xml", CLASS);
  private static final ClassPathResource OVERRIDE_CONTEXT = new ClassPathResource(CLASSNAME + "-override.xml", CLASS);
  private static final ClassPathResource TESTBEAN_CONTEXT = new ClassPathResource(CLASSNAME + "-testbean.xml", CLASS);

  @Test  // SPR-2108
  public void testProxyAssignable() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(MAP_CONTEXT);
    Object baseMap = bf.getBean("singletonMap");
    boolean condition = baseMap instanceof Map;
    assertThat(condition).isTrue();
  }

  @Test
  public void testSimpleProxy() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(MAP_CONTEXT);
    Object simpleMap = bf.getBean("simpleMap");
    boolean condition1 = simpleMap instanceof Map;
    assertThat(condition1).isTrue();
    boolean condition = simpleMap instanceof HashMap;
    assertThat(condition).isTrue();
  }

  @Test
  public void testScopedOverride() throws Exception {
    GenericApplicationContext ctx = new GenericApplicationContext();
    new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(OVERRIDE_CONTEXT);
    SimpleMapScope scope = new SimpleMapScope();
    ctx.getBeanFactory().registerScope("request", scope);
    ctx.refresh();

    ITestBean bean = (ITestBean) ctx.getBean("testBean");
    assertThat(bean.getName()).isEqualTo("male");
    assertThat(bean.getAge()).isEqualTo(99);

    assertThat(scope.getMap().containsKey("scopedTarget.testBean")).isTrue();
    assertThat(scope.getMap().get("scopedTarget.testBean").getClass()).isEqualTo(TestBean.class);
  }

  @Test
  public void testJdkScopedProxy() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(TESTBEAN_CONTEXT);
    bf.setSerializationId("X");
    SimpleMapScope scope = new SimpleMapScope();
    bf.registerScope("request", scope);

    ITestBean bean = (ITestBean) bf.getBean("testBean");
    assertThat(bean).isNotNull();
    assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
    boolean condition1 = bean instanceof ScopedObject;
    assertThat(condition1).isTrue();
    ScopedObject scoped = (ScopedObject) bean;
    assertThat(scoped.getTargetObject().getClass()).isEqualTo(TestBean.class);
    bean.setAge(101);

    assertThat(scope.getMap().containsKey("testBeanTarget")).isTrue();
    assertThat(scope.getMap().get("testBeanTarget").getClass()).isEqualTo(TestBean.class);

    ITestBean deserialized = SerializationTestUtils.serializeAndDeserialize(bean);
    assertThat(deserialized).isNotNull();
    assertThat(AopUtils.isJdkDynamicProxy(deserialized)).isTrue();
    assertThat(bean.getAge()).isEqualTo(101);
    boolean condition = deserialized instanceof ScopedObject;
    assertThat(condition).isTrue();
    ScopedObject scopedDeserialized = (ScopedObject) deserialized;
    assertThat(scopedDeserialized.getTargetObject().getClass()).isEqualTo(TestBean.class);

    bf.setSerializationId(null);
  }

  @Test
  public void testCglibScopedProxy() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(LIST_CONTEXT);
    bf.setSerializationId("Y");
    SimpleMapScope scope = new SimpleMapScope();
    bf.registerScope("request", scope);

    TestBean tb = (TestBean) bf.getBean("testBean");
    assertThat(AopUtils.isCglibProxy(tb.getFriends())).isTrue();
    boolean condition1 = tb.getFriends() instanceof ScopedObject;
    assertThat(condition1).isTrue();
    ScopedObject scoped = (ScopedObject) tb.getFriends();
    assertThat(scoped.getTargetObject().getClass()).isEqualTo(ArrayList.class);
    tb.getFriends().add("myFriend");

    assertThat(scope.getMap().containsKey("scopedTarget.scopedList")).isTrue();
    assertThat(scope.getMap().get("scopedTarget.scopedList").getClass()).isEqualTo(ArrayList.class);

    ArrayList<?> deserialized = (ArrayList<?>) SerializationTestUtils.serializeAndDeserialize(tb.getFriends());
    assertThat(deserialized).isNotNull();
    assertThat(AopUtils.isCglibProxy(deserialized)).isTrue();
    assertThat(deserialized.contains("myFriend")).isTrue();
    boolean condition = deserialized instanceof ScopedObject;
    assertThat(condition).isTrue();
    ScopedObject scopedDeserialized = (ScopedObject) deserialized;
    assertThat(scopedDeserialized.getTargetObject().getClass()).isEqualTo(ArrayList.class);

    bf.setSerializationId(null);
  }

}
