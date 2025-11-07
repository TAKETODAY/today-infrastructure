/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.beans.BeanProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 15:37
 */
class JdbcBeanMetadataTests {

  @Test
  void shouldCreateJdbcBeanMetadata() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);

    assertThat(metadata.getObjectType()).isEqualTo(TestBean.class);
    assertThat(metadata.caseSensitive).isTrue();
    assertThat(metadata.autoDeriveColumnNames).isFalse();
    assertThat(metadata.throwOnMappingFailure).isTrue();
  }

  @Test
  void shouldCreateNewInstance() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    Object instance = metadata.newInstance();

    assertThat(instance).isInstanceOf(TestBean.class);
  }

  @Test
  void shouldGetBeanPropertyWithDirectMatch() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    BeanProperty property = metadata.getBeanProperty("name", null);

    assertThat(property).isNotNull();
    assertThat(property.getName()).isEqualTo("name");
  }

  @Test
  void shouldGetBeanPropertyWithCaseInsensitiveMatch() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, false, false, true);
    BeanProperty property = metadata.getBeanProperty("NAME", null);

    assertThat(property).isNotNull();
    assertThat(property.getName()).isEqualTo("name");
  }

  @Test
  void shouldGetBeanPropertyWithColumnMapping() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    Map<String, String> columnMappings = Map.of("user_name", "name");
    BeanProperty property = metadata.getBeanProperty("user_name", columnMappings);

    assertThat(property).isNotNull();
    assertThat(property.getName()).isEqualTo("name");
  }

  @Test
  void shouldGetBeanPropertyWithAutoDeriveColumnNames() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, true, true);
    BeanProperty property = metadata.getBeanProperty("user_name", null);

    assertThat(property).isNotNull();
    assertThat(property.getName()).isEqualTo("userName");
  }

  @Test
  void shouldReturnNullForNonExistentProperty() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    BeanProperty property = metadata.getBeanProperty("nonExistent", null);

    assertThat(property).isNull();
  }

  @Test
  void shouldGetAnnotatedPropertyName() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(AnnotatedTestBean.class, true, false, true);
    BeanProperty property = metadata.getBeanProperty("custom_name", null);

    assertThat(property).isNotNull();
    assertThat(property.getName()).isEqualTo("annotatedField");
  }

  @Test
  void shouldEqualsAndHashCode() {
    JdbcBeanMetadata metadata1 = new JdbcBeanMetadata(TestBean.class, true, false, true);
    JdbcBeanMetadata metadata2 = new JdbcBeanMetadata(TestBean.class, true, false, true);
    JdbcBeanMetadata metadata3 = new JdbcBeanMetadata(TestBean.class, false, false, true);

    assertThat(metadata1).isEqualTo(metadata1);
    assertThat(metadata1).isEqualTo(metadata2);
    assertThat(metadata1).isNotEqualTo(metadata3);
    assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
  }

  @Test
  void shouldHandleNullColumnMappings() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    BeanProperty property = metadata.getBeanProperty("name", null);

    assertThat(property).isNotNull();
    assertThat(property.getName()).isEqualTo("name");
  }

  @Test
  void shouldReturnNullWhenColumnMappingNotFound() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    Map<String, String> columnMappings = Map.of("unknown_column", "unknown_property");
    BeanProperty property = metadata.getBeanProperty("unknown_column", columnMappings);

    assertThat(property).isNull();
  }

  @Test
  void shouldReturnNullWhenMappedPropertyNotFound() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    Map<String, String> columnMappings = Map.of("custom_column", "nonExistentProperty");
    BeanProperty property = metadata.getBeanProperty("custom_column", columnMappings);

    assertThat(property).isNull();
  }

  @Test
  void shouldNotAutoDeriveWhenDisabled() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    BeanProperty property = metadata.getBeanProperty("user_name", null);

    assertThat(property).isNull();
  }

  @Test
  void shouldHandleEmptyColumnName() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    BeanProperty property = metadata.getBeanProperty("", null);

    assertThat(property).isNull();
  }

  @Test
  void shouldHandlePropertyWithEmptyAnnotationValue() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(EmptyAnnotationBean.class, true, false, true);
    BeanProperty property = metadata.getBeanProperty("", null);

    assertThat(property).isNull();
  }

  @Test
  void shouldGetPropertyFromCacheOnSecondCall() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    BeanProperty property1 = metadata.getBeanProperty("name", null);
    BeanProperty property2 = metadata.getBeanProperty("name", null);

    assertThat(property1).isNotNull();
    assertThat(property1).isSameAs(property2);
  }

  @Test
  void shouldHandleMultiplePropertiesWithSameNameDifferentCase() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(DuplicateNameBean.class, false, false, true);
    BeanProperty property = metadata.getBeanProperty("name", null);

    assertThat(property).isNotNull();
  }

  @Test
  void shouldHandleComplexUnderscoreToCamelCase() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, true, true);
    BeanProperty property = metadata.getBeanProperty("user_name_age", null);

    // Should match userNameAge if it exists, or fallback behavior
    // In this case, it won't match any existing property
    assertThat(property).isNull();
  }

  @Test
  void shouldEqualsWithSameInstance() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    assertThat(metadata).isEqualTo(metadata);
  }

  @Test
  void shouldNotEqualsWithNull() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    assertThat(metadata).isNotEqualTo(null);
  }

  @Test
  void shouldNotEqualsWithDifferentClass() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    assertThat(metadata).isNotEqualTo(new Object());
  }

  @Test
  void shouldGetPropertyNameWithoutAnnotation() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    BeanProperty property = metadata.getBeanProperty("age", null);

    assertThat(property).isNotNull();
    assertThat(property.getName()).isEqualTo("age");
  }

  static class EmptyAnnotationBean {
    @infra.persistence.Column("")
    private String emptyNameField;

    public String getEmptyNameField() {
      return emptyNameField;
    }

    public void setEmptyNameField(String emptyNameField) {
      this.emptyNameField = emptyNameField;
    }
  }

  static class DuplicateNameBean {
    private String name;
    private String NAME;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getNAME() {
      return NAME;
    }

    public void setNAME(String NAME) {
      this.NAME = NAME;
    }
  }

  static class TestBean {
    private String name;
    private String userName;
    private int age;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }
  }

  static class AnnotatedTestBean {

    @infra.persistence.Column("custom_name")
    private String annotatedField;

    public String getAnnotatedField() {
      return annotatedField;
    }

    public void setAnnotatedField(String annotatedField) {
      this.annotatedField = annotatedField;
    }
  }

}