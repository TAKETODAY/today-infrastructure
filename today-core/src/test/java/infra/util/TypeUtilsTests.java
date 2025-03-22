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

package infra.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 20:20
 */
class TypeUtilsTests {

  @Test
  void sameTypesAreAssignable() {
    assertThat(TypeUtils.isAssignable(String.class, String.class)).isTrue();
  }

  @Test
  void objectClassIsAssignableFromAnyType() {
    assertThat(TypeUtils.isAssignable(Object.class, String.class)).isTrue();
    assertThat(TypeUtils.isAssignable(Object.class, Integer.class)).isTrue();
  }

  @Test
  void subclassIsAssignableToSuperclass() {
    assertThat(TypeUtils.isAssignable(Number.class, Integer.class)).isTrue();
  }

  @Test
  void arrayTypesAreAssignableIfComponentTypesAreAssignable() {
    assertThat(TypeUtils.isAssignable(Number[].class, Integer[].class)).isTrue();
  }

  @Test
  void parameterizedTypeIsAssignableToRawType() {
    ParameterizedType listOfStrings = (ParameterizedType) new ArrayList<String>() { }.getClass().getGenericSuperclass();
    assertThat(TypeUtils.isAssignable(ArrayList.class, listOfStrings)).isTrue();
  }

  @Test
  void genericArrayTypeIsHandledCorrectly() {
    class GenericArray<T> {
      T[] array;
    }
    Type genericArrayType = GenericArray.class.getDeclaredFields()[0].getGenericType();
    assertThat(TypeUtils.isAssignable(Object[].class, genericArrayType)).isTrue();
  }

  @Test
  void nullTypeArgumentsAreNotAllowed() {
    assertThatThrownBy(() -> TypeUtils.isAssignable(null, String.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Left-hand side type is required");

    assertThatThrownBy(() -> TypeUtils.isAssignable(String.class, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Right-hand side type is required");
  }

  @Test
  void incompatibleTypesAreNotAssignable() {
    assertThat(TypeUtils.isAssignable(String.class, Integer.class)).isFalse();
    assertThat(TypeUtils.isAssignable(Integer.class, String.class)).isFalse();
  }

  @Test
  void parameterizedTypesWithDifferentArgumentsAreNotAssignable() {
    ParameterizedType listOfStrings = (ParameterizedType) new ArrayList<String>() { }.getClass().getGenericSuperclass();
    ParameterizedType listOfIntegers = (ParameterizedType) new ArrayList<Integer>() { }.getClass().getGenericSuperclass();
    assertThat(TypeUtils.isAssignable(listOfStrings, listOfIntegers)).isFalse();
  }

  @Test
  void notAssignableBoundHandlesNullTypes() {
    assertThat(TypeUtils.isNotAssignableBound(String.class, null)).isFalse();
    assertThat(TypeUtils.isNotAssignableBound(null, String.class)).isTrue();
    assertThat(TypeUtils.isNotAssignableBound(null, null)).isFalse();
  }

  @Test
  void nestedParameterizedTypesAreHandled() {
    class Outer<T> {
      class Inner<U> { }
    }
    Type nestedType = new Outer<String>().new Inner<Integer>() { }.getClass().getGenericSuperclass();
    assertThat(TypeUtils.isAssignable(Outer.Inner.class, nestedType)).isTrue();
  }

  @Test
  void genericArraysOfDifferentDimensionsAreNotAssignable() {
    class Container<T> {
      T[] singleArray;
      T[][] doubleArray;
    }
    Type singleArrayType = Container.class.getDeclaredFields()[0].getGenericType();
    Type doubleArrayType = Container.class.getDeclaredFields()[1].getGenericType();

    assertThat(TypeUtils.isAssignable(singleArrayType, doubleArrayType)).isFalse();
  }

  @Test
  void wildcardTypeWithExplicitBounds() {
    class ContainerWithBounds<T> { }
    class ExtendedContainer<T extends Number> extends ContainerWithBounds<T> { }

    Type boundedType = ExtendedContainer.class.getGenericSuperclass();
    assertThat(TypeUtils.isAssignable(ContainerWithBounds.class, boundedType)).isTrue();
  }

}