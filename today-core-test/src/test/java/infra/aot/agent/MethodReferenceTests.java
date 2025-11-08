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

package infra.aot.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/1 16:56
 */
class MethodReferenceTests {

  @Test
  void shouldCreateMethodReference() {
    MethodReference methodReference = MethodReference.of(String.class, "toString");
    assertThat(methodReference.getClassName()).isEqualTo("java.lang.String");
    assertThat(methodReference.getMethodName()).isEqualTo("toString");
  }

  @Test
  void shouldImplementEqualsAndHashCode() {
    MethodReference methodReference1 = MethodReference.of(String.class, "toString");
    MethodReference methodReference2 = MethodReference.of(String.class, "toString");
    MethodReference differentMethod = MethodReference.of(String.class, "equals");
    MethodReference differentClass = MethodReference.of(Integer.class, "toString");

    assertThat(methodReference1).isEqualTo(methodReference2);
    assertThat(methodReference1).hasSameHashCodeAs(methodReference2);
    assertThat(methodReference1).isNotEqualTo(differentMethod);
    assertThat(methodReference1).isNotEqualTo(differentClass);
  }

  @Test
  void shouldImplementToString() {
    MethodReference methodReference = MethodReference.of(String.class, "toString");
    assertThat(methodReference.toString()).isEqualTo("java.lang.String#toString");
  }

  @Test
  void shouldHandleNullEquals() {
    MethodReference methodReference = MethodReference.of(String.class, "toString");
    assertThat(methodReference).isNotEqualTo(null);
  }

  @Test
  void shouldHandleSameInstanceEquals() {
    MethodReference methodReference = MethodReference.of(String.class, "toString");
    assertThat(methodReference).isEqualTo(methodReference);
  }

}