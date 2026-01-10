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