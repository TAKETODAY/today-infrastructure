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

package cn.taketoday.jdbc.persistence;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.jdbc.persistence.model.UserModel;

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

    BeanMetadata metadata = BeanMetadata.from(UserModel.class);
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("id"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("class"))).isTrue();

  }

  @Test
  void forTransientAnnotation() {
    PropertyFilter propertyFilter = PropertyFilter.forTransientAnnotation();
    BeanMetadata metadata = BeanMetadata.from(NestedUserModel.class);
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("class"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("id"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("my"))).isFalse();
    assertThat(propertyFilter.isFiltered(metadata.getBeanProperty("user"))).isTrue();

  }

  @Test
  void forAnnotation() {
    PropertyFilter propertyFilter = PropertyFilter.forAnnotation(MyTransient.class);
    BeanMetadata metadata = BeanMetadata.from(NestedUserModel.class);
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

    BeanMetadata metadata = BeanMetadata.from(NestedUserModel.class);
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