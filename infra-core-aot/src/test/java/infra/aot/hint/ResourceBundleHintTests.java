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
 * @since 5.0 2025/11/16 22:13
 */
class ResourceBundleHintTests {

  @Test
  void builderCreatesHintWithBaseNameOnly() {
    ResourceBundleHint hint = new ResourceBundleHint.Builder("com.example.messages").build();
    assertThat(hint.getBaseName()).isEqualTo("com.example.messages");
    assertThat(hint.getReachableType()).isNull();
  }

  @Test
  void builderCreatesHintWithBaseNameAndReachableType() {
    TypeReference typeReference = TypeReference.of("com.example.SomeClass");
    ResourceBundleHint hint = new ResourceBundleHint.Builder("com.example.messages")
            .onReachableType(typeReference)
            .build();

    assertThat(hint.getBaseName()).isEqualTo("com.example.messages");
    assertThat(hint.getReachableType()).isEqualTo(typeReference);
  }

  @Test
  void equalsAndHashCodeWithSameValues() {
    ResourceBundleHint hint1 = new ResourceBundleHint.Builder("com.example.messages").build();
    ResourceBundleHint hint2 = new ResourceBundleHint.Builder("com.example.messages").build();

    assertThat(hint1).isEqualTo(hint2);
    assertThat(hint1.hashCode()).isEqualTo(hint2.hashCode());
  }

  @Test
  void equalsAndHashCodeWithDifferentBaseNames() {
    ResourceBundleHint hint1 = new ResourceBundleHint.Builder("com.example.messages1").build();
    ResourceBundleHint hint2 = new ResourceBundleHint.Builder("com.example.messages2").build();

    assertThat(hint1).isNotEqualTo(hint2);
    assertThat(hint1.hashCode()).isNotEqualTo(hint2.hashCode());
  }

  @Test
  void equalsAndHashCodeWithDifferentReachableTypes() {
    TypeReference type1 = TypeReference.of("com.example.Class1");
    TypeReference type2 = TypeReference.of("com.example.Class2");

    ResourceBundleHint hint1 = new ResourceBundleHint.Builder("com.example.messages")
            .onReachableType(type1)
            .build();
    ResourceBundleHint hint2 = new ResourceBundleHint.Builder("com.example.messages")
            .onReachableType(type2)
            .baseName(null)
            .build();

    assertThat(hint1).isNotEqualTo(hint2);
  }

  @Test
  void equalsWithSameInstance() {
    ResourceBundleHint hint = new ResourceBundleHint.Builder("com.example.messages").build();
    assertThat(hint).isEqualTo(hint);
  }

  @Test
  void equalsWithNull() {
    ResourceBundleHint hint = new ResourceBundleHint.Builder("com.example.messages").build();
    assertThat(hint).isNotEqualTo(null);
  }

  @Test
  void equalsWithDifferentObjectType() {
    ResourceBundleHint hint = new ResourceBundleHint.Builder("com.example.messages").build();
    assertThat(hint).isNotEqualTo("some string");
  }

}