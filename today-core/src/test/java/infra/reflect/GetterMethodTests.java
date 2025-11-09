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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/27 15:58
 */
class GetterMethodTests {

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

    public short get_short() {
      return _short;
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

    POJO1._staticVolatileBoolean = true;
    POJO1._staticVolatileByte = 17;
    POJO1._staticVolatileShort = 87;
    POJO1._staticVolatileInt = Integer.MIN_VALUE;
    POJO1._staticVolatileLong = 1337;
    POJO1._staticVolatileChar = 'a';
    POJO1._staticVolatileDouble = Math.PI;
    POJO1._staticVolatileFloat = (float) Math.log(93);
    POJO1._staticVolatileObj = pojo;

    POJO1._staticBoolean = true;
    POJO1._staticByte = 17;
    POJO1._staticShort = 87;
    POJO1._staticInt = Integer.MIN_VALUE;
    POJO1._staticLong = 1337;
    POJO1._staticChar = 'a';
    POJO1._staticDouble = Math.PI;
    POJO1._staticFloat = (float) Math.log(93);
    POJO1._staticObj = pojo;

    final Field[] declaredFields = pojo.getClass().getDeclaredFields();
    for (Field field : declaredFields) {
      final String name = field.getName();
      if (name.charAt(0) == '_') {
        final GetterMethod getter = GetterMethod.forField(field);

        Object val1 = field.get(pojo);
        assertEquals(val1, getter.get(pojo));
      }
    }

    Method[] methods = pojo.getClass().getDeclaredMethods();
    for (Method method : methods) {
      if (!method.getName().startsWith("get_"))
        continue;

      Field field = pojo.getClass().getDeclaredField(method.getName().substring(3));

      GetterMethod getter = GetterMethod.forMethod(method);

      Object val1 = field.get(pojo);
      assertEquals(val1, getter.get(pojo));
    }
  }

  @Test
  void shouldCreateGetterMethodForFieldWithExistingReadMethod() throws NoSuchFieldException, NoSuchMethodException {
    Field field = POJO1.class.getDeclaredField("_int");
    Method readMethod = POJO1.class.getDeclaredMethod("get_int");

    GetterMethod getter = GetterMethod.forField(field);

    assertThat(getter).isNotNull();
    assertThat(getter.getReadMethod()).isEqualTo(readMethod);
  }

  @Test
  void shouldCreateGetterMethodForMethod() throws NoSuchMethodException {
    Method method = POJO1.class.getDeclaredMethod("get_int");

    GetterMethod getter = GetterMethod.forMethod(method);

    assertThat(getter).isNotNull();
    POJO1 pojo = new POJO1();
    pojo._int = 42;
    assertThat(getter.get(pojo)).isEqualTo(42);
  }

  @Test
  void shouldCreateGetterMethodForMethodInvoker() throws NoSuchMethodException {
    Method method = POJO1.class.getDeclaredMethod("get_int");
    MethodInvoker invoker = MethodInvoker.forMethod(method);

    GetterMethod getter = GetterMethod.forMethod(invoker);

    assertThat(getter).isNotNull();
    POJO1 pojo = new POJO1();
    pojo._int = 42;
    assertThat(getter.get(pojo)).isEqualTo(42);
  }

  @Test
  void shouldCreateReflectiveGetterMethod() throws NoSuchFieldException {
    Field field = POJO1.class.getDeclaredField("_int");

    GetterMethod getter = GetterMethod.forReflective(field);

    assertThat(getter).isNotNull();
    POJO1 pojo = new POJO1();
    pojo._int = 42;
    assertThat(getter.get(pojo)).isEqualTo(42);
  }

  @Test
  void shouldGetMethodAccessorGetterMethod() throws NoSuchMethodException {
    Method method = POJO1.class.getDeclaredMethod("get_int");
    MethodInvoker invoker = MethodInvoker.forMethod(method);
    MethodAccessorGetterMethod getter = new MethodAccessorGetterMethod(invoker);

    assertThat(getter).isNotNull();
    assertThat(getter.getReadMethod()).isEqualTo(method);

    assertThat(new GetterMethod() {
      @Override
      public @Nullable Object get(Object obj) {
        return null;
      }
    }.getReadMethod()).isNull();
  }

}