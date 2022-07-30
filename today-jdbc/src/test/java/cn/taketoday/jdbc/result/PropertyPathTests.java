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

package cn.taketoday.jdbc.result;

import org.junit.jupiter.api.Test;

import lombok.Data;
import lombok.NoArgsConstructor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/30 20:31
 */
class PropertyPathTests {

  @Data
  @NoArgsConstructor
  static class Nested {

    public Nested(String name) {
      this.name = name;
    }

    String name;

    Nested nested;

  }

  @Test
  void getNestedObject() {
    Nested nested = new Nested("yhj");
    Nested nested1 = new Nested("yhj1");
    Nested nested2 = new Nested("yhj2");

    nested.nested = nested1;
    nested1.nested = nested2;

    PropertyPath propertyPath = new PropertyPath(nested.getClass(), "nested.nested.name");

    assertThat(propertyPath.next).isNotNull();
    assertThat(propertyPath.next.next).isNotNull();
    assertThat(propertyPath.getNestedBeanProperty()).isEqualTo(propertyPath.next.next.beanProperty);
    Object nestedValue = propertyPath.getNestedObject(nested);
    assertThat(nestedValue).isEqualTo(nested2);

    propertyPath.set(nested, "yhj2-modified");
    assertThat(nested.nested.nested.name).isEqualTo("yhj2-modified");

    assertThat(propertyPath.toString()).isEqualTo("nested.nested.name");
  }

  @Test
  void getNestedObjectNull() {
    Nested nested = new Nested("yhj");
    PropertyPath propertyPath = new PropertyPath(nested.getClass(), "nested.nested.name");

    assertThat(propertyPath.next).isNotNull();
    assertThat(propertyPath.next.next).isNotNull();

    Object nestedValue = propertyPath.getNestedObject(nested);

    assertThat(nestedValue).isEqualTo(nested.nested.nested);
    propertyPath.set(nested, "yhj2-modified");
    assertThat(nested.nested.nested.name).isEqualTo("yhj2-modified");

    // error case
    PropertyPath propertyPathError = new PropertyPath(nested.getClass(), "nested.nested.names");
    assertThat(propertyPathError.next).isNotNull();
    assertThat(propertyPathError.next.next).isNotNull();
    assertThat(propertyPathError.next.next.next).isNull();
    assertThat(propertyPathError.next.next.beanProperty).isNull();
    assertThat(propertyPathError.toString()).isEqualTo("nested.nested." + PropertyPath.emptyPlaceholder);

    // error case
    PropertyPath propertyPathnestedsError = new PropertyPath(nested.getClass(), "nested.nesteds.names");

    assertThat(propertyPathnestedsError.next).isNotNull();
    assertThat(propertyPathnestedsError.next.next).isNull();
    assertThat(propertyPathnestedsError.toString()).isEqualTo("nested." + PropertyPath.emptyPlaceholder);
  }

}