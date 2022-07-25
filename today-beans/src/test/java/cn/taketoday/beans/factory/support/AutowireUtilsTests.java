/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.factory.support;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AutowireUtils}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Loïc Ledoyen
 */
public class AutowireUtilsTests {

  @Test
  public void genericMethodReturnTypes() {
    Method notParameterized = ReflectionUtils.findMethod(MyTypeWithMethods.class, "notParameterized");
    Object actual = AutowireUtils.resolveReturnTypeForFactoryMethod(notParameterized, new Object[0], getClass().getClassLoader());
    assertThat(actual).isEqualTo(String.class);

    Method notParameterizedWithArguments = ReflectionUtils.findMethod(MyTypeWithMethods.class, "notParameterizedWithArguments", Integer.class, Boolean.class);
    assertThat(AutowireUtils.resolveReturnTypeForFactoryMethod(notParameterizedWithArguments, new Object[] { 99, true }, getClass().getClassLoader())).isEqualTo(String.class);

    Method createProxy = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createProxy", Object.class);
    assertThat(AutowireUtils.resolveReturnTypeForFactoryMethod(createProxy, new Object[] { "foo" }, getClass().getClassLoader())).isEqualTo(String.class);

    Method createNamedProxyWithDifferentTypes = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createNamedProxy", String.class, Object.class);
    assertThat(AutowireUtils.resolveReturnTypeForFactoryMethod(createNamedProxyWithDifferentTypes, new Object[] { "enigma", 99L }, getClass().getClassLoader())).isEqualTo(Long.class);

    Method createNamedProxyWithDuplicateTypes = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createNamedProxy", String.class, Object.class);
    assertThat(AutowireUtils.resolveReturnTypeForFactoryMethod(createNamedProxyWithDuplicateTypes, new Object[] { "enigma", "foo" }, getClass().getClassLoader())).isEqualTo(String.class);

    Method createMock = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createMock", Class.class);
    assertThat(AutowireUtils.resolveReturnTypeForFactoryMethod(createMock, new Object[] { Runnable.class }, getClass().getClassLoader())).isEqualTo(Runnable.class);
    assertThat(AutowireUtils.resolveReturnTypeForFactoryMethod(createMock, new Object[] { Runnable.class.getName() }, getClass().getClassLoader())).isEqualTo(Runnable.class);

    Method createNamedMock = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createNamedMock", String.class, Class.class);
    assertThat(AutowireUtils.resolveReturnTypeForFactoryMethod(createNamedMock, new Object[] { "foo", Runnable.class }, getClass().getClassLoader())).isEqualTo(Runnable.class);

    Method createVMock = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createVMock", Object.class, Class.class);
    assertThat(AutowireUtils.resolveReturnTypeForFactoryMethod(createVMock, new Object[] { "foo", Runnable.class }, getClass().getClassLoader())).isEqualTo(Runnable.class);

    // Ideally we would expect String.class instead of Object.class, but
    // resolveReturnTypeForFactoryMethod() does not currently support this form of
    // look-up.
    Method extractValueFrom = ReflectionUtils.findMethod(MyTypeWithMethods.class, "extractValueFrom", MyInterfaceType.class);
    assertThat(AutowireUtils.resolveReturnTypeForFactoryMethod(extractValueFrom, new Object[] { new MySimpleInterfaceType() }, getClass().getClassLoader())).isEqualTo(Object.class);

    // Ideally we would expect Boolean.class instead of Object.class, but this
    // information is not available at run-time due to type erasure.
    Map<Integer, Boolean> map = new HashMap<>();
    map.put(0, false);
    map.put(1, true);
    Method extractMagicValue = ReflectionUtils.findMethod(MyTypeWithMethods.class, "extractMagicValue", Map.class);
    assertThat(AutowireUtils.resolveReturnTypeForFactoryMethod(extractMagicValue, new Object[] { map }, getClass().getClassLoader())).isEqualTo(Object.class);
  }

  public interface MyInterfaceType<T> {
  }

  public class MySimpleInterfaceType implements MyInterfaceType<String> {
  }

  public static class MyTypeWithMethods<T> {

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

    public void readIntegerInputMessage(MyInterfaceType<Integer> message) {
    }

    public void readIntegerArrayInputMessage(MyInterfaceType<Integer>[] message) {
    }

    public void readGenericArrayInputMessage(T[] message) {
    }
  }

}
