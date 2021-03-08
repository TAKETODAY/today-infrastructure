/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.utils;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static cn.taketoday.context.utils.GenericTypeResolver.resolveReturnTypeArgument;
import static cn.taketoday.context.utils.GenericTypeResolver.resolveReturnTypeForGenericMethod;
import static cn.taketoday.context.utils.GenericTypeResolver.resolveTypeArgument;
import static cn.taketoday.context.utils.ReflectionUtils.findMethod;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author TODAY 2021/3/8 19:29
 * @since 3.0
 */
public class GenericTypeResolverTests {

  @Test
  public void simpleInterfaceType() {
    assertEquals(String.class, resolveTypeArgument(MySimpleInterfaceType.class, MyInterfaceType.class));
  }

  @Test
  public void simpleCollectionInterfaceType() {
    assertEquals(Collection.class, resolveTypeArgument(MyCollectionInterfaceType.class, MyInterfaceType.class));
  }

  @Test
  public void simpleSuperclassType() {
    assertEquals(String.class, resolveTypeArgument(MySimpleSuperclassType.class, MySuperclassType.class));
  }

  @Test
  public void simpleCollectionSuperclassType() {
    assertEquals(Collection.class, resolveTypeArgument(MyCollectionSuperclassType.class, MySuperclassType.class));
  }

  @Test
  public void nullIfNotResolvable() {
    GenericClass<String> obj = new GenericClass<>();
    assertNull(resolveTypeArgument(obj.getClass(), GenericClass.class));
  }

  @Test
  public void methodReturnTypes() {
    assertEquals(Integer.class,
                 resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "integer"), MyInterfaceType.class));
    assertEquals(String.class,
                 resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "string"), MyInterfaceType.class));
    assertEquals(null, resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "raw"), MyInterfaceType.class));
    assertEquals(null,
                 resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "object"), MyInterfaceType.class));
  }

  /**
   * @since 3.2
   */
  @Test
  public void genericMethodReturnTypes() {
    Method notParameterized = findMethod(MyTypeWithMethods.class, "notParameterized", new Class[] {});
    assertEquals(String.class, resolveReturnTypeForGenericMethod(notParameterized, new Object[] {}));

    Method notParameterizedWithArguments = findMethod(MyTypeWithMethods.class, "notParameterizedWithArguments",
                                                      Integer.class, Boolean.class);
    assertEquals(String.class,
                 resolveReturnTypeForGenericMethod(notParameterizedWithArguments, new Object[] { 99, true }));

    Method createProxy = findMethod(MyTypeWithMethods.class, "createProxy", Object.class);
    assertEquals(String.class, resolveReturnTypeForGenericMethod(createProxy, new Object[] { "foo" }));

    Method createNamedProxyWithDifferentTypes = findMethod(MyTypeWithMethods.class, "createNamedProxy",
                                                           String.class, Object.class);
    // one argument to few
    assertNull(resolveReturnTypeForGenericMethod(createNamedProxyWithDifferentTypes, new Object[] { "enigma" }));
    assertEquals(Long.class,
                 resolveReturnTypeForGenericMethod(createNamedProxyWithDifferentTypes, new Object[] { "enigma", 99L }));

    Method createNamedProxyWithDuplicateTypes = findMethod(MyTypeWithMethods.class, "createNamedProxy",
                                                           String.class, Object.class);
    assertEquals(String.class,
                 resolveReturnTypeForGenericMethod(createNamedProxyWithDuplicateTypes, new Object[] { "enigma", "foo" }));

    Method createMock = findMethod(MyTypeWithMethods.class, "createMock", Class.class);
    assertEquals(Runnable.class, resolveReturnTypeForGenericMethod(createMock, new Object[] { Runnable.class }));

    Method createNamedMock = findMethod(MyTypeWithMethods.class, "createNamedMock", String.class,
                                        Class.class);
    assertEquals(Runnable.class,
                 resolveReturnTypeForGenericMethod(createNamedMock, new Object[] { "foo", Runnable.class }));

    Method createVMock = findMethod(MyTypeWithMethods.class, "createVMock",
                                    Object.class, Class.class);
    assertEquals(Runnable.class,
                 resolveReturnTypeForGenericMethod(createVMock, new Object[] { "foo", Runnable.class }));

    // Ideally we would expect String.class instead of Object.class, but
    // resolveReturnTypeForGenericMethod() does not currently support this form of
    // look-up.
    Method extractValueFrom = findMethod(MyTypeWithMethods.class, "extractValueFrom",
                                         MyInterfaceType.class);
    assertEquals(Object.class,
                 resolveReturnTypeForGenericMethod(extractValueFrom, new Object[] { new MySimpleInterfaceType() }));

    // Ideally we would expect Boolean.class instead of Object.class, but this
    // information is not available at run-time due to type erasure.
    Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();
    map.put(0, false);
    map.put(1, true);
    Method extractMagicValue = findMethod(MyTypeWithMethods.class, "extractMagicValue", Map.class);
    assertEquals(Object.class, resolveReturnTypeForGenericMethod(extractMagicValue, new Object[] { map }));
  }

  @Test
  public void testBoundParameterizedType() {
    assertEquals(B.class, resolveTypeArgument(TestImpl.class, ITest.class));
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

    public void readIntegerInputMessage(MyInterfaceType<Integer> message) {
    }

    public void readIntegerArrayInputMessage(MyInterfaceType<Integer>[] message) {
    }

    public void readGenericArrayInputMessage(T[] message) {
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
