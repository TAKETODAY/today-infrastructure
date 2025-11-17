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

package infra.aot.hint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/16 22:16
 */
class JavaSerializationHintTests {

  @Test
  void builderCreatesHintWithTypeOnly() {
    TypeReference type = TypeReference.of("java.lang.String");
    JavaSerializationHint hint = new JavaSerializationHint.Builder(type).build();
    assertThat(hint.getType()).isEqualTo(type);
    assertThat(hint.getReachableType()).isNull();
  }

  @Test
  void builderCreatesHintWithTypeAndReachableType() {
    TypeReference type = TypeReference.of("java.lang.String");
    TypeReference reachableType = TypeReference.of("java.util.List");
    JavaSerializationHint hint = new JavaSerializationHint.Builder(type)
            .onReachableType(reachableType)
            .build();

    assertThat(hint.getType()).isEqualTo(type);
    assertThat(hint.getReachableType()).isEqualTo(reachableType);
  }

  @Test
  void equalsAndHashCodeWithSameValues() {
    TypeReference type = TypeReference.of("java.lang.String");
    JavaSerializationHint hint1 = new JavaSerializationHint.Builder(type).build();
    JavaSerializationHint hint2 = new JavaSerializationHint.Builder(type).build();

    assertThat(hint1).isEqualTo(hint2);
    assertThat(hint1.hashCode()).isEqualTo(hint2.hashCode());
  }

  @Test
  void equalsAndHashCodeWithDifferentTypes() {
    TypeReference type1 = TypeReference.of("java.lang.String");
    TypeReference type2 = TypeReference.of("java.lang.Integer");
    JavaSerializationHint hint1 = new JavaSerializationHint.Builder(type1).build();
    JavaSerializationHint hint2 = new JavaSerializationHint.Builder(type2).build();

    assertThat(hint1).isNotEqualTo(hint2);
    assertThat(hint1.hashCode()).isNotEqualTo(hint2.hashCode());
  }

  @Test
  void equalsAndHashCodeWithDifferentReachableTypes() {
    TypeReference type = TypeReference.of("java.lang.String");
    TypeReference reachableType1 = TypeReference.of("java.util.List");
    TypeReference reachableType2 = TypeReference.of("java.util.Set");

    JavaSerializationHint hint1 = new JavaSerializationHint.Builder(type)
            .onReachableType(reachableType1)
            .build();
    JavaSerializationHint hint2 = new JavaSerializationHint.Builder(type)
            .onReachableType(reachableType2)
            .build();

    assertThat(hint1).isNotEqualTo(hint2);
    assertThat(hint1.hashCode()).isNotEqualTo(hint2.hashCode());
  }

  @Test
  void equalsWithSameInstance() {
    TypeReference type = TypeReference.of("java.lang.String");
    JavaSerializationHint hint = new JavaSerializationHint.Builder(type).build();
    assertThat(hint).isEqualTo(hint);
  }

  @Test
  void equalsWithNull() {
    TypeReference type = TypeReference.of("java.lang.String");
    JavaSerializationHint hint = new JavaSerializationHint.Builder(type).build();
    assertThat(hint).isNotEqualTo(null);
  }

  @Test
  void equalsWithDifferentObjectType() {
    TypeReference type = TypeReference.of("java.lang.String");
    JavaSerializationHint hint = new JavaSerializationHint.Builder(type).build();
    assertThat(hint).isNotEqualTo("some string");
  }

}