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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import infra.beans.BeanMetadata;
import infra.jdbc.model.UserModel;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/3 18:33
 */
class PropertyFilterTests {

  static class NestedUserModel {

    int id;

    @Transient
    UserModel user;

    @MyTransient
    UserModel my;
  }

  @Test
  void filteredNames() {
    PropertyFilter propertyFilter = PropertyFilter.filteredNames(Set.of("class"));

    BeanMetadata metadata = BeanMetadata.forClass(UserModel.class);
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("id"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("class"))).isTrue();

  }

  @Test
  void forTransientAnnotation() {
    PropertyFilter propertyFilter = PropertyFilter.forTransientAnnotation();
    BeanMetadata metadata = BeanMetadata.forClass(NestedUserModel.class);
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("class"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("id"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("my"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("user"))).isTrue();

  }

  @Test
  void forAnnotation() {
    PropertyFilter propertyFilter = PropertyFilter.forAnnotation(MyTransient.class);
    BeanMetadata metadata = BeanMetadata.forClass(NestedUserModel.class);
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("class"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("id"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("my"))).isTrue();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("user"))).isFalse();

  }

  @Test
  void and() {
    PropertyFilter propertyFilter = PropertyFilter.forAnnotation(MyTransient.class)
            .and(PropertyFilter.filteredNames(Set.of("class")))
            .and(PropertyFilter.forTransientAnnotation());

    BeanMetadata metadata = BeanMetadata.forClass(NestedUserModel.class);
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("class"))).isTrue();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("id"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("my"))).isTrue();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("user"))).isTrue();

  }

  @Retention(RUNTIME)
  @Target({ METHOD, FIELD })
  public @interface MyTransient {

  }

}