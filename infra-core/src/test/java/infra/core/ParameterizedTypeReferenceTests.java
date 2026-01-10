/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 22:29
 */
class ParameterizedTypeReferenceTests {

  @Test
  void stringTypeReference() {
    ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<>() { };
    assertThat(typeReference.getType()).isEqualTo(String.class);
  }

  @Test
  void mapTypeReference() throws Exception {
    Type mapType = getClass().getMethod("mapMethod").getGenericReturnType();
    ParameterizedTypeReference<Map<Object, String>> typeReference = new ParameterizedTypeReference<>() { };
    assertThat(typeReference.getType()).isEqualTo(mapType);
  }

  @Test
  void listTypeReference() throws Exception {
    Type listType = getClass().getMethod("listMethod").getGenericReturnType();
    ParameterizedTypeReference<List<String>> typeReference = new ParameterizedTypeReference<>() { };
    assertThat(typeReference.getType()).isEqualTo(listType);
  }

  @Test
  void reflectiveTypeReferenceWithSpecificDeclaration() throws Exception {
    Type listType = getClass().getMethod("listMethod").getGenericReturnType();
    ParameterizedTypeReference<List<String>> typeReference = ParameterizedTypeReference.forType(listType);
    assertThat(typeReference.getType()).isEqualTo(listType);
  }

  @Test
  void reflectiveTypeReferenceWithGenericDeclaration() throws Exception {
    Type listType = getClass().getMethod("listMethod").getGenericReturnType();
    ParameterizedTypeReference<?> typeReference = ParameterizedTypeReference.forType(listType);
    assertThat(typeReference.getType()).isEqualTo(listType);
  }

  @Test
  void nestedParameterizedTypeReference() {
    ParameterizedTypeReference<Map<String, List<Integer>>> typeReference = new ParameterizedTypeReference<>() { };
    assertThat(typeReference.getType()).isInstanceOf(ParameterizedType.class);
    ParameterizedType parameterizedType = (ParameterizedType) typeReference.getType();
    assertThat(parameterizedType.getRawType()).isEqualTo(Map.class);
    assertThat(parameterizedType.getActualTypeArguments()).hasSize(2);
  }

  @Test
  void resolvableTypeWrapsUnderlyingType() throws Exception {
    Type listType = getClass().getMethod("listMethod").getGenericReturnType();
    ParameterizedTypeReference<List<String>> typeReference = ParameterizedTypeReference.forType(listType);
    ResolvableType resolvableType = typeReference.getResolvableType();

    assertThat(resolvableType.getRawClass()).isEqualTo(List.class);
    assertThat(resolvableType.getGeneric(0).getRawClass()).isEqualTo(String.class);
  }

  @Test
  void forTypeWithRawType() {
    ParameterizedTypeReference<?> typeRef = ParameterizedTypeReference.forType(String.class);
    assertThat(typeRef.getType()).isEqualTo(String.class);
  }

  @Test
  void equalsWithSameTypeReference() {
    ParameterizedTypeReference<List<String>> ref1 = new ParameterizedTypeReference<>() { };
    ParameterizedTypeReference<List<String>> ref2 = new ParameterizedTypeReference<>() { };
    assertThat(ref1).isEqualTo(ref2);
  }

  @Test
  void equalsWithDifferentTypeReference() {
    ParameterizedTypeReference<List<String>> ref1 = new ParameterizedTypeReference<>() { };
    ParameterizedTypeReference<List<Integer>> ref2 = new ParameterizedTypeReference<>() { };
    assertThat(ref1).isNotEqualTo(ref2);
  }

  @Test
  void hashCodeWithSameTypeReference() {
    ParameterizedTypeReference<List<String>> ref1 = new ParameterizedTypeReference<>() { };
    ParameterizedTypeReference<List<String>> ref2 = new ParameterizedTypeReference<>() { };
    assertThat(ref1.hashCode()).isEqualTo(ref2.hashCode());
  }

  @Test
  void toStringContainsTypeInformation() {
    ParameterizedTypeReference<List<String>> ref = new ParameterizedTypeReference<>() { };
    assertThat(ref.toString()).contains("ParameterizedTypeReference<");
    assertThat(ref.toString()).contains("java.util.List<java.lang.String>");
  }

  @Test
  void forTypeWithWildcardType() throws Exception {
    Type wildcardType = getClass().getMethod("wildcardMethod").getGenericReturnType();
    ParameterizedTypeReference<?> typeRef = ParameterizedTypeReference.forType(wildcardType);
    assertThat(typeRef.getType()).isEqualTo(wildcardType);
  }

  @Test
  void resolvableTypeWithWildcardBounds() {
    ParameterizedTypeReference<List<? extends Number>> ref = new ParameterizedTypeReference<>() { };
    ResolvableType resolvableType = ref.getResolvableType();
    assertThat(resolvableType.getGeneric(0).resolve()).isEqualTo(Number.class);
  }

  @Test
  void deepNestedParameterizedType() {
    ParameterizedTypeReference<Map<String, List<Map<Integer, String>>>> ref =
            new ParameterizedTypeReference<>() { };
    assertThat(ref.getType()).isInstanceOf(ParameterizedType.class);
  }

  @Test
  void boundedWildcardInheritanceResolvesCorrectly() {
    BoundedWildcardTypeReference typeRef = new BoundedWildcardTypeReference();
    ResolvableType resolvableType = typeRef.getResolvableType();
    assertThat(resolvableType.resolve()).isEqualTo(Number.class);
  }

  @Test
  void wildcardInheritanceResolvesCorrectly() {
    ParameterizedTypeReference<List<?>> typeRef = new WildcardTypeReference() { };
    assertThat(typeRef.getType()).isInstanceOf(ParameterizedType.class);
  }

  private static class WildcardTypeReference extends ParameterizedTypeReference<List<?>> {
  }

  private static class BoundedWildcardTypeReference<T extends Number> extends ParameterizedTypeReference<T> {
  }

  public static List<?> wildcardMethod() {
    return null;
  }

  private static class FirstLevelTypeReference<T> extends ParameterizedTypeReference<T> {
  }

  private static class SecondLevelTypeReference extends FirstLevelTypeReference<String> {
  }

  public static Map<Object, String> mapMethod() {
    return null;
  }

  public static List<String> listMethod() {
    return null;
  }

}