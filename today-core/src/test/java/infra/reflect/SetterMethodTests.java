/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import infra.reflect.SetterMethod;
import lombok.Getter;
import lombok.Setter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/27 16:04
 */
class SetterMethodTests {

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

  @Test
  void testAllTypes() throws IllegalAccessException, NoSuchFieldException {
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
        final SetterMethod setter = SetterMethod.forField(field);

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

      SetterMethod setter = SetterMethod.forMethod(method);
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
      SetterMethod setter = SetterMethod.forMethod(method);
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