/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.generate;

import com.squareup.javapoet.ClassName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import cn.taketoday.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GeneratedTypeReference}.
 *
 * @author Stephane Nicoll
 */
class GeneratedTypeReferenceTests {

  @ParameterizedTest
  @MethodSource("reflectionTargetNames")
  void hasSuitableReflectionTargetName(TypeReference typeReference, String binaryName) {
    assertThat(typeReference.getName()).isEqualTo(binaryName);
  }

  static Stream<Arguments> reflectionTargetNames() {
    return Stream.of(
            Arguments.of(GeneratedTypeReference.of(ClassName.get("com.example", "Test")), "com.example.Test"),
            Arguments.of(GeneratedTypeReference.of(ClassName.get("com.example", "Test", "Inner")), "com.example.Test$Inner"));
  }

  @Test
  void createWithClassName() {
    GeneratedTypeReference typeReference = GeneratedTypeReference.of(
            ClassName.get("com.example", "Test"));
    assertThat(typeReference.getPackageName()).isEqualTo("com.example");
    assertThat(typeReference.getSimpleName()).isEqualTo("Test");
    assertThat(typeReference.getCanonicalName()).isEqualTo("com.example.Test");
    assertThat(typeReference.getEnclosingType()).isNull();
  }

  @Test
  void createWithClassNameAndParent() {
    GeneratedTypeReference typeReference = GeneratedTypeReference.of(
            ClassName.get("com.example", "Test").nestedClass("Nested"));
    assertThat(typeReference.getPackageName()).isEqualTo("com.example");
    assertThat(typeReference.getSimpleName()).isEqualTo("Nested");
    assertThat(typeReference.getCanonicalName()).isEqualTo("com.example.Test.Nested");
    assertThat(typeReference.getEnclosingType()).satisfies(parentTypeReference -> {
      assertThat(parentTypeReference.getPackageName()).isEqualTo("com.example");
      assertThat(parentTypeReference.getSimpleName()).isEqualTo("Test");
      assertThat(parentTypeReference.getCanonicalName()).isEqualTo("com.example.Test");
      assertThat(parentTypeReference.getEnclosingType()).isNull();
    });
  }

  @Test
  void nameOfCglibProxy() {
    TypeReference reference = GeneratedTypeReference.of(
            ClassName.get("com.example", "Test$$SpringCGLIB$$0"));
    assertThat(reference.getSimpleName()).isEqualTo("Test$$SpringCGLIB$$0");
    assertThat(reference.getEnclosingType()).isNull();
  }

  @Test
  void nameOfNestedCglibProxy() {
    TypeReference reference = GeneratedTypeReference.of(
            ClassName.get("com.example", "Test").nestedClass("Another$$SpringCGLIB$$0"));
    assertThat(reference.getSimpleName()).isEqualTo("Another$$SpringCGLIB$$0");
    assertThat(reference.getEnclosingType()).isNotNull();
    assertThat(reference.getEnclosingType().getSimpleName()).isEqualTo("Test");
  }

  @Test
  void equalsWithIdenticalCanonicalNameIsTrue() {
    assertThat(GeneratedTypeReference.of(ClassName.get("java.lang", "String")))
            .isEqualTo(TypeReference.of(String.class));
  }

}
