/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.beans.factory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.util.InfraStrategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class BeanMetadataPrefixTests {

  @BeforeAll
  static void configurePrefix() {
    InfraStrategies.setProperty(BeanMetadata.FIELD_PREFIXES_PROPERTY_NAME, "m_,_");
  }

  @AfterAll
  static void resetPrefix() {
    InfraStrategies.setProperty(BeanMetadata.FIELD_PREFIXES_PROPERTY_NAME, null);
  }

  @Test
  void prefixedFieldBindsToExistingProperty() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(PrefixedBean.class);
    assertThat(beanMetadata.containsProperty("name")).isTrue();
    assertThat(beanMetadata.containsProperty("m_name")).isFalse();

    PrefixedBean bean = new PrefixedBean();
    beanMetadata.setPropertyValue(bean, "name", "today");
    assertThat(bean.getName()).isEqualTo("today");
  }

  @Test
  void unprefixedFieldKeepsItsOwnName() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(PrefixedBean.class);
    assertThat(beanMetadata.containsProperty("m_name")).isFalse();
    assertThat(beanMetadata.containsProperty("age")).isTrue();
  }

  @Test
  void prefixedFieldWithoutMatchingPropertyKeepsRawName() {
    // 字段 _nickname 带前缀 "_"，但不存在 nickname 属性时，应保留原名
    BeanMetadata beanMetadata = BeanMetadata.forClass(PrefixedBean.class);
    assertThat(beanMetadata.containsProperty("_nickname")).isTrue();
  }

  @Test
  void prefixItselfAsWholeFieldNameIsNotStripped() {
    // 字段名恰好等于前缀（无剩余部分）时跳过前缀剥离
    BeanMetadata beanMetadata = BeanMetadata.forClass(PrefixedBean.class);
    assertThat(beanMetadata.containsProperty("m_")).isTrue();
  }

  @Test
  void secondConfiguredPrefixAlsoMatches() {
    // 配置了 m_,_ 两个前缀，_ 前缀对 _title 字段生效
    BeanMetadata beanMetadata = BeanMetadata.forClass(PrefixedBean.class);
    assertThat(beanMetadata.containsProperty("title")).isTrue();
    assertThat(beanMetadata.containsProperty("_title")).isFalse();

    PrefixedBean bean = new PrefixedBean();
    beanMetadata.setPropertyValue(bean, "title", "frame");
    assertThat(bean.getTitle()).isEqualTo("frame");
  }

  @Test
  void prefixedFieldIsWiredToBeanPropertyField() {
    // 前缀字段通过 setField 关联到对应的 BeanProperty
    BeanMetadata beanMetadata = BeanMetadata.forClass(PrefixedBean.class);
    BeanProperty property = beanMetadata.getProperty("name");
    assertThat(property).isNotNull();
    assertThat(property.getField()).isNotNull();
    assertThat(property.getField().getName()).isEqualTo("m_name");
  }

  @Test
  void getPropertyRoundTripWorksThroughPrefix() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(PrefixedBean.class);
    PrefixedBean bean = new PrefixedBean();

    bean.setName("hello");
    assertThat(beanMetadata.getPropertyValue(bean, "name")).isEqualTo("hello");
  }

  @Test
  void fieldNamesReturnsAllNonStaticFieldNames() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(PrefixedBean.class);

    assertThat(beanMetadata.propertyNames()).contains(
            "name", "age", "_nickname", "m_", "title");
    // 静态字段被排除
    assertThat(beanMetadata.propertyNames()).doesNotContain("STATIC");

    assertThatThrownBy(() -> beanMetadata.propertyNames().add("any"))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  static class PrefixedBean {

    private String m_name;

    private int age;

    private String _nickname;

    private String m_;

    private String _title;

    private static String STATIC = "static";

    public String getName() {
      return m_name;
    }

    public void setName(String name) {
      this.m_name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public String getTitle() {
      return _title;
    }

    public void setTitle(String title) {
      this._title = title;
    }

  }
}
