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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import infra.beans.factory.support.BeanDefinitionReader;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit and integration tests for the collection merging support.
 *
 * @author Rob Harrop
 * @author Rick Evans
 */
@SuppressWarnings("rawtypes")
public class CollectionMergingTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @BeforeEach
  public void setUp() throws Exception {
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
    reader.loadBeanDefinitions(new ClassPathResource("collectionMerging.xml", getClass()));
  }

  @Test
  public void mergeList() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithList");
    List list = bean.getSomeList();
    assertThat(list.size()).as("Incorrect size").isEqualTo(3);
    assertThat(list.get(0)).isEqualTo("Rob Harrop");
    assertThat(list.get(1)).isEqualTo("Rod Johnson");
    assertThat(list.get(2)).isEqualTo("Juergen Hoeller");
  }

  @Test
  public void mergeListWithInnerBeanAsListElement() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithListOfRefs");
    List<?> list = bean.getSomeList();
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(3);
    assertThat(list.get(2)).isNotNull();
    boolean condition = list.get(2) instanceof TestBean;
    assertThat(condition).isTrue();
  }

  @Test
  public void mergeSet() {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithSet");
    Set set = bean.getSomeSet();
    assertThat(set.size()).as("Incorrect size").isEqualTo(2);
    assertThat(set.contains("Rob Harrop")).isTrue();
    assertThat(set.contains("Sally Greenwood")).isTrue();
  }

  @Test
  public void mergeSetWithInnerBeanAsSetElement() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetOfRefs");
    Set<?> set = bean.getSomeSet();
    assertThat(set).isNotNull();
    assertThat(set.size()).isEqualTo(2);
    Iterator it = set.iterator();
    it.next();
    Object o = it.next();
    assertThat(o).isNotNull();
    boolean condition = o instanceof TestBean;
    assertThat(condition).isTrue();
    assertThat(((TestBean) o).getName()).isEqualTo("Sally");
  }

  @Test
  public void mergeMap() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithMap");
    Map map = bean.getSomeMap();
    assertThat(map.size()).as("Incorrect size").isEqualTo(3);
    assertThat(map.get("Rob")).isEqualTo("Sally");
    assertThat(map.get("Rod")).isEqualTo("Kerry");
    assertThat(map.get("Juergen")).isEqualTo("Eva");
  }

  @Test
  public void mergeMapWithInnerBeanAsMapEntryValue() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapOfRefs");
    Map<?, ?> map = bean.getSomeMap();
    assertThat(map).isNotNull();
    assertThat(map.size()).isEqualTo(2);
    assertThat(map.get("Rob")).isNotNull();
    boolean condition = map.get("Rob") instanceof TestBean;
    assertThat(condition).isTrue();
    assertThat(((TestBean) map.get("Rob")).getName()).isEqualTo("Sally");
  }

  @Test
  public void mergeProperties() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithProps");
    Properties props = bean.getSomeProperties();
    assertThat(props.size()).as("Incorrect size").isEqualTo(3);
    assertThat(props.getProperty("Rob")).isEqualTo("Sally");
    assertThat(props.getProperty("Rod")).isEqualTo("Kerry");
    assertThat(props.getProperty("Juergen")).isEqualTo("Eva");
  }

  @Test
  public void mergeListInConstructor() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithListInConstructor");
    List list = bean.getSomeList();
    assertThat(list.size()).as("Incorrect size").isEqualTo(3);
    assertThat(list.get(0)).isEqualTo("Rob Harrop");
    assertThat(list.get(1)).isEqualTo("Rod Johnson");
    assertThat(list.get(2)).isEqualTo("Juergen Hoeller");
  }

  @Test
  public void mergeListWithInnerBeanAsListElementInConstructor() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithListOfRefsInConstructor");
    List<?> list = bean.getSomeList();
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(3);
    assertThat(list.get(2)).isNotNull();
    boolean condition = list.get(2) instanceof TestBean;
    assertThat(condition).isTrue();
  }

  @Test
  public void mergeSetInConstructor() {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetInConstructor");
    Set set = bean.getSomeSet();
    assertThat(set.size()).as("Incorrect size").isEqualTo(2);
    assertThat(set.contains("Rob Harrop")).isTrue();
    assertThat(set.contains("Sally Greenwood")).isTrue();
  }

  @Test
  public void mergeSetWithInnerBeanAsSetElementInConstructor() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithSetOfRefsInConstructor");
    Set<?> set = bean.getSomeSet();
    assertThat(set).isNotNull();
    assertThat(set.size()).isEqualTo(2);
    Iterator it = set.iterator();
    it.next();
    Object o = it.next();
    assertThat(o).isNotNull();
    boolean condition = o instanceof TestBean;
    assertThat(condition).isTrue();
    assertThat(((TestBean) o).getName()).isEqualTo("Sally");
  }

  @Test
  public void mergeMapInConstructor() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapInConstructor");
    Map map = bean.getSomeMap();
    assertThat(map.size()).as("Incorrect size").isEqualTo(3);
    assertThat(map.get("Rob")).isEqualTo("Sally");
    assertThat(map.get("Rod")).isEqualTo("Kerry");
    assertThat(map.get("Juergen")).isEqualTo("Eva");
  }

  @Test
  public void mergeMapWithInnerBeanAsMapEntryValueInConstructor() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithMapOfRefsInConstructor");
    Map<?, ?> map = bean.getSomeMap();
    assertThat(map).isNotNull();
    assertThat(map.size()).isEqualTo(2);
    assertThat(map.get("Rob")).isNotNull();
    boolean condition = map.get("Rob") instanceof TestBean;
    assertThat(condition).isTrue();
    assertThat(((TestBean) map.get("Rob")).getName()).isEqualTo("Sally");
  }

  @Test
  public void mergePropertiesInConstructor() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("childWithPropsInConstructor");
    Properties props = bean.getSomeProperties();
    assertThat(props.size()).as("Incorrect size").isEqualTo(3);
    assertThat(props.getProperty("Rob")).isEqualTo("Sally");
    assertThat(props.getProperty("Rod")).isEqualTo("Kerry");
    assertThat(props.getProperty("Juergen")).isEqualTo("Eva");
  }

}
