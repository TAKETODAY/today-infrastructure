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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import cn.taketoday.beans.factory.support.FieldRetrievingFactoryBean;
import cn.taketoday.beans.factory.support.PropertiesFactoryBean;
import cn.taketoday.beans.factory.parsing.ComponentDefinition;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.CollectingReaderEventListener;
import cn.taketoday.beans.testfixture.beans.CustomEnum;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.util.LinkedCaseInsensitiveMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 */
@SuppressWarnings("rawtypes")
public class UtilNamespaceHandlerTests {

  private StandardBeanFactory beanFactory;

  private final CollectingReaderEventListener listener = new CollectingReaderEventListener();

  @BeforeEach
  public void setUp() {
    this.beanFactory = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
    reader.setEventListener(this.listener);
    reader.loadBeanDefinitions(new ClassPathResource("testUtilNamespace.xml", getClass()));
  }

  @Test
  public void testConstant() {
    Integer min = (Integer) this.beanFactory.getBean("min");
    assertThat(min.intValue()).isEqualTo(Integer.MIN_VALUE);
  }

  @Test
  public void testConstantWithDefaultName() {
    Integer max = (Integer) this.beanFactory.getBean("java.lang.Integer.MAX_VALUE");
    assertThat(max.intValue()).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void testEvents() {
    ComponentDefinition propertiesComponent = this.listener.getComponentDefinition("myProperties");
    assertThat(propertiesComponent).as("Event for 'myProperties' not sent").isNotNull();
    BeanDefinition propertiesBean = propertiesComponent.getBeanDefinitions()[0];
    assertThat(propertiesBean.getBeanClass()).as("Incorrect BeanDefinition").isEqualTo(PropertiesFactoryBean.class);

    ComponentDefinition constantComponent = this.listener.getComponentDefinition("min");
    assertThat(propertiesComponent).as("Event for 'min' not sent").isNotNull();
    BeanDefinition constantBean = constantComponent.getBeanDefinitions()[0];
    assertThat(constantBean.getBeanClass()).as("Incorrect BeanDefinition").isEqualTo(FieldRetrievingFactoryBean.class);
  }

  @Test
  public void testNestedProperties() {
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
    Properties props = bean.getSomeProperties();
    assertThat(props.get("foo")).as("Incorrect property value").isEqualTo("bar");
  }

  @Test
  public void testPropertyPath() {
    String name = (String) this.beanFactory.getBean("name");
    assertThat(name).isEqualTo("Rob Harrop");
  }

  @Test
  public void testNestedPropertyPath() {
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
    assertThat(bean.getName()).isEqualTo("Rob Harrop");
  }

  @Test
  public void testSimpleMap() {
    Map map = (Map) this.beanFactory.getBean("simpleMap");
    assertThat(map.get("foo")).isEqualTo("bar");
    Map map2 = (Map) this.beanFactory.getBean("simpleMap");
    assertThat(map == map2).isTrue();
  }

  @Test
  public void testScopedMap() {
    Map map = (Map) this.beanFactory.getBean("scopedMap");
    assertThat(map.get("foo")).isEqualTo("bar");
    Map map2 = (Map) this.beanFactory.getBean("scopedMap");
    assertThat(map2.get("foo")).isEqualTo("bar");
    assertThat(map != map2).isTrue();
  }

  @Test
  public void testSimpleList() {
    List list = (List) this.beanFactory.getBean("simpleList");
    assertThat(list.get(0)).isEqualTo("Rob Harrop");
    List list2 = (List) this.beanFactory.getBean("simpleList");
    assertThat(list == list2).isTrue();
  }

  @Test
  public void testScopedList() {
    List list = (List) this.beanFactory.getBean("scopedList");
    assertThat(list.get(0)).isEqualTo("Rob Harrop");
    List list2 = (List) this.beanFactory.getBean("scopedList");
    assertThat(list2.get(0)).isEqualTo("Rob Harrop");
    assertThat(list != list2).isTrue();
  }

  @Test
  public void testSimpleSet() {
    Set set = (Set) this.beanFactory.getBean("simpleSet");
    assertThat(set.contains("Rob Harrop")).isTrue();
    Set set2 = (Set) this.beanFactory.getBean("simpleSet");
    assertThat(set == set2).isTrue();
  }

  @Test
  public void testScopedSet() {
    Set set = (Set) this.beanFactory.getBean("scopedSet");
    assertThat(set.contains("Rob Harrop")).isTrue();
    Set set2 = (Set) this.beanFactory.getBean("scopedSet");
    assertThat(set2.contains("Rob Harrop")).isTrue();
    assertThat(set != set2).isTrue();
  }

  @Test
  public void testMapWithRef() {
    Map map = (Map) this.beanFactory.getBean("mapWithRef");
    boolean condition = map instanceof TreeMap;
    assertThat(condition).isTrue();
    assertThat(map.get("bean")).isEqualTo(this.beanFactory.getBean("testBean"));
  }

  @Test
  public void testMapWithTypes() {
    Map map = (Map) this.beanFactory.getBean("mapWithTypes");
    boolean condition = map instanceof LinkedCaseInsensitiveMap;
    assertThat(condition).isTrue();
    assertThat(map.get("bean")).isEqualTo(this.beanFactory.getBean("testBean"));
  }

  @Test
  public void testNestedCollections() {
    TestBean bean = (TestBean) this.beanFactory.getBean("nestedCollectionsBean");

    List list = bean.getSomeList();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0)).isEqualTo("foo");

    Set set = bean.getSomeSet();
    assertThat(set.size()).isEqualTo(1);
    assertThat(set.contains("bar")).isTrue();

    Map map = bean.getSomeMap();
    assertThat(map.size()).isEqualTo(1);
    boolean condition = map.get("foo") instanceof Set;
    assertThat(condition).isTrue();
    Set innerSet = (Set) map.get("foo");
    assertThat(innerSet.size()).isEqualTo(1);
    assertThat(innerSet.contains("bar")).isTrue();

    TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedCollectionsBean");
    assertThat(bean2.getSomeList()).isEqualTo(list);
    assertThat(bean2.getSomeSet()).isEqualTo(set);
    assertThat(bean2.getSomeMap()).isEqualTo(map);
    assertThat(list == bean2.getSomeList()).isFalse();
    assertThat(set == bean2.getSomeSet()).isFalse();
    assertThat(map == bean2.getSomeMap()).isFalse();
  }

  @Test
  public void testNestedShortcutCollections() {
    TestBean bean = (TestBean) this.beanFactory.getBean("nestedShortcutCollections");

    assertThat(bean.getStringArray().length).isEqualTo(1);
    assertThat(bean.getStringArray()[0]).isEqualTo("fooStr");

    List list = bean.getSomeList();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0)).isEqualTo("foo");

    Set set = bean.getSomeSet();
    assertThat(set.size()).isEqualTo(1);
    assertThat(set.contains("bar")).isTrue();

    TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedShortcutCollections");
    assertThat(Arrays.equals(bean.getStringArray(), bean2.getStringArray())).isTrue();
    assertThat(bean.getStringArray() == bean2.getStringArray()).isFalse();
    assertThat(bean2.getSomeList()).isEqualTo(list);
    assertThat(bean2.getSomeSet()).isEqualTo(set);
    assertThat(list == bean2.getSomeList()).isFalse();
    assertThat(set == bean2.getSomeSet()).isFalse();
  }

  @Test
  public void testNestedInCollections() {
    TestBean bean = (TestBean) this.beanFactory.getBean("nestedCustomTagBean");

    List list = bean.getSomeList();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0)).isEqualTo(Integer.MIN_VALUE);

    Set set = bean.getSomeSet();
    assertThat(set.size()).isEqualTo(2);
    assertThat(set.contains(Thread.State.NEW)).isTrue();
    assertThat(set.contains(Thread.State.RUNNABLE)).isTrue();

    Map map = bean.getSomeMap();
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get("min")).isEqualTo(CustomEnum.VALUE_1);

    TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedCustomTagBean");
    assertThat(bean2.getSomeList()).isEqualTo(list);
    assertThat(bean2.getSomeSet()).isEqualTo(set);
    assertThat(bean2.getSomeMap()).isEqualTo(map);
    assertThat(list == bean2.getSomeList()).isFalse();
    assertThat(set == bean2.getSomeSet()).isFalse();
    assertThat(map == bean2.getSomeMap()).isFalse();
  }

  @Test
  public void testCircularCollections() {
    TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionsBean");

    List list = bean.getSomeList();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0)).isEqualTo(bean);

    Set set = bean.getSomeSet();
    assertThat(set.size()).isEqualTo(1);
    assertThat(set.contains(bean)).isTrue();

    Map map = bean.getSomeMap();
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get("foo")).isEqualTo(bean);
  }

  @Test
  public void testCircularCollectionBeansStartingWithList() {
    this.beanFactory.getBean("circularList");
    TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

    List list = bean.getSomeList();
    assertThat(Proxy.isProxyClass(list.getClass())).isTrue();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0)).isEqualTo(bean);

    Set set = bean.getSomeSet();
    assertThat(Proxy.isProxyClass(set.getClass())).isFalse();
    assertThat(set.size()).isEqualTo(1);
    assertThat(set.contains(bean)).isTrue();

    Map map = bean.getSomeMap();
    assertThat(Proxy.isProxyClass(map.getClass())).isFalse();
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get("foo")).isEqualTo(bean);
  }

  @Test
  public void testCircularCollectionBeansStartingWithSet() {
    this.beanFactory.getBean("circularSet");
    TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

    List list = bean.getSomeList();
    assertThat(Proxy.isProxyClass(list.getClass())).isFalse();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0)).isEqualTo(bean);

    Set set = bean.getSomeSet();
    assertThat(Proxy.isProxyClass(set.getClass())).isTrue();
    assertThat(set.size()).isEqualTo(1);
    assertThat(set.contains(bean)).isTrue();

    Map map = bean.getSomeMap();
    assertThat(Proxy.isProxyClass(map.getClass())).isFalse();
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get("foo")).isEqualTo(bean);
  }

  @Test
  public void testCircularCollectionBeansStartingWithMap() {
    this.beanFactory.getBean("circularMap");
    TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

    List list = bean.getSomeList();
    assertThat(Proxy.isProxyClass(list.getClass())).isFalse();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0)).isEqualTo(bean);

    Set set = bean.getSomeSet();
    assertThat(Proxy.isProxyClass(set.getClass())).isFalse();
    assertThat(set.size()).isEqualTo(1);
    assertThat(set.contains(bean)).isTrue();

    Map map = bean.getSomeMap();
    assertThat(Proxy.isProxyClass(map.getClass())).isTrue();
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get("foo")).isEqualTo(bean);
  }

  @Test
  public void testNestedInConstructor() {
    TestBean bean = (TestBean) this.beanFactory.getBean("constructedTestBean");
    assertThat(bean.getName()).isEqualTo("Rob Harrop");
  }

  @Test
  public void testLoadProperties() {
    Properties props = (Properties) this.beanFactory.getBean("myProperties");
    assertThat(props.get("foo")).as("Incorrect property value").isEqualTo("bar");
    assertThat(props.get("foo2")).as("Incorrect property value").isNull();
    Properties props2 = (Properties) this.beanFactory.getBean("myProperties");
    assertThat(props == props2).isTrue();
  }

  @Test
  public void testScopedProperties() {
    Properties props = (Properties) this.beanFactory.getBean("myScopedProperties");
    assertThat(props.get("foo")).as("Incorrect property value").isEqualTo("bar");
    assertThat(props.get("foo2")).as("Incorrect property value").isNull();
    Properties props2 = (Properties) this.beanFactory.getBean("myScopedProperties");
    assertThat(props.get("foo")).as("Incorrect property value").isEqualTo("bar");
    assertThat(props.get("foo2")).as("Incorrect property value").isNull();
    assertThat(props != props2).isTrue();
  }

  @Test
  public void testLocalProperties() {
    Properties props = (Properties) this.beanFactory.getBean("myLocalProperties");
    assertThat(props.get("foo")).as("Incorrect property value").isNull();
    assertThat(props.get("foo2")).as("Incorrect property value").isEqualTo("bar2");
  }

  @Test
  public void testMergedProperties() {
    Properties props = (Properties) this.beanFactory.getBean("myMergedProperties");
    assertThat(props.get("foo")).as("Incorrect property value").isEqualTo("bar");
    assertThat(props.get("foo2")).as("Incorrect property value").isEqualTo("bar2");
  }

  @Test
  public void testLocalOverrideDefault() {
    Properties props = (Properties) this.beanFactory.getBean("defaultLocalOverrideProperties");
    assertThat(props.get("foo")).as("Incorrect property value").isEqualTo("bar");
    assertThat(props.get("foo2")).as("Incorrect property value").isEqualTo("local2");
  }

  @Test
  public void testLocalOverrideFalse() {
    Properties props = (Properties) this.beanFactory.getBean("falseLocalOverrideProperties");
    assertThat(props.get("foo")).as("Incorrect property value").isEqualTo("bar");
    assertThat(props.get("foo2")).as("Incorrect property value").isEqualTo("local2");
  }

  @Test
  public void testLocalOverrideTrue() {
    Properties props = (Properties) this.beanFactory.getBean("trueLocalOverrideProperties");
    assertThat(props.get("foo")).as("Incorrect property value").isEqualTo("local");
    assertThat(props.get("foo2")).as("Incorrect property value").isEqualTo("local2");
  }

}
