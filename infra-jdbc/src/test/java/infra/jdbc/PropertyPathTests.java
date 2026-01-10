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

package infra.jdbc;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import infra.core.style.ToStringBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/30 20:31
 */
class PropertyPathTests {

  static class Nested {

    public Nested() {
    }

    public Nested(String name) {
      this.name = name;
    }

    String name;

    Nested nested;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Nested getNested() {
      return nested;
    }

    public void setNested(Nested nested) {
      this.nested = nested;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Nested nested1))
        return false;
      return Objects.equals(name, nested1.name)
              && Objects.equals(nested, nested1.nested);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, nested);
    }

    @Override
    public String toString() {
      return ToStringBuilder.forInstance(this)
              .append("name", name)
              .append("nested", nested)
              .toString();
    }
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