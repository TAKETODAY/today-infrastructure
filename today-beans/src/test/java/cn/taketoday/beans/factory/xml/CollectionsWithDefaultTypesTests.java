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

import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class CollectionsWithDefaultTypesTests {

  private final StandardBeanFactory beanFactory;

  public CollectionsWithDefaultTypesTests() {
    this.beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
            new ClassPathResource("collectionsWithDefaultTypes.xml", getClass()));
  }

  @Test
  public void testListHasDefaultType() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
    for (Object o : bean.getSomeList()) {
      assertThat(o.getClass()).as("Value type is incorrect").isEqualTo(Integer.class);
    }
  }

  @Test
  public void testSetHasDefaultType() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
    for (Object o : bean.getSomeSet()) {
      assertThat(o.getClass()).as("Value type is incorrect").isEqualTo(Integer.class);
    }
  }

  @Test
  public void testMapHasDefaultKeyAndValueType() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
    assertMap(bean.getSomeMap());
  }

  @Test
  public void testMapWithNestedElementsHasDefaultKeyAndValueType() throws Exception {
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean2");
    assertMap(bean.getSomeMap());
  }

  @SuppressWarnings("rawtypes")
  private void assertMap(Map<?, ?> map) {
    for (Map.Entry entry : map.entrySet()) {
      assertThat(entry.getKey().getClass()).as("Key type is incorrect").isEqualTo(Integer.class);
      assertThat(entry.getValue().getClass()).as("Value type is incorrect").isEqualTo(Boolean.class);
    }
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void testBuildCollectionFromMixtureOfReferencesAndValues() throws Exception {
    MixedCollectionBean jumble = (MixedCollectionBean) this.beanFactory.getBean("jumble");
    assertThat(jumble.getJumble().size() == 3).as("Expected 3 elements, not " + jumble.getJumble().size()).isTrue();
    List l = (List) jumble.getJumble();
    assertThat(l.get(0).equals("literal")).isTrue();
    Integer[] array1 = (Integer[]) l.get(1);
    assertThat(array1[0].equals(2)).isTrue();
    assertThat(array1[1].equals(4)).isTrue();
    int[] array2 = (int[]) l.get(2);
    assertThat(array2[0] == 3).isTrue();
    assertThat(array2[1] == 5).isTrue();
  }

}
