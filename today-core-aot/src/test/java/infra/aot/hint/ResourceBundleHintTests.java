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