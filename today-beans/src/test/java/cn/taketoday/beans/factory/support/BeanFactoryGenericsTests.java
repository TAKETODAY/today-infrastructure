/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.config.TypedStringValue;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.beans.propertyeditors.CustomNumberEditor;
import cn.taketoday.beans.testfixture.beans.GenericBean;
import cn.taketoday.beans.testfixture.beans.GenericIntegerBean;
import cn.taketoday.beans.testfixture.beans.GenericSetOfIntegerBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.OverridingClassLoader;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.UrlResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/8 21:27
 */
class BeanFactoryGenericsTests {

  @Test
  void testGenericSetProperty() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Set<String> input = new HashSet<>();
    input.add("4");
    input.add("5");
    rbd.getPropertyValues().add("integerSet", input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet().contains(4)).isTrue();
    assertThat(gb.getIntegerSet().contains(5)).isTrue();
  }

  @Test
  void testGenericListProperty() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    List<String> input = new ArrayList<>();
    input.add("http://localhost:8080");
    input.add("http://localhost:9090");
    rbd.getPropertyValues().add("resourceList", input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
    assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
  }

  @Test
  void testGenericListPropertyWithAutowiring() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
    bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

    RootBeanDefinition rbd = new RootBeanDefinition(GenericIntegerBean.class);
    rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
    bf.registerBeanDefinition("genericBean", rbd);
    GenericIntegerBean gb = (GenericIntegerBean) bf.getBean("genericBean");

    assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
    assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
  }

  @Test
  void testGenericListPropertyWithInvalidElementType() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericIntegerBean.class);

    List<Integer> input = new ArrayList<>();
    input.add(1);
    rbd.getPropertyValues().add("testBeanList", input);

    bf.registerBeanDefinition("genericBean", rbd);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    bf.getBean("genericBean"))
            .withMessageContaining("genericBean")
            .withMessageContaining("testBeanList[0]")
            .withMessageContaining(TestBean.class.getName())
            .withMessageContaining("Integer");
  }

  @Test
  void testGenericListPropertyWithOptionalAutowiring() {
    StandardBeanFactory bf = new StandardBeanFactory();

    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getResourceList()).isNull();
  }

  @Test
  void testGenericMapProperty() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Map<String, String> input = new HashMap<>();
    input.put("4", "5");
    input.put("6", "7");
    rbd.getPropertyValues().add("shortMap", input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
    assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
  }

  @Test
  void testGenericListOfArraysProperty() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            new ClassPathResource("genericBeanTests.xml", getClass()));
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("listOfArrays");

    assertThat(gb.getListOfArrays()).hasSize(1);
    String[] array = gb.getListOfArrays().get(0);
    assertThat(array).hasSize(2);
    assertThat(array[0]).isEqualTo("value1");
    assertThat(array[1]).isEqualTo("value2");
  }

  @Test
  void testGenericSetConstructor() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Set<String> input = new HashSet<>();
    input.add("4");
    input.add("5");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet().contains(4)).isTrue();
    assertThat(gb.getIntegerSet().contains(5)).isTrue();
  }

  @Test
  void testGenericSetConstructorWithAutowiring() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerSingleton("integer1", 4);
    bf.registerSingleton("integer2", 5);

    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet().contains(4)).isTrue();
    assertThat(gb.getIntegerSet().contains(5)).isTrue();
  }

  @Test
  void testGenericSetConstructorWithOptionalAutowiring() {
    StandardBeanFactory bf = new StandardBeanFactory();

    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet()).isNull();
  }

  @Test
  void testGenericSetListConstructor() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Set<String> input = new HashSet<>();
    input.add("4");
    input.add("5");
    List<String> input2 = new ArrayList<>();
    input2.add("http://localhost:8080");
    input2.add("http://localhost:9090");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet().contains(4)).isTrue();
    assertThat(gb.getIntegerSet().contains(5)).isTrue();
    assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
    assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
  }

  @Test
  void testGenericSetListConstructorWithAutowiring() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerSingleton("integer1", 4);
    bf.registerSingleton("integer2", 5);
    bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
    bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet().contains(4)).isTrue();
    assertThat(gb.getIntegerSet().contains(5)).isTrue();
    assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
    assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
  }

  @Test
  void testGenericSetListConstructorWithOptionalAutowiring() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
    bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet()).isNull();
    assertThat(gb.getResourceList()).isNull();
  }

  @Test
  void testGenericSetMapConstructor() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Set<String> input = new HashSet<>();
    input.add("4");
    input.add("5");
    Map<String, String> input2 = new HashMap<>();
    input2.put("4", "5");
    input2.put("6", "7");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet().contains(4)).isTrue();
    assertThat(gb.getIntegerSet().contains(5)).isTrue();
    assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
    assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
  }

  @Test
  void testGenericMapResourceConstructor() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Map<String, String> input = new HashMap<>();
    input.put("4", "5");
    input.put("6", "7");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
    rbd.getConstructorArgumentValues().addGenericArgumentValue("http://localhost:8080");

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
    assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
    assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
  }

  @Test
  void testGenericMapMapConstructor() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Map<String, String> input = new HashMap<>();
    input.put("1", "0");
    input.put("2", "3");
    Map<String, String> input2 = new HashMap<>();
    input2.put("4", "5");
    input2.put("6", "7");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getShortMap()).isNotSameAs(gb.getPlainMap());
    assertThat(gb.getPlainMap()).hasSize(2);
    assertThat(gb.getPlainMap().get("1")).isEqualTo("0");
    assertThat(gb.getPlainMap().get("2")).isEqualTo("3");
    assertThat(gb.getShortMap()).hasSize(2);
    assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
    assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
  }

  @Test
  void testGenericMapMapConstructorWithSameRefAndConversion() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Map<String, String> input = new HashMap<>();
    input.put("1", "0");
    input.put("2", "3");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getShortMap()).isNotSameAs(gb.getPlainMap());
    assertThat(gb.getPlainMap()).hasSize(2);
    assertThat(gb.getPlainMap().get("1")).isEqualTo("0");
    assertThat(gb.getPlainMap().get("2")).isEqualTo("3");
    assertThat(gb.getShortMap()).hasSize(2);
    assertThat(gb.getShortMap().get(Short.valueOf("1"))).isEqualTo(0);
    assertThat(gb.getShortMap().get(Short.valueOf("2"))).isEqualTo(3);
  }

  @Test
  void testGenericMapMapConstructorWithSameRefAndNoConversion() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Map<Short, Integer> input = new HashMap<>();
    input.put((short) 1, 0);
    input.put((short) 2, 3);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getShortMap()).isSameAs(gb.getPlainMap());
    assertThat(gb.getShortMap()).hasSize(2);
    assertThat(gb.getShortMap().get(Short.valueOf("1"))).isEqualTo(0);
    assertThat(gb.getShortMap().get(Short.valueOf("2"))).isEqualTo(3);
  }

  @Test
  void testGenericMapWithKeyTypeConstructor() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Map<String, String> input = new HashMap<>();
    input.put("4", "5");
    input.put("6", "7");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getLongMap().get(4L)).isEqualTo("5");
    assertThat(gb.getLongMap().get(6L)).isEqualTo("7");
  }

  @Test
  void testGenericMapWithCollectionValueConstructor() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.addPropertyEditorRegistrar(registry -> registry.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, false)));
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);

    Map<String, AbstractCollection<?>> input = new HashMap<>();
    HashSet<Integer> value1 = new HashSet<>();
    value1.add(1);
    input.put("1", value1);
    ArrayList<Boolean> value2 = new ArrayList<>();
    value2.add(Boolean.TRUE);
    input.put("2", value2);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(Boolean.TRUE);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    boolean condition1 = gb.getCollectionMap().get(1) instanceof HashSet;
    assertThat(condition1).isTrue();
    boolean condition = gb.getCollectionMap().get(2) instanceof ArrayList;
    assertThat(condition).isTrue();
  }

  @Test
  void testGenericSetFactoryMethod() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setFactoryMethodName("createInstance");

    Set<String> input = new HashSet<>();
    input.add("4");
    input.add("5");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet().contains(4)).isTrue();
    assertThat(gb.getIntegerSet().contains(5)).isTrue();
  }

  @Test
  void testGenericSetListFactoryMethod() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setFactoryMethodName("createInstance");

    Set<String> input = new HashSet<>();
    input.add("4");
    input.add("5");
    List<String> input2 = new ArrayList<>();
    input2.add("http://localhost:8080");
    input2.add("http://localhost:9090");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet().contains(4)).isTrue();
    assertThat(gb.getIntegerSet().contains(5)).isTrue();
    assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
    assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
  }

  @Test
  void testGenericSetMapFactoryMethod() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setFactoryMethodName("createInstance");

    Set<String> input = new HashSet<>();
    input.add("4");
    input.add("5");
    Map<String, String> input2 = new HashMap<>();
    input2.put("4", "5");
    input2.put("6", "7");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getIntegerSet().contains(4)).isTrue();
    assertThat(gb.getIntegerSet().contains(5)).isTrue();
    assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
    assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
  }

  @Test
  void testGenericMapResourceFactoryMethod() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setFactoryMethodName("createInstance");

    Map<String, String> input = new HashMap<>();
    input.put("4", "5");
    input.put("6", "7");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
    rbd.getConstructorArgumentValues().addGenericArgumentValue("http://localhost:8080");

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
    assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
    assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
  }

  @Test
  void testGenericMapMapFactoryMethod() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setFactoryMethodName("createInstance");

    Map<String, String> input = new HashMap<>();
    input.put("1", "0");
    input.put("2", "3");
    Map<String, String> input2 = new HashMap<>();
    input2.put("4", "5");
    input2.put("6", "7");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input2);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getPlainMap().get("1")).isEqualTo("0");
    assertThat(gb.getPlainMap().get("2")).isEqualTo("3");
    assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
    assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
  }

  @Test
  void testGenericMapWithKeyTypeFactoryMethod() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setFactoryMethodName("createInstance");

    Map<String, String> input = new HashMap<>();
    input.put("4", "5");
    input.put("6", "7");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    assertThat(gb.getLongMap().get(Long.valueOf("4"))).isEqualTo("5");
    assertThat(gb.getLongMap().get(Long.valueOf("6"))).isEqualTo("7");
  }

  @Test
  void testGenericMapWithCollectionValueFactoryMethod() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.addPropertyEditorRegistrar(registry -> registry.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, false)));
    RootBeanDefinition rbd = new RootBeanDefinition(GenericBean.class);
    rbd.setFactoryMethodName("createInstance");

    Map<String, AbstractCollection<?>> input = new HashMap<>();
    HashSet<Integer> value1 = new HashSet<>();
    value1.add(1);
    input.put("1", value1);
    ArrayList<Boolean> value2 = new ArrayList<>();
    value2.add(Boolean.TRUE);
    input.put("2", value2);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(Boolean.TRUE);
    rbd.getConstructorArgumentValues().addGenericArgumentValue(input);

    bf.registerBeanDefinition("genericBean", rbd);
    GenericBean<?> gb = (GenericBean<?>) bf.getBean("genericBean");

    boolean condition1 = gb.getCollectionMap().get(1) instanceof HashSet;
    assertThat(condition1).isTrue();
    boolean condition = gb.getCollectionMap().get(2) instanceof ArrayList;
    assertThat(condition).isTrue();
  }

  @Test
  void testGenericListBean() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            new ClassPathResource("genericBeanTests.xml", getClass()));
    List<?> list = (List<?>) bf.getBean("list");
    assertThat(list).hasSize(1);
    assertThat(list.get(0)).isEqualTo(new URL("http://localhost:8080"));
  }

  @Test
  void testGenericSetBean() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            new ClassPathResource("genericBeanTests.xml", getClass()));
    Set<?> set = (Set<?>) bf.getBean("set");
    assertThat(set).hasSize(1);
    assertThat(set.iterator().next()).isEqualTo(new URL("http://localhost:8080"));
  }

  @Test
  void testGenericMapBean() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            new ClassPathResource("genericBeanTests.xml", getClass()));
    Map<?, ?> map = (Map<?, ?>) bf.getBean("map");
    assertThat(map).hasSize(1);
    assertThat(map.keySet().iterator().next()).isEqualTo(10);
    assertThat(map.values().iterator().next()).isEqualTo(new URL("http://localhost:8080"));
  }

  @Test
  void testGenericallyTypedIntegerBean() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            new ClassPathResource("genericBeanTests.xml", getClass()));
    GenericIntegerBean gb = (GenericIntegerBean) bf.getBean("integerBean");
    assertThat(gb.getGenericProperty()).isEqualTo(10);
    assertThat(gb.getGenericListProperty().get(0)).isEqualTo(20);
    assertThat(gb.getGenericListProperty().get(1)).isEqualTo(30);
  }

  @Test
  void testGenericallyTypedSetOfIntegerBean() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            new ClassPathResource("genericBeanTests.xml", getClass()));
    GenericSetOfIntegerBean gb = (GenericSetOfIntegerBean) bf.getBean("setOfIntegerBean");
    assertThat(gb.getGenericProperty().iterator().next()).isEqualTo(10);
    assertThat(gb.getGenericListProperty().get(0).iterator().next()).isEqualTo(20);
    assertThat(gb.getGenericListProperty().get(1).iterator().next()).isEqualTo(30);
  }

  @Test
//  @EnabledForTestGroups(LONG_RUNNING)
  void testSetBean() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            new ClassPathResource("genericBeanTests.xml", getClass()));
    UrlSet us = (UrlSet) bf.getBean("setBean");
    assertThat(us).hasSize(1);
    assertThat(us.iterator().next()).isEqualTo(new URL("https://www.springframework.org"));
  }

  /**
   * Tests support for parameterized static {@code factory-method} declarations such as
   * Mockito's {@code mock()} method which has the following signature.
   * <pre>
   * {@code
   * public static <T> T mock(Class<T> classToMock)
   * }
   * </pre>
   * <p>See SPR-9493
   */
  @Test
  void parameterizedStaticFactoryMethod() {
    RootBeanDefinition rbd = new RootBeanDefinition(getClass());
    rbd.setFactoryMethodName("createMockitoMock");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class);

    assertRunnableMockFactory(rbd);
  }

  @Test
  void parameterizedStaticFactoryMethodWithWrappedClassName() {
    RootBeanDefinition rbd = new RootBeanDefinition();
    rbd.setBeanClassName(getClass().getName());
    rbd.setFactoryMethodName("createMockitoMock");
    // TypedStringValue is used as an equivalent to an XML-defined argument String
    rbd.getConstructorArgumentValues().addGenericArgumentValue(new TypedStringValue(Runnable.class.getName()));

    assertRunnableMockFactory(rbd);
  }

  private void assertRunnableMockFactory(RootBeanDefinition rbd) {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("mock", rbd);

    assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
    assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
    Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
    assertThat(beans).hasSize(1);
  }

  /**
   * Tests support for parameterized instance {@code factory-method} declarations such
   * as EasyMock's {@code IMocksControl.createMock()} method which has the following
   * signature.
   * <pre>
   * {@code
   * public <T> T createMock(Class<T> toMock)
   * }
   * </pre>
   * <p>See SPR-10411
   */
  @Test
  void parameterizedInstanceFactoryMethod() {
    StandardBeanFactory bf = new StandardBeanFactory();

    RootBeanDefinition rbd = new RootBeanDefinition(MocksControl.class);
    bf.registerBeanDefinition("mocksControl", rbd);

    rbd = new RootBeanDefinition();
    rbd.setFactoryBeanName("mocksControl");
    rbd.setFactoryMethodName("createMock");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class);
    bf.registerBeanDefinition("mock", rbd);

    assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
    assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
    assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
    assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
    Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
    assertThat(beans).hasSize(1);
  }

  @Test
  void parameterizedInstanceFactoryMethodWithNonResolvedClassName() {
    StandardBeanFactory bf = new StandardBeanFactory();

    RootBeanDefinition rbd = new RootBeanDefinition(MocksControl.class);
    bf.registerBeanDefinition("mocksControl", rbd);

    rbd = new RootBeanDefinition();
    rbd.setFactoryBeanName("mocksControl");
    rbd.setFactoryMethodName("createMock");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class.getName());
    bf.registerBeanDefinition("mock", rbd);

    assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
    assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
    assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
    assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
    Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
    assertThat(beans).hasSize(1);
  }

  @Test
  void parameterizedInstanceFactoryMethodWithInvalidClassName() {
    StandardBeanFactory bf = new StandardBeanFactory();

    RootBeanDefinition rbd = new RootBeanDefinition(MocksControl.class);
    bf.registerBeanDefinition("mocksControl", rbd);

    rbd = new RootBeanDefinition();
    rbd.setFactoryBeanName("mocksControl");
    rbd.setFactoryMethodName("createMock");
    rbd.getConstructorArgumentValues().addGenericArgumentValue("x");
    bf.registerBeanDefinition("mock", rbd);

    assertThat(bf.isTypeMatch("mock", Runnable.class)).isFalse();
    assertThat(bf.isTypeMatch("mock", Runnable.class)).isFalse();
    assertThat(bf.getType("mock")).isNull();
    assertThat(bf.getType("mock")).isNull();
    Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
    assertThat(beans).isEmpty();
  }

  @Test
  void parameterizedInstanceFactoryMethodWithIndexedArgument() {
    StandardBeanFactory bf = new StandardBeanFactory();

    RootBeanDefinition rbd = new RootBeanDefinition(MocksControl.class);
    bf.registerBeanDefinition("mocksControl", rbd);

    rbd = new RootBeanDefinition();
    rbd.setFactoryBeanName("mocksControl");
    rbd.setFactoryMethodName("createMock");
    rbd.getConstructorArgumentValues().addIndexedArgumentValue(0, Runnable.class);
    bf.registerBeanDefinition("mock", rbd);

    assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
    assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
    assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
    assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
    Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
    assertThat(beans).hasSize(1);
  }

  @Test
    // SPR-16720
  void parameterizedInstanceFactoryMethodWithTempClassLoader() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setTempClassLoader(new OverridingClassLoader(getClass().getClassLoader()));

    RootBeanDefinition rbd = new RootBeanDefinition(MocksControl.class);
    bf.registerBeanDefinition("mocksControl", rbd);

    rbd = new RootBeanDefinition();
    rbd.setFactoryBeanName("mocksControl");
    rbd.setFactoryMethodName("createMock");
    rbd.getConstructorArgumentValues().addGenericArgumentValue(Runnable.class);
    bf.registerBeanDefinition("mock", rbd);

    assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
    assertThat(bf.isTypeMatch("mock", Runnable.class)).isTrue();
    assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
    assertThat(bf.getType("mock")).isEqualTo(Runnable.class);
    Map<String, Runnable> beans = bf.getBeansOfType(Runnable.class);
    assertThat(beans).hasSize(1);
  }

  @Test
  void testGenericMatchingWithBeanNameDifferentiation() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setAutowireCandidateResolver(new GenericTypeAwareAutowireCandidateResolver());

    bf.registerBeanDefinition("doubleStore", new RootBeanDefinition(NumberStore.class));
    bf.registerBeanDefinition("floatStore", new RootBeanDefinition(NumberStore.class));
    bf.registerBeanDefinition("numberBean",
            new RootBeanDefinition(NumberBean.class, RootBeanDefinition.AUTOWIRE_CONSTRUCTOR, false));

    NumberBean nb = bf.getBean(NumberBean.class);
    assertThat(nb.getDoubleStore()).isSameAs(bf.getBean("doubleStore"));
    assertThat(nb.getFloatStore()).isSameAs(bf.getBean("floatStore"));

    String[] numberStoreNames = bf.getBeanNamesForType(ResolvableType.fromClass(NumberStore.class)).toArray(new String[0]);
    String[] doubleStoreNames = bf.getBeanNamesForType(ResolvableType.fromClassWithGenerics(NumberStore.class, Double.class)).toArray(new String[0]);
    String[] floatStoreNames = bf.getBeanNamesForType(ResolvableType.fromClassWithGenerics(NumberStore.class, Float.class)).toArray(new String[0]);
    assertThat(numberStoreNames).hasSize(2);
    assertThat(numberStoreNames[0]).isEqualTo("doubleStore");
    assertThat(numberStoreNames[1]).isEqualTo("floatStore");
    assertThat(doubleStoreNames).isEmpty();
    assertThat(floatStoreNames).isEmpty();
  }

  @Test
  void testGenericMatchingWithFullTypeDifferentiation() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
    bf.setAutowireCandidateResolver(new GenericTypeAwareAutowireCandidateResolver());

    RootBeanDefinition bd1 = new RootBeanDefinition(NumberStoreFactory.class);
    bd1.setFactoryMethodName("newDoubleStore");
    bf.registerBeanDefinition("store1", bd1);
    RootBeanDefinition bd2 = new RootBeanDefinition(NumberStoreFactory.class);
    bd2.setFactoryMethodName("newFloatStore");
    bf.registerBeanDefinition("store2", bd2);
    bf.registerBeanDefinition("numberBean",
            new RootBeanDefinition(NumberBean.class, RootBeanDefinition.AUTOWIRE_CONSTRUCTOR, false));

    NumberBean nb = bf.getBean(NumberBean.class);
    assertThat(nb.getDoubleStore()).isSameAs(bf.getBean("store1"));
    assertThat(nb.getFloatStore()).isSameAs(bf.getBean("store2"));

    String[] numberStoreNames = bf.getBeanNamesForType(ResolvableType.fromClass(NumberStore.class)).toArray(new String[0]);
    String[] doubleStoreNames = bf.getBeanNamesForType(ResolvableType.fromClassWithGenerics(NumberStore.class, Double.class)).toArray(new String[0]);
    String[] floatStoreNames = bf.getBeanNamesForType(ResolvableType.fromClassWithGenerics(NumberStore.class, Float.class)).toArray(new String[0]);
    assertThat(numberStoreNames).hasSize(2);
    assertThat(numberStoreNames[0]).isEqualTo("store1");
    assertThat(numberStoreNames[1]).isEqualTo("store2");
    assertThat(doubleStoreNames).hasSize(1);
    assertThat(doubleStoreNames[0]).isEqualTo("store1");
    assertThat(floatStoreNames).hasSize(1);
    assertThat(floatStoreNames[0]).isEqualTo("store2");
    ObjectProvider<NumberStore<?>> numberStoreProvider = bf.getBeanProvider(ResolvableType.fromClass(NumberStore.class));
    ObjectProvider<NumberStore<Double>> doubleStoreProvider = bf.getBeanProvider(ResolvableType.fromClassWithGenerics(NumberStore.class, Double.class));
    ObjectProvider<NumberStore<Float>> floatStoreProvider = bf.getBeanProvider(ResolvableType.fromClassWithGenerics(NumberStore.class, Float.class));
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(numberStoreProvider::get);
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(numberStoreProvider::getIfAvailable);
    assertThat(numberStoreProvider.getIfUnique()).isNull();
    assertThat(doubleStoreProvider.get()).isSameAs(bf.getBean("store1"));
    assertThat(doubleStoreProvider.getIfAvailable()).isSameAs(bf.getBean("store1"));
    assertThat(doubleStoreProvider.getIfUnique()).isSameAs(bf.getBean("store1"));
    assertThat(floatStoreProvider.get()).isSameAs(bf.getBean("store2"));
    assertThat(floatStoreProvider.getIfAvailable()).isSameAs(bf.getBean("store2"));
    assertThat(floatStoreProvider.getIfUnique()).isSameAs(bf.getBean("store2"));

    List<NumberStore<?>> resolved = new ArrayList<>();
    for (NumberStore<?> instance : numberStoreProvider) {
      resolved.add(instance);
    }
    assertThat(resolved).hasSize(2);
    assertThat(resolved.get(0)).isSameAs(bf.getBean("store1"));
    assertThat(resolved.get(1)).isSameAs(bf.getBean("store2"));

    resolved = numberStoreProvider.stream().toList();
    assertThat(resolved).hasSize(2);
    assertThat(resolved.get(0)).isSameAs(bf.getBean("store1"));
    assertThat(resolved.get(1)).isSameAs(bf.getBean("store2"));

    resolved = numberStoreProvider.orderedStream().toList();
    assertThat(resolved).hasSize(2);
    assertThat(resolved.get(0)).isSameAs(bf.getBean("store2"));
    assertThat(resolved.get(1)).isSameAs(bf.getBean("store1"));

    resolved = new ArrayList<>();
    for (NumberStore<Double> instance : doubleStoreProvider) {
      resolved.add(instance);
    }
    assertThat(resolved).hasSize(1);
    assertThat(resolved.contains(bf.getBean("store1"))).isTrue();

    resolved = doubleStoreProvider.stream().collect(Collectors.toList());
    assertThat(resolved).hasSize(1);
    assertThat(resolved.contains(bf.getBean("store1"))).isTrue();

    resolved = doubleStoreProvider.orderedStream().collect(Collectors.toList());
    assertThat(resolved).hasSize(1);
    assertThat(resolved.contains(bf.getBean("store1"))).isTrue();

    resolved = new ArrayList<>();
    for (NumberStore<Float> instance : floatStoreProvider) {
      resolved.add(instance);
    }
    assertThat(resolved).hasSize(1);
    assertThat(resolved.contains(bf.getBean("store2"))).isTrue();

    resolved = floatStoreProvider.stream().collect(Collectors.toList());
    assertThat(resolved).hasSize(1);
    assertThat(resolved.contains(bf.getBean("store2"))).isTrue();

    resolved = floatStoreProvider.orderedStream().collect(Collectors.toList());
    assertThat(resolved).hasSize(1);
    assertThat(resolved.contains(bf.getBean("store2"))).isTrue();
  }

  @Test
  void testGenericMatchingWithUnresolvedOrderedStream() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
    bf.setAutowireCandidateResolver(new GenericTypeAwareAutowireCandidateResolver());

    RootBeanDefinition bd1 = new RootBeanDefinition(NumberStoreFactory.class);
    bd1.setFactoryMethodName("newDoubleStore");
    bf.registerBeanDefinition("store1", bd1);
    RootBeanDefinition bd2 = new RootBeanDefinition(NumberStoreFactory.class);
    bd2.setFactoryMethodName("newFloatStore");
    bf.registerBeanDefinition("store2", bd2);

    ObjectProvider<NumberStore<?>> numberStoreProvider = bf.getBeanProvider(ResolvableType.fromClass(NumberStore.class));
    List<NumberStore<?>> resolved = numberStoreProvider.orderedStream().toList();
    assertThat(resolved).hasSize(2);
    assertThat(resolved.get(0)).isSameAs(bf.getBean("store2"));
    assertThat(resolved.get(1)).isSameAs(bf.getBean("store1"));
  }

  /**
   * Mimics and delegates to {@link Mockito#mock(Class)} -- created here to avoid factory
   * method resolution issues caused by the introduction of {@code Mockito.mock(T...)}
   * in Mockito 4.10.
   */
  public static <T> T createMockitoMock(Class<T> classToMock) {
    return Mockito.mock(classToMock);
  }

  @SuppressWarnings("serial")
  public static class NamedUrlList extends ArrayList<URL> {
  }

  @SuppressWarnings("serial")
  public static class NamedUrlSet extends HashSet<URL> {
  }

  @SuppressWarnings("serial")
  public static class NamedUrlMap extends HashMap<Integer, URL> {
  }

  public static class CollectionDependentBean {

    public CollectionDependentBean(NamedUrlList list, NamedUrlSet set, NamedUrlMap map) {
      assertThat(list).hasSize(1);
      assertThat(set).hasSize(1);
      assertThat(map).hasSize(1);
    }
  }

  @SuppressWarnings("serial")
  public static class UrlSet extends HashSet<URL> {

    public UrlSet() {
      super();
    }

    public UrlSet(Set<? extends URL> urls) {
      super();
    }

    public void setUrlNames(Set<URI> urlNames) throws MalformedURLException {
      for (URI urlName : urlNames) {
        add(urlName.toURL());
      }
    }
  }

  /**
   * Pseudo-implementation of EasyMock's {@code MocksControl} class.
   */
  public static class MocksControl {

    @SuppressWarnings("unchecked")
    public <T> T createMock(Class<T> toMock) {
      return (T) Proxy.newProxyInstance(BeanFactoryGenericsTests.class.getClassLoader(), new Class<?>[] { toMock },
              (InvocationHandler) (proxy, method, args) -> {
                throw new UnsupportedOperationException("mocked!");
              });
    }
  }

  public static class NumberStore<T extends Number> {
  }

  public static class DoubleStore extends NumberStore<Double> {
  }

  public static class FloatStore extends NumberStore<Float> {
  }

  public static class NumberBean {

    private final NumberStore<Double> doubleStore;

    private final NumberStore<Float> floatStore;

    public NumberBean(NumberStore<Double> doubleStore, NumberStore<Float> floatStore) {
      this.doubleStore = doubleStore;
      this.floatStore = floatStore;
    }

    public NumberStore<Double> getDoubleStore() {
      return this.doubleStore;
    }

    public NumberStore<Float> getFloatStore() {
      return this.floatStore;
    }
  }

  public static class NumberStoreFactory {

    @Order(1)
    public static NumberStore<Double> newDoubleStore() {
      return new DoubleStore();
    }

    @Order(0)
    public static NumberStore<Float> newFloatStore() {
      return new FloatStore();
    }
  }

}
