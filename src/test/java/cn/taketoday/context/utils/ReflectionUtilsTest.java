/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import cn.taketoday.context.reflect.GetterMethod;
import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.context.reflect.ReflectionException;
import cn.taketoday.context.reflect.SetterMethod;
import lombok.Getter;
import lombok.Setter;

import static org.junit.Assert.assertNotEquals;

/**
 * @author TODAY <br>
 *         2020-08-13 21:55
 */
public class ReflectionUtilsTest extends TestCase {

    public static class POJO1 {

    }

    public void testCreate() {
        Object o = ReflectionUtils.newConstructor(POJO1.class).newInstance();
        assertNotNull(o);
        assertSame(POJO1.class, o.getClass());
    }

    public static class POJO3 {
        public boolean constructorInvoked = true;
    }

    public static class POJO4 extends POJO3 {
        public boolean pojo4_constructorInvoked = true;
    }

    public void testCallConstructor() {
        POJO3 pojo3 = ReflectionUtils.newConstructor(POJO3.class).newInstance();
        assertNotNull(pojo3);
        assertTrue(pojo3.constructorInvoked);
    }

    public void testCallParentConstructor() {
        POJO4 pojo = ReflectionUtils.newConstructor(POJO4.class).newInstance();
        assertNotNull(pojo);
        assertTrue(pojo.constructorInvoked);
        assertTrue(pojo.pojo4_constructorInvoked);
    }

    public static class SetterMethodTest extends TestCase {
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
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                POJO1 pojo1 = (POJO1) o;

                if (_boolean != pojo1._boolean) return false;
                if (_byte != pojo1._byte) return false;
                if (_char != pojo1._char) return false;
                if (Double.compare(pojo1._double, _double) != 0) return false;
                if (Float.compare(pojo1._float, _float) != 0) return false;
                if (_int != pojo1._int) return false;
                if (_long != pojo1._long) return false;
                if (_short != pojo1._short) return false;

                return Objects.equals(_obj, pojo1._obj);
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
                    final SetterMethod setter = ReflectionUtils.newUnsafeSetterMethod(field);

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
                if (!method.getName().startsWith("set_")) continue;
                Field field = pojo1.getClass()
                        .getDeclaredField(method.getName().substring(3));

                SetterMethod setter = ReflectionUtils.newSetterMethod(method);
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
                if (!method.getName().startsWith("set_")) continue;

                Field field = pojo1.getClass().getDeclaredField(method.getName().substring(3));
                SetterMethod setter = ReflectionUtils.newSetterMethod(method);
                Object val1 = field.get(pojo1);
                assertNotNull(val1);

                setter.set(pojo1, null);

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

    public static class GetterMethodTest extends TestCase {
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
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                POJO1 pojo1 = (POJO1) o;

                if (_boolean != pojo1._boolean) return false;
                if (_byte != pojo1._byte) return false;
                if (_char != pojo1._char) return false;
                if (Double.compare(pojo1._double, _double) != 0) return false;
                if (Float.compare(pojo1._float, _float) != 0) return false;
                if (_int != pojo1._int) return false;
                if (_long != pojo1._long) return false;
                if (_short != pojo1._short) return false;

                return Objects.equals(_obj, pojo1._obj);
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
                    final GetterMethod getter = ReflectionUtils.newUnsafeGetterMethod(field);

                    Object val1 = field.get(pojo);
                    assertEquals(val1, getter.get(pojo));
                }
            }

            Method[] methods = pojo.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (!method.getName().startsWith("get_")) continue;

                Field field = pojo.getClass().getDeclaredField(method.getName().substring(3));

                GetterMethod getter = ReflectionUtils.newGetterMethod(method);

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

    public void testNewPropertyAccessor() throws NoSuchFieldException {
        final PropertyBean propertyBean = new PropertyBean();

        final Field declaredField = PropertyBean.class.getDeclaredField("static_pro");
        final PropertyAccessor staticProAccessor = ReflectionUtils.newPropertyAccessor(declaredField);

        assertEquals(staticProAccessor.get(null), 0);
        staticProAccessor.set(null, 2);
        assertEquals(staticProAccessor.get(null), 2);

        final Field boolField = PropertyBean.class.getDeclaredField("bool");
        final PropertyAccessor boolAccessor = ReflectionUtils.newPropertyAccessor(boolField);

        assertEquals(boolAccessor.get(propertyBean), false);
        boolAccessor.set(propertyBean, true);
        assertEquals(boolAccessor.get(propertyBean), true);

        final Field finalProField = PropertyBean.class.getDeclaredField("finalPro");
        final PropertyAccessor finalProAccessor = ReflectionUtils.newPropertyAccessor(finalProField);
        assertEquals(finalProAccessor.get(propertyBean), 10L);

        try {
            finalProAccessor.set(null, 101);
        }
        catch (ReflectionException e) {
            assertEquals(finalProAccessor.get(propertyBean), 10L);
        }
        final Field staticFinalProField = PropertyBean.class.getDeclaredField("staticFinalPro");
        final PropertyAccessor staticFinalProAccessor = ReflectionUtils.newPropertyAccessor(staticFinalProField);
        assertEquals(staticFinalProAccessor.get(propertyBean), (short) 100);

        try {
            staticFinalProAccessor.set(null, 101);
        }
        catch (ReflectionException e) {
            assertEquals(staticFinalProAccessor.get(propertyBean), (short) 100);
        }

    }

}
