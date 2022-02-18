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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.beans.propertyeditors.StringTrimmerEditor;
import cn.taketoday.beans.support.ArgumentConvertingMethodInvoker;
import cn.taketoday.util.ReflectiveMethodInvoker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/18 11:15
 */
class MethodInvokingFactoryBeanTests {

  @Test
  public void testParameterValidation() throws Exception {

    // assert that only static OR non static are set, but not both or none
    MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
    assertThatIllegalArgumentException().isThrownBy(mcfb::afterPropertiesSet);

    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetObject(this);
    mcfb.setTargetMethod("whatever");
    assertThatExceptionOfType(NoSuchMethodException.class).isThrownBy(mcfb::afterPropertiesSet);

    // bogus static method
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("some.bogus.Method.name");
    assertThatExceptionOfType(NoSuchMethodException.class).isThrownBy(mcfb::afterPropertiesSet);

    // bogus static method
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("method1");
    assertThatIllegalArgumentException().isThrownBy(mcfb::afterPropertiesSet);

    // missing method
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetObject(this);
    assertThatIllegalArgumentException().isThrownBy(mcfb::afterPropertiesSet);

    // bogus method
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetObject(this);
    mcfb.setTargetMethod("bogus");
    assertThatExceptionOfType(NoSuchMethodException.class).isThrownBy(mcfb::afterPropertiesSet);

    // static method
    TestClass1._staticField1 = 0;
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("staticMethod1");
    mcfb.afterPropertiesSet();

    // non-static method
    TestClass1 tc1 = new TestClass1();
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetObject(tc1);
    mcfb.setTargetMethod("method1");
    mcfb.afterPropertiesSet();
  }

  @Test
  public void testGetObjectType() throws Exception {
    TestClass1 tc1 = new TestClass1();
    MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetObject(tc1);
    mcfb.setTargetMethod("method1");
    mcfb.afterPropertiesSet();
    assertThat(int.class.equals(mcfb.getObjectType())).isTrue();

    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("voidRetvalMethod");
    mcfb.afterPropertiesSet();
    Class<?> objType = mcfb.getObjectType();
    assertThat(void.class).isSameAs(objType);

    // verify that we can call a method with args that are subtypes of the
    // target method arg types
    TestClass1._staticField1 = 0;
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("supertypes");
    mcfb.setArguments(new ArrayList<>(), new ArrayList<>(), "hello");
    mcfb.afterPropertiesSet();
    mcfb.getObjectType();

    // fail on improper argument types at afterPropertiesSet
    mcfb = new MethodInvokingFactoryBean();
    mcfb.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("supertypes");
    mcfb.setArguments("1", new Object());
    assertThatExceptionOfType(NoSuchMethodException.class).isThrownBy(mcfb::afterPropertiesSet);
  }

  @Test
  public void testGetObject() throws Exception {
    // singleton, non-static
    TestClass1 tc1 = new TestClass1();
    MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetObject(tc1);
    mcfb.setTargetMethod("method1");
    mcfb.afterPropertiesSet();
    Integer i = (Integer) mcfb.getObject();
    assertThat(i.intValue()).isEqualTo(1);
    i = (Integer) mcfb.getObject();
    assertThat(i.intValue()).isEqualTo(1);

    // non-singleton, non-static
    tc1 = new TestClass1();
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetObject(tc1);
    mcfb.setTargetMethod("method1");
    mcfb.setSingleton(false);
    mcfb.afterPropertiesSet();
    i = (Integer) mcfb.getObject();
    assertThat(i.intValue()).isEqualTo(1);
    i = (Integer) mcfb.getObject();
    assertThat(i.intValue()).isEqualTo(2);

    // singleton, static
    TestClass1._staticField1 = 0;
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("staticMethod1");
    mcfb.afterPropertiesSet();
    i = (Integer) mcfb.getObject();
    assertThat(i.intValue()).isEqualTo(1);
    i = (Integer) mcfb.getObject();
    assertThat(i.intValue()).isEqualTo(1);

    // non-singleton, static
    TestClass1._staticField1 = 0;
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setStaticMethod("cn.taketoday.beans.factory.support.MethodInvokingFactoryBeanTests$TestClass1.staticMethod1");
    mcfb.setSingleton(false);
    mcfb.afterPropertiesSet();
    i = (Integer) mcfb.getObject();
    assertThat(i.intValue()).isEqualTo(1);
    i = (Integer) mcfb.getObject();
    assertThat(i.intValue()).isEqualTo(2);

    // void return value
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("voidRetvalMethod");
    mcfb.afterPropertiesSet();
    assertThat(mcfb.getObject()).isNull();

    // now see if we can match methods with arguments that have supertype arguments
    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("supertypes");
    mcfb.setArguments(new ArrayList<>(), new ArrayList<>(), "hello");
    // should pass
    mcfb.afterPropertiesSet();
  }

  @Test
  public void testArgumentConversion() throws Exception {
    MethodInvokingFactoryBean mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("supertypes");
    mcfb.setArguments(new ArrayList<>(), new ArrayList<>(), "hello", "bogus");
    assertThatExceptionOfType(NoSuchMethodException.class).as(
            "Matched method with wrong number of args").isThrownBy(
            mcfb::afterPropertiesSet);

    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("supertypes");
    mcfb.setArguments(1, new Object());
    assertThatExceptionOfType(NoSuchMethodException.class).as(
            "Should have failed on getObject with mismatched argument types").isThrownBy(
            mcfb::afterPropertiesSet);

    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("supertypes2");
    mcfb.setArguments(new ArrayList<>(), new ArrayList<>(), "hello", "bogus");
    mcfb.afterPropertiesSet();
    assertThat(mcfb.getObject()).isEqualTo("hello");

    mcfb = new MethodInvokingFactoryBean();
    mcfb.setTargetClass(TestClass1.class);
    mcfb.setTargetMethod("supertypes2");
    mcfb.setArguments(new ArrayList<>(), new ArrayList<>(), new Object());
    assertThatExceptionOfType(NoSuchMethodException.class).as(
            "Matched method when shouldn't have matched").isThrownBy(
            mcfb::afterPropertiesSet);
  }

  @Test
  public void testInvokeWithNullArgument() throws Exception {
    ReflectiveMethodInvoker methodInvoker = new ReflectiveMethodInvoker();
    methodInvoker.setTargetClass(TestClass1.class);
    methodInvoker.setTargetMethod("nullArgument");
    methodInvoker.setArguments(new Object[] { null });
    methodInvoker.prepare();
    methodInvoker.invoke();
  }

  @Test
  public void testInvokeWithIntArgument() throws Exception {
    ArgumentConvertingMethodInvoker methodInvoker = new ArgumentConvertingMethodInvoker();
    methodInvoker.setTargetClass(TestClass1.class);
    methodInvoker.setTargetMethod("intArgument");
    methodInvoker.setArguments(5);
    methodInvoker.prepare();
    methodInvoker.invoke();

    methodInvoker = new ArgumentConvertingMethodInvoker();
    methodInvoker.setTargetClass(TestClass1.class);
    methodInvoker.setTargetMethod("intArgument");
    methodInvoker.setArguments(5);
    methodInvoker.prepare();
    methodInvoker.invoke();
  }

  @Test
  public void testInvokeWithIntArguments() throws Exception {
    MethodInvokingBean methodInvoker = new MethodInvokingBean();
    methodInvoker.setTargetClass(TestClass1.class);
    methodInvoker.setTargetMethod("intArguments");
    methodInvoker.setArguments(new Object[] { new Integer[] { 5, 10 } });
    methodInvoker.afterPropertiesSet();

    methodInvoker = new MethodInvokingBean();
    methodInvoker.setTargetClass(TestClass1.class);
    methodInvoker.setTargetMethod("intArguments");
    methodInvoker.setArguments(new Object[] { new String[] { "5", "10" } });
    methodInvoker.afterPropertiesSet();

    methodInvoker = new MethodInvokingBean();
    methodInvoker.setTargetClass(TestClass1.class);
    methodInvoker.setTargetMethod("intArguments");
    methodInvoker.setArguments(new Object[] { new Integer[] { 5, 10 } });
    methodInvoker.afterPropertiesSet();

    methodInvoker = new MethodInvokingBean();
    methodInvoker.setTargetClass(TestClass1.class);
    methodInvoker.setTargetMethod("intArguments");
    methodInvoker.setArguments("5", "10");
    methodInvoker.afterPropertiesSet();

    methodInvoker = new MethodInvokingBean();
    methodInvoker.setTargetClass(TestClass1.class);
    methodInvoker.setTargetMethod("intArguments");
    methodInvoker.setArguments(new Object[] { new Integer[] { 5, 10 } });
    methodInvoker.afterPropertiesSet();

    methodInvoker = new MethodInvokingBean();
    methodInvoker.setTargetClass(TestClass1.class);
    methodInvoker.setTargetMethod("intArguments");
    methodInvoker.setArguments("5", "10");
    methodInvoker.afterPropertiesSet();
  }

  public static class TestClass1 {

    public static int _staticField1;

    public int _field1 = 0;

    public int method1() {
      return ++_field1;
    }

    public static int staticMethod1() {
      return ++TestClass1._staticField1;
    }

    public static void voidRetvalMethod() {
    }

    public static void nullArgument(Object arg) {
    }

    public static void intArgument(int arg) {
    }

    public static void intArguments(int[] arg) {
    }

    public static String supertypes(Collection<?> c, Integer i) {
      return i.toString();
    }

    public static String supertypes(Collection<?> c, List<?> l, String s) {
      return s;
    }

    public static String supertypes2(Collection<?> c, List<?> l, Integer i) {
      return i.toString();
    }

    public static String supertypes2(Collection<?> c, List<?> l, String s, Integer i) {
      return s;
    }

    public static String supertypes2(Collection<?> c, List<?> l, String s, String s2) {
      return s;
    }
  }

}