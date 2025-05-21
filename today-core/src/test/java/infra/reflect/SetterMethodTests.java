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

package infra.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/27 16:04
 */
class SetterMethodTests {

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

    public static void set_staticInt(int _staticInt) {
      POJO1._staticInt = _staticInt;
    }

    public void set_boolean(boolean _boolean) {
      this._boolean = _boolean;
    }

    public void set_byte(byte _byte) {
      this._byte = _byte;
    }

    public void set_char(char _char) {
      this._char = _char;
    }

    public void set_double(double _double) {
      this._double = _double;
    }

    public void set_float(float _float) {
      this._float = _float;
    }

    public void set_int(int _int) {
      this._int = _int;
    }

    public void set_long(long _long) {
      this._long = _long;
    }

    public void set_obj(Object _obj) {
      this._obj = _obj;
    }

    public void set_short(short _short) {
      this._short = _short;
    }

    public static void set_staticBoolean(boolean _staticBoolean) {
      POJO1._staticBoolean = _staticBoolean;
    }

    public static void set_staticByte(byte _staticByte) {
      POJO1._staticByte = _staticByte;
    }

    public static void set_staticChar(char _staticChar) {
      POJO1._staticChar = _staticChar;
    }

    public static void set_staticDouble(double _staticDouble) {
      POJO1._staticDouble = _staticDouble;
    }

    public static void set_staticFloat(float _staticFloat) {
      POJO1._staticFloat = _staticFloat;
    }

    public static void set_staticLong(long _staticLong) {
      POJO1._staticLong = _staticLong;
    }

    public static void set_staticObj(Object _staticObj) {
      POJO1._staticObj = _staticObj;
    }

    public static void set_staticShort(short _staticShort) {
      POJO1._staticShort = _staticShort;
    }

    public static void set_staticVolatileBoolean(boolean _staticVolatileBoolean) {
      POJO1._staticVolatileBoolean = _staticVolatileBoolean;
    }

    public static void set_staticVolatileByte(byte _staticVolatileByte) {
      POJO1._staticVolatileByte = _staticVolatileByte;
    }

    public static void set_staticVolatileChar(char _staticVolatileChar) {
      POJO1._staticVolatileChar = _staticVolatileChar;
    }

    public static void set_staticVolatileDouble(double _staticVolatileDouble) {
      POJO1._staticVolatileDouble = _staticVolatileDouble;
    }

    public static void set_staticVolatileFloat(float _staticVolatileFloat) {
      POJO1._staticVolatileFloat = _staticVolatileFloat;
    }

    public static void set_staticVolatileInt(int _staticVolatileInt) {
      POJO1._staticVolatileInt = _staticVolatileInt;
    }

    public static void set_staticVolatileLong(long _staticVolatileLong) {
      POJO1._staticVolatileLong = _staticVolatileLong;
    }

    public static void set_staticVolatileObj(Object _staticVolatileObj) {
      POJO1._staticVolatileObj = _staticVolatileObj;
    }

    public static void set_staticVolatileShort(short _staticVolatileShort) {
      POJO1._staticVolatileShort = _staticVolatileShort;
    }

    public void set_volatileBoolean(boolean _volatileBoolean) {
      this._volatileBoolean = _volatileBoolean;
    }

    public void set_volatileByte(byte _volatileByte) {
      this._volatileByte = _volatileByte;
    }

    public void set_volatileChar(char _volatileChar) {
      this._volatileChar = _volatileChar;
    }

    public void set_volatileDouble(double _volatileDouble) {
      this._volatileDouble = _volatileDouble;
    }

    public void set_volatileFloat(float _volatileFloat) {
      this._volatileFloat = _volatileFloat;
    }

    public void set_volatileInt(int _volatileInt) {
      this._volatileInt = _volatileInt;
    }

    public void set_volatileLong(long _volatileLong) {
      this._volatileLong = _volatileLong;
    }

    public void set_volatileObj(Object _volatileObj) {
      this._volatileObj = _volatileObj;
    }

    public void set_volatileShort(short _volatileShort) {
      this._volatileShort = _volatileShort;
    }

    public boolean is_boolean() {
      return _boolean;
    }

    public byte get_byte() {
      return _byte;
    }

    public char get_char() {
      return _char;
    }

    public double get_double() {
      return _double;
    }

    public float get_float() {
      return _float;
    }

    public int get_int() {
      return _int;
    }

    public long get_long() {
      return _long;
    }

    public Object get_obj() {
      return _obj;
    }

    public short get_short() {
      return _short;
    }

    public static boolean is_staticBoolean() {
      return _staticBoolean;
    }

    public static byte get_staticByte() {
      return _staticByte;
    }

    public static char get_staticChar() {
      return _staticChar;
    }

    public static double get_staticDouble() {
      return _staticDouble;
    }

    public static float get_staticFloat() {
      return _staticFloat;
    }

    public static int get_staticInt() {
      return _staticInt;
    }

    public static long get_staticLong() {
      return _staticLong;
    }

    public static Object get_staticObj() {
      return _staticObj;
    }

    public static short get_staticShort() {
      return _staticShort;
    }

    public static boolean is_staticVolatileBoolean() {
      return _staticVolatileBoolean;
    }

    public static byte get_staticVolatileByte() {
      return _staticVolatileByte;
    }

    public static char get_staticVolatileChar() {
      return _staticVolatileChar;
    }

    public static double get_staticVolatileDouble() {
      return _staticVolatileDouble;
    }

    public static float get_staticVolatileFloat() {
      return _staticVolatileFloat;
    }

    public static int get_staticVolatileInt() {
      return _staticVolatileInt;
    }

    public static long get_staticVolatileLong() {
      return _staticVolatileLong;
    }

    public static Object get_staticVolatileObj() {
      return _staticVolatileObj;
    }

    public static short get_staticVolatileShort() {
      return _staticVolatileShort;
    }

    public boolean is_volatileBoolean() {
      return _volatileBoolean;
    }

    public byte get_volatileByte() {
      return _volatileByte;
    }

    public char get_volatileChar() {
      return _volatileChar;
    }

    public double get_volatileDouble() {
      return _volatileDouble;
    }

    public float get_volatileFloat() {
      return _volatileFloat;
    }

    public int get_volatileInt() {
      return _volatileInt;
    }

    public long get_volatileLong() {
      return _volatileLong;
    }

    public Object get_volatileObj() {
      return _volatileObj;
    }

    public short get_volatileShort() {
      return _volatileShort;
    }

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

    POJO1._staticVolatileBoolean = true;
    POJO1._staticVolatileByte = 17;
    POJO1._staticVolatileShort = 87;
    POJO1._staticVolatileInt = Integer.MIN_VALUE;
    POJO1._staticVolatileLong = 1337;
    POJO1._staticVolatileChar = 'a';
    POJO1._staticVolatileDouble = Math.PI;
    POJO1._staticVolatileFloat = (float) Math.log(93);
    POJO1._staticVolatileObj = pojo1;

    POJO1._staticBoolean = true;
    POJO1._staticByte = 17;
    POJO1._staticShort = 87;
    POJO1._staticInt = Integer.MIN_VALUE;
    POJO1._staticLong = 1337;
    POJO1._staticChar = 'a';
    POJO1._staticDouble = Math.PI;
    POJO1._staticFloat = (float) Math.log(93);
    POJO1._staticObj = pojo1;

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