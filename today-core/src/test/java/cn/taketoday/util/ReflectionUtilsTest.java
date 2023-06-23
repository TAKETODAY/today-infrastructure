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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import cn.taketoday.reflect.GetterMethod;
import cn.taketoday.reflect.PropertyAccessor;
import cn.taketoday.reflect.ReflectionException;
import cn.taketoday.reflect.SetterMethod;
import cn.taketoday.tests.sample.objects.TestObject;
import cn.taketoday.util.ReflectionUtils.MethodFilter;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TODAY <br>
 * 2020-08-13 21:55
 */
public class ReflectionUtilsTest {

  @Test
  void doWithMethodsUsingUserDeclaredMethodsFilterStartingWithObject() {
    ListSavingMethodCallback mc = new ListSavingMethodCallback();
    ReflectionUtils.doWithMethods(Object.class, mc, ReflectionUtils.USER_DECLARED_METHODS);
    assertThat(mc.getMethodNames()).isEmpty();
  }

  @Test
  void doWithMethodsUsingUserDeclaredMethodsFilterStartingWithTestObject() {
    ListSavingMethodCallback mc = new ListSavingMethodCallback();
    ReflectionUtils.doWithMethods(TestObject.class, mc, ReflectionUtils.USER_DECLARED_METHODS);
    assertThat(mc.getMethodNames())
            .as("user declared methods").contains("absquatulate", "compareTo", "getName", "setName", "getAge", "setAge", "getSpouse", "setSpouse")
            .as("methods on Object").doesNotContain("equals", "hashCode", "toString", "clone", "finalize", "getClass", "notify", "notifyAll", "wait");
  }

  @Test
  void doWithMethodsUsingUserDeclaredMethodsComposedFilter() {
    ListSavingMethodCallback mc = new ListSavingMethodCallback();
    // "q" because both absquatulate() and equals() contain "q"
    MethodFilter isSetterMethodOrNameContainsQ = m -> m.getName().startsWith("set") || m.getName().contains("q");
    MethodFilter methodFilter = ReflectionUtils.USER_DECLARED_METHODS.and(isSetterMethodOrNameContainsQ);
    ReflectionUtils.doWithMethods(TestObject.class, mc, methodFilter);
    assertThat(mc.getMethodNames()).containsExactlyInAnyOrder("setName", "setAge", "setSpouse", "absquatulate");
  }

  public static class POJO1 {

  }

  @Test
  public void testCreate() {
    Object o = ReflectionUtils.newInstance(POJO1.class);
    assertNotNull(o);
    assertSame(POJO1.class, o.getClass());
  }

  public static class POJO3 {
    public boolean constructorInvoked = true;
  }

  public static class POJO4 extends POJO3 {
    public boolean pojo4_constructorInvoked = true;
  }

  @Test
  public void testCallConstructor() {
    POJO3 pojo3 = ReflectionUtils.newInstance(POJO3.class);
    assertNotNull(pojo3);
    assertTrue(pojo3.constructorInvoked);
  }

  @Test
  public void testCallParentConstructor() {
    POJO4 pojo = ReflectionUtils.newInstance(POJO4.class);
    assertNotNull(pojo);
    assertTrue(pojo.constructorInvoked);
    assertTrue(pojo.pojo4_constructorInvoked);
  }

  public static class SetterMethodTest {
    @Getter
    @Setter
    public static class POJO1 {

      boolean _boolean;
      byte _byte;
      short _short;
      int _int;
      long _long;
      float _float;
      double _double;
      char _char;
      Object _obj;

      volatile boolean _volatileBoolean;
      volatile byte _volatileByte;
      volatile short _volatileShort;
      volatile int _volatileInt;
      volatile long _volatileLong;
      volatile float _volatileFloat;
      volatile double _volatileDouble;
      volatile char _volatileChar;
      volatile Object _volatileObj;

      static boolean _staticBoolean;
      static byte _staticByte;
      static short _staticShort;
      static int _staticInt;
      static long _staticLong;
      static float _staticFloat;
      static double _staticDouble;
      static char _staticChar;
      static Object _staticObj;

      static volatile boolean _staticVolatileBoolean;
      static volatile byte _staticVolatileByte;
      static volatile short _staticVolatileShort;
      static volatile int _staticVolatileInt;
      static volatile long _staticVolatileLong;
      static volatile float _staticVolatileFloat;
      static volatile double _staticVolatileDouble;
      static volatile char _staticVolatileChar;
      static volatile Object _staticVolatileObj;

      @Override
      public boolean equals(Object o) {
        if (this == o)
          return true;
        if (o == null || getClass() != o.getClass())
          return false;

        POJO1 pojo1 = (POJO1) o;

        if (_boolean != pojo1._boolean)
          return false;
        if (_byte != pojo1._byte)
          return false;
        if (_char != pojo1._char)
          return false;
        if (Double.compare(pojo1._double, _double) != 0)
          return false;
        if (Float.compare(pojo1._float, _float) != 0)
          return false;
        if (_int != pojo1._int)
          return false;
        if (_long != pojo1._long)
          return false;
        if (_short != pojo1._short)
          return false;

        return Objects.equals(_obj, pojo1._obj);
      }

      @Override
      public int hashCode() {
        return Objects.hash(_boolean, _byte, _short, _int, _long, _float, _double, _char, _obj, _volatileBoolean, _volatileByte, _volatileShort, _volatileInt, _volatileLong, _volatileFloat,
                _volatileDouble,
                _volatileChar, _volatileObj);
      }
    }

    public void testAllTypes() throws IllegalAccessException, NoSuchFieldException {
      POJO1 pojo1 = new POJO1();
      pojo1._boolean = true;
      pojo1._byte = 17;
      pojo1._short = 87;
      pojo1._int = Integer.MIN_VALUE;
      pojo1._long = 1337;
      pojo1._char = 'a';
      pojo1._double = Math.PI;
      pojo1._float = (float) Math.log(93);
      pojo1._obj = pojo1;

      pojo1._volatileBoolean = true;
      pojo1._volatileByte = 17;
      pojo1._volatileShort = 87;
      pojo1._volatileInt = Integer.MIN_VALUE;
      pojo1._volatileLong = 1337;
      pojo1._volatileChar = 'a';
      pojo1._volatileDouble = Math.PI;
      pojo1._volatileFloat = (float) Math.log(93);
      pojo1._volatileObj = pojo1;

      pojo1._staticVolatileBoolean = true;
      pojo1._staticVolatileByte = 17;
      pojo1._staticVolatileShort = 87;
      pojo1._staticVolatileInt = Integer.MIN_VALUE;
      pojo1._staticVolatileLong = 1337;
      pojo1._staticVolatileChar = 'a';
      pojo1._staticVolatileDouble = Math.PI;
      pojo1._staticVolatileFloat = (float) Math.log(93);
      pojo1._staticVolatileObj = pojo1;

      pojo1._staticBoolean = true;
      pojo1._staticByte = 17;
      pojo1._staticShort = 87;
      pojo1._staticInt = Integer.MIN_VALUE;
      pojo1._staticLong = 1337;
      pojo1._staticChar = 'a';
      pojo1._staticDouble = Math.PI;
      pojo1._staticFloat = (float) Math.log(93);
      pojo1._staticObj = pojo1;

      POJO1 pojo2 = new POJO1();

      assertNotEquals(pojo1, pojo2);

      final Field[] declaredFields = pojo1.getClass().getDeclaredFields();
      for (Field field : declaredFields) {
        final String name = field.getName();
        if (name.charAt(0) == '_') {
          final SetterMethod setter = SetterMethod.fromField(field);

          Object val1 = field.get(pojo1);
          Object val2 = field.get(pojo2);

          if (Modifier.isStatic(field.getModifiers())) {
            assertEquals(val1, val2);
            setter.set(pojo2, val1);
            Object val3 = field.get(pojo2);
            assertEquals(val1, val3);
            setter.set(pojo2, val2);
          }
          else {
            assertNotEquals(val1, val2);
            setter.set(pojo2, val1);
            Object val3 = field.get(pojo2);
            assertEquals(val1, val3);
            setter.set(pojo2, val2);
          }
        }
      }

      Method[] methods = pojo1.getClass().getDeclaredMethods();
      for (Method method : methods) {
        if (!method.getName().startsWith("set_"))
          continue;
        Field field = pojo1.getClass()
                .getDeclaredField(method.getName().substring(3));

        SetterMethod setter = SetterMethod.fromMethod(method);
        Object val1 = field.get(pojo1);
        Object val2 = field.get(pojo2);

        assertNotEquals(val1, val2);
        setter.set(pojo2, val1);
        Object val3 = field.get(pojo2);
        assertEquals(val1, val3);
      }

      assertEquals(pojo1, pojo2);

      // let's reset all values to NULL
      // primitive fields will not be affected
      for (Method method : methods) {
        if (!method.getName().startsWith("set_"))
          continue;

        Field field = pojo1.getClass().getDeclaredField(method.getName().substring(3));
        SetterMethod setter = SetterMethod.fromMethod(method);
        Object val1 = field.get(pojo1);
        assertNotNull(val1);

        if (!field.getType().isPrimitive()) {
          setter.set(pojo1, null);
        }

        Object val2 = field.get(pojo1);
        if (!field.getType().isPrimitive()) {
          assertNull(val2);
          continue;
        }
        assertNotNull(val2);
        // not affected
        assertEquals(val1, val2);
      }
      pojo2._obj = null;
      assertEquals(pojo2, pojo1);
    }
  }

  // --------------

  public static class GetterMethodTest {
    @Getter
    @Setter
    public static class POJO1 {
      boolean _boolean;
      byte _byte;
      short _short;
      int _int;
      long _long;
      float _float;
      double _double;
      char _char;
      Object _obj;

      volatile boolean _volatileBoolean;
      volatile byte _volatileByte;
      volatile short _volatileShort;
      volatile int _volatileInt;
      volatile long _volatileLong;
      volatile float _volatileFloat;
      volatile double _volatileDouble;
      volatile char _volatileChar;
      volatile Object _volatileObj;

      static boolean _staticBoolean;
      static byte _staticByte;
      static short _staticShort;
      static int _staticInt;
      static long _staticLong;
      static float _staticFloat;
      static double _staticDouble;
      static char _staticChar;
      static Object _staticObj;

      static volatile boolean _staticVolatileBoolean;
      static volatile byte _staticVolatileByte;
      static volatile short _staticVolatileShort;
      static volatile int _staticVolatileInt;
      static volatile long _staticVolatileLong;
      static volatile float _staticVolatileFloat;
      static volatile double _staticVolatileDouble;
      static volatile char _staticVolatileChar;
      static volatile Object _staticVolatileObj;

      @Override
      public boolean equals(Object o) {
        if (this == o)
          return true;
        if (o == null || getClass() != o.getClass())
          return false;

        POJO1 pojo1 = (POJO1) o;

        if (_boolean != pojo1._boolean)
          return false;
        if (_byte != pojo1._byte)
          return false;
        if (_char != pojo1._char)
          return false;
        if (Double.compare(pojo1._double, _double) != 0)
          return false;
        if (Float.compare(pojo1._float, _float) != 0)
          return false;
        if (_int != pojo1._int)
          return false;
        if (_long != pojo1._long)
          return false;
        if (_short != pojo1._short)
          return false;

        return Objects.equals(_obj, pojo1._obj);
      }

      @Override
      public int hashCode() {
        return Objects.hash(_boolean, _byte, _short, _int, _long, _float, _double, _char, _obj, _volatileBoolean, _volatileByte, _volatileShort, _volatileInt, _volatileLong, _volatileFloat,
                _volatileDouble,
                _volatileChar, _volatileObj);
      }
    }

    public void testAllTypes() throws IllegalAccessException, NoSuchFieldException {
      POJO1 pojo = new POJO1();
      pojo._boolean = true;
      pojo._byte = 17;
      pojo._short = 87;
      pojo._int = Integer.MIN_VALUE;
      pojo._long = 1337;
      pojo._char = 'a';
      pojo._double = Math.PI;
      pojo._float = (float) Math.log(93);
      pojo._obj = pojo;

      pojo._volatileBoolean = true;
      pojo._volatileByte = 17;
      pojo._volatileShort = 87;
      pojo._volatileInt = Integer.MIN_VALUE;
      pojo._volatileLong = 1337;
      pojo._volatileChar = 'a';
      pojo._volatileDouble = Math.PI;
      pojo._volatileFloat = (float) Math.log(93);
      pojo._volatileObj = pojo;

      pojo._staticVolatileBoolean = true;
      pojo._staticVolatileByte = 17;
      pojo._staticVolatileShort = 87;
      pojo._staticVolatileInt = Integer.MIN_VALUE;
      pojo._staticVolatileLong = 1337;
      pojo._staticVolatileChar = 'a';
      pojo._staticVolatileDouble = Math.PI;
      pojo._staticVolatileFloat = (float) Math.log(93);
      pojo._staticVolatileObj = pojo;

      pojo._staticBoolean = true;
      pojo._staticByte = 17;
      pojo._staticShort = 87;
      pojo._staticInt = Integer.MIN_VALUE;
      pojo._staticLong = 1337;
      pojo._staticChar = 'a';
      pojo._staticDouble = Math.PI;
      pojo._staticFloat = (float) Math.log(93);
      pojo._staticObj = pojo;

      final Field[] declaredFields = pojo.getClass().getDeclaredFields();
      for (Field field : declaredFields) {
        final String name = field.getName();
        if (name.charAt(0) == '_') {
          final GetterMethod getter = GetterMethod.fromField(field);

          Object val1 = field.get(pojo);
          assertEquals(val1, getter.get(pojo));
        }
      }

      Method[] methods = pojo.getClass().getDeclaredMethods();
      for (Method method : methods) {
        if (!method.getName().startsWith("get_"))
          continue;

        Field field = pojo.getClass().getDeclaredField(method.getName().substring(3));

        GetterMethod getter = GetterMethod.fromMethod(method);

        Object val1 = field.get(pojo);
        assertEquals(val1, getter.get(pojo));
      }
    }

  }

  // -----------------------

  @Getter
  @Setter
  public static class PropertyBean {
    static int static_pro = 0;
    boolean bool = false;
    final long finalPro = 10L;
    static final short staticFinalPro = 100;
  }

  @Test
  public void testNewPropertyAccessor() throws NoSuchFieldException {
    final PropertyBean propertyBean = new PropertyBean();

    final Field declaredField = PropertyBean.class.getDeclaredField("static_pro");
    final PropertyAccessor staticProAccessor = PropertyAccessor.fromField(declaredField);

    assertEquals(staticProAccessor.get(null), 0);
    staticProAccessor.set(null, 2);
    assertEquals(staticProAccessor.get(null), 2);

    final Field boolField = PropertyBean.class.getDeclaredField("bool");
    final PropertyAccessor boolAccessor = PropertyAccessor.fromField(boolField);

    assertEquals(boolAccessor.get(propertyBean), false);
    boolAccessor.set(propertyBean, true);
    assertEquals(boolAccessor.get(propertyBean), true);

    final Field finalProField = PropertyBean.class.getDeclaredField("finalPro");
    final PropertyAccessor finalProAccessor = PropertyAccessor.fromField(finalProField);
    assertEquals(finalProAccessor.get(propertyBean), 10L);

    try {
      finalProAccessor.set(null, 101);
    }
    catch (ReflectionException e) {
      assertEquals(finalProAccessor.get(propertyBean), 10L);
    }
    final Field staticFinalProField = PropertyBean.class.getDeclaredField("staticFinalPro");
    final PropertyAccessor staticFinalProAccessor = PropertyAccessor.fromField(staticFinalProField);
    assertEquals(staticFinalProAccessor.get(propertyBean), (short) 100);

    try {
      staticFinalProAccessor.set(null, 101);
    }
    catch (ReflectionException e) {
      assertEquals(staticFinalProAccessor.get(propertyBean), (short) 100);
    }

  }

  // ------------------------------------------------------------------------------

  @Test
  public void testFindField() {
    Field field = ReflectionUtils.findField(TestObjectSubclassWithPublicField.class, "publicField", String.class);
    assertThat(field).isNotNull();
    assertThat(field.getName()).isEqualTo("publicField");
    assertThat(field.getType()).isEqualTo(String.class);
    assertThat(Modifier.isPublic(field.getModifiers())).as("Field should be public.").isTrue();

    field = ReflectionUtils.findField(TestObjectSubclassWithNewField.class, "prot", String.class);
    assertThat(field).isNotNull();
    assertThat(field.getName()).isEqualTo("prot");
    assertThat(field.getType()).isEqualTo(String.class);
    assertThat(Modifier.isProtected(field.getModifiers())).as("Field should be protected.").isTrue();

    field = ReflectionUtils.findField(TestObjectSubclassWithNewField.class, "name", String.class);
    assertThat(field).isNotNull();
    assertThat(field.getName()).isEqualTo("name");
    assertThat(field.getType()).isEqualTo(String.class);
    assertThat(Modifier.isPrivate(field.getModifiers())).as("Field should be private.").isTrue();
  }

  @Test
  public void testSetField() {
    TestObjectSubclassWithNewField testBean = new TestObjectSubclassWithNewField();
    Field field = ReflectionUtils.findField(TestObjectSubclassWithNewField.class, "name", String.class);

    ReflectionUtils.makeAccessible(field);

    ReflectionUtils.setField(field, testBean, "FooBar");
    assertThat(testBean.getName()).isNotNull();
    assertThat(testBean.getName()).isEqualTo("FooBar");

    ReflectionUtils.setField(field, testBean, null);
    assertThat((Object) testBean.getName()).isNull();
  }

  @Test
  public void testInvokeMethod() throws Exception {
    String rob = "Rob Harrop";

    TestObject bean = new TestObject();
    bean.setName(rob);

    Method getName = TestObject.class.getMethod("getName");
    Method setName = TestObject.class.getMethod("setName", String.class);

    Object name = ReflectionUtils.invokeMethod(getName, bean);
    assertThat(name).as("Incorrect name returned").isEqualTo(rob);

    String juergen = "Juergen Hoeller";
    ReflectionUtils.invokeMethod(setName, bean, juergen);
    assertThat(bean.getName()).as("Incorrect name set").isEqualTo(juergen);
  }

  @Test
  public void testDeclaresException() throws Exception {
    Method remoteExMethod = A.class.getDeclaredMethod("foo", Integer.class);
    assertThat(ReflectionUtils.declaresException(remoteExMethod, RemoteException.class)).isTrue();
    assertThat(ReflectionUtils.declaresException(remoteExMethod, ConnectException.class)).isTrue();
    assertThat(ReflectionUtils.declaresException(remoteExMethod, NoSuchMethodException.class)).isFalse();
    assertThat(ReflectionUtils.declaresException(remoteExMethod, Exception.class)).isFalse();

    Method illegalExMethod = B.class.getDeclaredMethod("bar", String.class);
    assertThat(ReflectionUtils.declaresException(illegalExMethod, IllegalArgumentException.class)).isTrue();
    assertThat(ReflectionUtils.declaresException(illegalExMethod, NumberFormatException.class)).isTrue();
    assertThat(ReflectionUtils.declaresException(illegalExMethod, IllegalStateException.class)).isFalse();
    assertThat(ReflectionUtils.declaresException(illegalExMethod, Exception.class)).isFalse();
  }

  @Test
  public void testCopySrcToDestinationOfIncorrectClass() {
    TestObject src = new TestObject();
    String dest = new String();
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionUtils.shallowCopyFieldState(src, dest));
  }

  @Test
  public void testRejectsNullSrc() {
    TestObject src = null;
    String dest = new String();
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionUtils.shallowCopyFieldState(src, dest));
  }

  @Test
  public void testRejectsNullDest() {
    TestObject src = new TestObject();
    String dest = null;
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionUtils.shallowCopyFieldState(src, dest));
  }

  @Test
  public void testValidCopy() {
    TestObject src = new TestObject();
    TestObject dest = new TestObject();
    testValidCopy(src, dest);
  }

  @Test
  public void testValidCopyOnSubTypeWithNewField() {
    TestObjectSubclassWithNewField src = new TestObjectSubclassWithNewField();
    TestObjectSubclassWithNewField dest = new TestObjectSubclassWithNewField();
    src.magic = 11;

    // Will check inherited fields are copied
    testValidCopy(src, dest);

    // Check subclass fields were copied
    assertThat(dest.magic).isEqualTo(src.magic);
    assertThat(dest.prot).isEqualTo(src.prot);
  }

  @Test
  public void testValidCopyToSubType() {
    TestObject src = new TestObject();
    TestObjectSubclassWithNewField dest = new TestObjectSubclassWithNewField();
    dest.magic = 11;
    testValidCopy(src, dest);
    // Should have left this one alone
    assertThat(dest.magic).isEqualTo(11);
  }

  @Test
  public void testValidCopyToSubTypeWithFinalField() {
    TestObjectSubclassWithFinalField src = new TestObjectSubclassWithFinalField();
    TestObjectSubclassWithFinalField dest = new TestObjectSubclassWithFinalField();
    // Check that this doesn't fail due to attempt to assign final
    testValidCopy(src, dest);
  }

  private void testValidCopy(TestObject src, TestObject dest) {
    src.setName("freddie");
    src.setAge(15);
    src.setSpouse(new TestObject());
    assertThat(src.getAge() == dest.getAge()).isFalse();

    ReflectionUtils.shallowCopyFieldState(src, dest);
    assertThat(dest.getAge()).isEqualTo(src.getAge());
    assertThat(dest.getSpouse()).isEqualTo(src.getSpouse());
  }

  @Test
  public void testDoWithProtectedMethods() {
    ListSavingMethodCallback mc = new ListSavingMethodCallback();
    ReflectionUtils.doWithMethods(TestObject.class, mc, new MethodFilter() {
      @Override
      public boolean matches(Method m) {
        return Modifier.isProtected(m.getModifiers());
      }
    });
    assertThat(mc.getMethodNames().isEmpty()).isFalse();
    assertThat(mc.getMethodNames().contains("clone")).as("Must find protected method on Object").isTrue();
    assertThat(mc.getMethodNames().contains("finalize")).as("Must find protected method on Object").isTrue();
    assertThat(mc.getMethodNames().contains("hashCode")).as("Public, not protected").isFalse();
    assertThat(mc.getMethodNames().contains("absquatulate")).as("Public, not protected").isFalse();
  }

  @Test
  public void testDuplicatesFound() {
    ListSavingMethodCallback mc = new ListSavingMethodCallback();
    ReflectionUtils.doWithMethods(TestObjectSubclass.class, mc);
    int absquatulateCount = 0;
    for (String name : mc.getMethodNames()) {
      if (name.equals("absquatulate")) {
        ++absquatulateCount;
      }
    }
    assertThat(absquatulateCount).as("Found 2 absquatulates").isEqualTo(2);
  }

  @Test
  public void testFindMethod() throws Exception {
    assertThat(ReflectionUtils.findMethod(B.class, "bar", String.class)).isNotNull();
    assertThat(ReflectionUtils.findMethod(B.class, "foo", Integer.class)).isNotNull();
    assertThat(ReflectionUtils.findMethod(B.class, "getClass")).isNotNull();
  }

  @Test
  public void testGetAllDeclaredMethods() throws Exception {
    class Foo {
      @Override
      public String toString() {
        return super.toString();
      }
    }
    int toStringMethodCount = 0;
    final Method[] allDeclaredMethods = ReflectionUtils.getAllDeclaredMethods(Foo.class);
    for (Method method : allDeclaredMethods) {
      if (method.getName().equals("toString")) {
        toStringMethodCount++;
      }
    }
    assertThat(toStringMethodCount).isEqualTo(2);
  }

  @Test
  public void testGetUniqueDeclaredMethods() throws Exception {
    class Foo {
      @Override
      public String toString() {
        return super.toString();
      }
    }
    int toStringMethodCount = 0;
    for (Method method : ReflectionUtils.getUniqueDeclaredMethods(Foo.class)) {
      if (method.getName().equals("toString")) {
        toStringMethodCount++;
      }
    }
    assertThat(toStringMethodCount).isEqualTo(1);
  }

  @Test
  public void testGetUniqueDeclaredMethods_withCovariantReturnType() throws Exception {
    class Parent {
      @SuppressWarnings("unused")
      public Number m1() {
        return Integer.valueOf(42);
      }
    }
    class Leaf extends Parent {
      @Override
      public Integer m1() {
        return Integer.valueOf(42);
      }
    }
    int m1MethodCount = 0;
    Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(Leaf.class);
    for (Method method : methods) {
      if (method.getName().equals("m1")) {
        m1MethodCount++;
      }
    }
    assertThat(m1MethodCount).isEqualTo(1);
  }

  @Test
  public void testGetUniqueDeclaredMethods_isFastEnough() {
    @SuppressWarnings("unused")
    class C { //@formatter:off
            void m00() { } void m01() { } void m02() { } void m03() { } void m04() { }
            void m05() { } void m06() { } void m07() { } void m08() { } void m09() { }
            void m10() { } void m11() { } void m12() { } void m13() { } void m14() { }
            void m15() { } void m16() { } void m17() { } void m18() { } void m19() { }
            void m20() { } void m21() { } void m22() { } void m23() { } void m24() { }
            void m25() { } void m26() { } void m27() { } void m28() { } void m29() { }
            void m30() { } void m31() { } void m32() { } void m33() { } void m34() { }
            void m35() { } void m36() { } void m37() { } void m38() { } void m39() { }
            void m40() { } void m41() { } void m42() { } void m43() { } void m44() { }
            void m45() { } void m46() { } void m47() { } void m48() { } void m49() { }
            void m50() { } void m51() { } void m52() { } void m53() { } void m54() { }
            void m55() { } void m56() { } void m57() { } void m58() { } void m59() { }
            void m60() { } void m61() { } void m62() { } void m63() { } void m64() { }
            void m65() { } void m66() { } void m67() { } void m68() { } void m69() { }
            void m70() { } void m71() { } void m72() { } void m73() { } void m74() { }
            void m75() { } void m76() { } void m77() { } void m78() { } void m79() { }
            void m80() { } void m81() { } void m82() { } void m83() { } void m84() { }
            void m85() { } void m86() { } void m87() { } void m88() { } void m89() { }
            void m90() { } void m91() { } void m92() { } void m93() { } void m94() { }
            void m95() { } void m96() { } void m97() { } void m98() { } void m99() { }
        } //@formatter:on

    Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(C.class);
    assertThat(methods.length).isGreaterThan(100);
  }

  @Test
  public void testGetDeclaredMethodsReturnsCopy() {
    Method[] m1 = ReflectionUtils.getDeclaredMethods(A.class);
    Method[] m2 = ReflectionUtils.getDeclaredMethods(A.class);
    assertThat(m1).isNotSameAs(m2);
  }

  private static class ListSavingMethodCallback implements ReflectionUtils.MethodCallback {

    private List<String> methodNames = new ArrayList<>();

    private List<Method> methods = new ArrayList<>();

    @Override
    public void doWith(Method m) throws IllegalArgumentException, IllegalAccessException {
      this.methodNames.add(m.getName());
      this.methods.add(m);
    }

    public List<String> getMethodNames() {
      return this.methodNames;
    }

    @SuppressWarnings("unused")
    public List<Method> getMethods() {
      return this.methods;
    }
  }

  private static class TestObjectSubclass extends TestObject {

    @Override
    public void absquatulate() {
      throw new UnsupportedOperationException();
    }
  }

  private static class TestObjectSubclassWithPublicField extends TestObject {

    @SuppressWarnings("unused")
    public String publicField = "foo";
  }

  private static class TestObjectSubclassWithNewField extends TestObject {

    private int magic;

    protected String prot = "foo";
  }

  private static class TestObjectSubclassWithFinalField extends TestObject {

    @SuppressWarnings("unused")
    private final String foo = "will break naive copy that doesn't exclude statics";
  }

  private static class A {

    @SuppressWarnings("unused")
    private void foo(Integer i) throws RemoteException { }
  }

  @SuppressWarnings("unused")
  private static class B extends A {

    void bar(String s) throws IllegalArgumentException { }

    int add(int... args) {
      int sum = 0;
      for (int i = 0; i < args.length; i++) {
        sum += args[i];
      }
      return sum;
    }
  }

  // --------------------

  @Test
  public void getterPropertyName() {

    assertThat(ReflectionUtils.getterPropertyName("isName", boolean.class))
            .isEqualTo("isName");
    assertThat(ReflectionUtils.getterPropertyName("isName", Boolean.class))
            .isEqualTo("getIsName");
    assertThat(ReflectionUtils.getterPropertyName("isName", String.class))
            .isEqualTo("getIsName");

  }

  @Test
  public void setterPropertyName() {
    assertThat(ReflectionUtils.setterPropertyName("isName", boolean.class))
            .isEqualTo("setName");
    assertThat(ReflectionUtils.setterPropertyName("isName", Boolean.class))
            .isEqualTo("setIsName");
    assertThat(ReflectionUtils.setterPropertyName("isName", String.class))
            .isEqualTo("setIsName");

  }

  @Test
  public void getMethodIfAvailable() {
    Method method = ReflectionUtils.getMethodIfAvailable(Collection.class, "size");
    assertThat(method).isNotNull();
    assertThat(method.getName()).isEqualTo("size");

    method = ReflectionUtils.getMethodIfAvailable(Collection.class, "remove", Object.class);
    assertThat(method).isNotNull();
    assertThat(method.getName()).isEqualTo("remove");

    assertThat(ReflectionUtils.getMethodIfAvailable(Collection.class, "remove")).isNull();
    assertThat(ReflectionUtils.getMethodIfAvailable(Collection.class, "someOtherMethod")).isNull();
  }

  @Test
  public void hasMethod() {
    assertThat(ReflectionUtils.hasMethod(Collection.class, "size")).isTrue();
    assertThat(ReflectionUtils.hasMethod(Collection.class, "remove", Object.class)).isTrue();
    assertThat(ReflectionUtils.hasMethod(Collection.class, "remove")).isFalse();
    assertThat(ReflectionUtils.hasMethod(Collection.class, "someOtherMethod")).isFalse();
  }

  @Test
  public void getMethodCountForName() {
    assertThat(ReflectionUtils.getMethodCountForName(OverloadedMethodsClass.class, "print"))
            .as("Verifying number of overloaded 'print' methods for OverloadedMethodsClass.")
            .isEqualTo(2);
    assertThat(ReflectionUtils.getMethodCountForName(SubOverloadedMethodsClass.class, "print"))
            .as("Verifying number of overloaded 'print' methods for SubgetPackageNameOverloadedMethodsClass.")
            .isEqualTo(4);
  }

  @Test
  public void argsStaticMethod() throws IllegalAccessException, InvocationTargetException {
    Method method = ReflectionUtils.getStaticMethod(NestedClass.class, "argStaticMethod", String.class);
    method.invoke(null, "test");
    assertThat(NestedClass.argCalled).as("argument method was not invoked.").isTrue();
  }

  @SuppressWarnings("unused")
  private static class OverloadedMethodsClass {

    public void print(String messages) {
      /* no-op */
    }

    public void print(String[] messages) {
      /* no-op */
    }
  }

  @SuppressWarnings("unused")
  private static class SubOverloadedMethodsClass extends OverloadedMethodsClass {

    public void print(String header, String[] messages) {
      /* no-op */
    }

    void print(String header, String[] messages, String footer) {
      /* no-op */
    }
  }

  public static class NestedClass {

    static boolean noArgCalled;
    static boolean argCalled;
    static boolean overloadedCalled;

    public static void staticMethod() {
      noArgCalled = true;
    }

    public static void staticMethod(String anArg) {
      overloadedCalled = true;
    }

    public static void argStaticMethod(String anArg) {
      argCalled = true;
    }
  }

}
