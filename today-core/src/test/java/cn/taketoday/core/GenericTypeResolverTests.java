/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static cn.taketoday.util.ReflectionUtils.findMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author TODAY 2021/3/8 19:29
 * @since 3.0
 */
public class GenericTypeResolverTests {

  @Test
  void simpleInterfaceType() {
    assertEquals(String.class, GenericTypeResolver.resolveTypeArgument(MySimpleInterfaceType.class, MyInterfaceType.class));
  }

  @Test
  void simpleCollectionInterfaceType() {
    assertEquals(Collection.class, GenericTypeResolver.resolveTypeArgument(MyCollectionInterfaceType.class, MyInterfaceType.class));
  }

  @Test
  void simpleSuperclassType() {
    assertEquals(String.class, GenericTypeResolver.resolveTypeArgument(MySimpleSuperclassType.class, MySuperclassType.class));
  }

  @Test
  void simpleCollectionSuperclassType() {
    assertEquals(Collection.class, GenericTypeResolver.resolveTypeArgument(MyCollectionSuperclassType.class, MySuperclassType.class));
  }

  @Test
  void nullIfNotResolvable() {
    GenericClass<String> obj = new GenericClass<>();
    assertNull(GenericTypeResolver.resolveTypeArgument(obj.getClass(), GenericClass.class));
  }

  @Test
  void methodReturnTypes() {
    assertEquals(Integer.class,
            GenericTypeResolver.resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "integer"), MyInterfaceType.class));
    assertEquals(String.class,
            GenericTypeResolver.resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "string"), MyInterfaceType.class));
    assertNull(GenericTypeResolver.resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "raw"), MyInterfaceType.class));
    assertNull(GenericTypeResolver.resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "object"), MyInterfaceType.class));
  }

  @Test
  void testBoundParameterizedType() {
    assertEquals(B.class, GenericTypeResolver.resolveTypeArgument(TestImpl.class, ITest.class));
  }

  public interface MyInterfaceType<T> {
  }

  public class MySimpleInterfaceType implements MyInterfaceType<String> {
  }

  public class MyCollectionInterfaceType implements MyInterfaceType<Collection<String>> {
  }

  public abstract class MySuperclassType<T> {
  }

  public class MySimpleSuperclassType extends MySuperclassType<String> {
  }

  public class MyCollectionSuperclassType extends MySuperclassType<Collection<String>> {
  }

  public static class MyTypeWithMethods<T> {

    public MyInterfaceType<Integer> integer() {
      return null;
    }

    public MySimpleInterfaceType string() {
      return null;
    }

    public Object object() {
      return null;
    }

    @SuppressWarnings("rawtypes")
    public MyInterfaceType raw() {
      return null;
    }

    public String notParameterized() {
      return null;
    }

    public String notParameterizedWithArguments(Integer x, Boolean b) {
      return null;
    }

    /**
     * Simulates a factory method that wraps the supplied object in a proxy of the
     * same type.
     */
    public static <T> T createProxy(T object) {
      return null;
    }

    /**
     * Similar to {@link #createProxy(Object)} but adds an additional argument before
     * the argument of type {@code T}. Note that they may potentially be of the same
     * time when invoked!
     */
    public static <T> T createNamedProxy(String name, T object) {
      return null;
    }

    /**
     * Simulates factory methods found in libraries such as Mockito and EasyMock.
     */
    public static <MOCK> MOCK createMock(Class<MOCK> toMock) {
      return null;
    }

    /**
     * Similar to {@link #createMock(Class)} but adds an additional method argument
     * before the parameterized argument.
     */
    public static <T> T createNamedMock(String name, Class<T> toMock) {
      return null;
    }

    /**
     * Similar to {@link #createNamedMock(String, Class)} but adds an additional
     * parameterized type.
     */
    public static <V extends Object, T> T createVMock(V name, Class<T> toMock) {
      return null;
    }

    /**
     * Extract some value of the type supported by the interface (i.e., by a concrete,
     * non-generic implementation of the interface).
     */
    public static <T> T extractValueFrom(MyInterfaceType<T> myInterfaceType) {
      return null;
    }

    /**
     * Extract some magic value from the supplied map.
     */
    public static <K, V> V extractMagicValue(Map<K, V> map) {
      return null;
    }

    void readIntegerInputMessage(MyInterfaceType<Integer> message) {
    }

    void readIntegerArrayInputMessage(MyInterfaceType<Integer>[] message) {
    }

    void readGenericArrayInputMessage(T[] message) {
    }
  }

  public static class MySimpleTypeWithMethods extends MyTypeWithMethods<Integer> {
  }

  static class GenericClass<T> {
  }

  class A { }

  class B<T> { }

  class ITest<T> { }

  class TestImpl<I extends A, T extends B<I>> extends ITest<T> {
  }
}
