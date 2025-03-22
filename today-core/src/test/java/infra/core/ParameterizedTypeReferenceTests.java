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