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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.context.reflect.BeanConstructor;
import cn.taketoday.context.reflect.ConstructorAccessor;
import cn.taketoday.context.reflect.ConstructorAccessorGenerator;
import cn.taketoday.context.reflect.GetterMethod;
import cn.taketoday.context.reflect.GetterSetterPropertyAccessor;
import cn.taketoday.context.reflect.MethodAccessor;
import cn.taketoday.context.reflect.MethodAccessorPropertyAccessor;
import cn.taketoday.context.reflect.MethodInvoker;
import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.context.reflect.ReadOnlyGetterMethodPropertyAccessor;
import cn.taketoday.context.reflect.ReadOnlyMethodAccessorPropertyAccessor;
import cn.taketoday.context.reflect.ReflectionException;
import cn.taketoday.context.reflect.SetterMethod;
import sun.misc.Unsafe;

/**
 * Fast reflection operation
 *
 * @author TODAY <br>
 *         2020-08-13 18:45
 */
//@SuppressWarnings("restriction")
public abstract class ReflectionUtils {

    //
    // ------------------------------

    /**
     * Get declared method
     *
     * @param methodName
     *            Method name
     * @param targetClass
     *            Target class
     * @param parameterTypes
     *            Parameter types
     *
     * @return Declared method
     */
    public static Method getDeclaredMethod(final String methodName,
                                           final Class<?> targetClass,
                                           final Class<?>... parameterTypes) {
        Assert.notNull(targetClass, "targetClass must not be null");
        Class<?> current = targetClass;
        while (current != null) {
            try {
                return current.getDeclaredMethod(methodName, parameterTypes);
            }
            catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    public static Method obtainDeclaredMethod(final String methodName,
                                              final Class<?> targetClass,
                                              final Class<?>... parameterTypes) {
        final Method declaredMethod = getDeclaredMethod(methodName, targetClass, parameterTypes);
        if (declaredMethod == null) {
            throw new ReflectionException("No such method named: " + methodName + " in class: " + targetClass.getName());
        }
        return declaredMethod;
    }

    // Accessor
    // --------------------------------

    public static PropertyAccessor newPropertyAccessor(final Field field) {
        Assert.notNull(field, "field must not be null");

        final String propertyName = field.getName();
        final String capitalizeProperty = StringUtils.capitalize(propertyName);
        final Class<?> type = field.getType();
        final Class<?> declaringClass = field.getDeclaringClass();

        final Method getMethod = getDeclaredMethod(getterPropertyName(capitalizeProperty, type), declaringClass);

        final boolean isReadOnly = Modifier.isFinal(field.getModifiers());
        if (isReadOnly && getMethod != null) {
            return new ReadOnlyMethodAccessorPropertyAccessor(newMethodAccessor(getMethod));
        }

        Method setMethod = getDeclaredMethod("set".concat(capitalizeProperty), declaringClass, type);
        if (setMethod != null && getMethod != null) {
            MethodAccessor setMethodAccessor = newMethodAccessor(setMethod);
            MethodAccessor getMethodAccessor = newMethodAccessor(getMethod);
            return new MethodAccessorPropertyAccessor(setMethodAccessor, getMethodAccessor);
        }

        final GetterMethod getterMethod = newUnsafeGetterMethod(field);
        if (isReadOnly) {
            return new ReadOnlyGetterMethodPropertyAccessor(getterMethod);
        }
        final SetterMethod setterMethod = newUnsafeSetterMethod(field);
        return new GetterSetterPropertyAccessor(getterMethod, setterMethod);
    }

    /**
     * Create a new {@link MethodAccessor}
     *
     * @param method
     *   Target method
     *
     * @return MethodAccessor to access Method
     */
    public static MethodAccessor newMethodAccessor(final Method method) {
        return MethodInvoker.create(method);
    }

    public static ConstructorAccessor newConstructorAccessor(final Constructor<?> constructor) {
        return new ConstructorAccessorGenerator(constructor).create();
    }

    /**
     * Get target class's {@link BeanConstructor}
     *
     * @param targetClass
     *            Target class
     *
     * @return {@link BeanConstructor}
     */
    @SuppressWarnings("unchecked")
    public static <T> BeanConstructor<T> newConstructor(final Class<T> targetClass) {
        final Constructor<T> suitableConstructor = ClassUtils.getSuitableConstructor(targetClass);
        if (suitableConstructor == null) {
            throw new ReflectionException("No suitable constructor in class: " + targetClass);
        }
        final ConstructorAccessor constructorAccessor = newConstructorAccessor(suitableConstructor);
        return args -> {
            try {
                return (T) constructorAccessor.newInstance(args);
            }
            catch (IllegalArgumentException e) {
                throw new ReflectionException("Illegal Argument in constructor: " + targetClass, e);
            }
        };
    }

    // GetterMethod
    // ---------------------

    public static GetterMethod newGetterMethod(final Field field) {
        Assert.notNull(field, "field must not be null");
        return newGetterMethod(field.getName(), field.getType(), field.getDeclaringClass());
    }

    public static GetterMethod newGetterMethod(final String name, final Class<?> type, final Class<?> declaringClass) {
        return newGetterMethod(obtainDeclaredMethod(getterPropertyName(name, type), declaringClass, type));
    }

    private static String getterPropertyName(final String name, final Class<?> type) {
        return (type == boolean.class ? "is" : "get").concat(name);
    }

    public static GetterMethod newGetterMethod(final Method method) {
        final MethodAccessor methodAccessor = newMethodAccessor(method);
        return obj -> methodAccessor.invoke(obj, null);
    }

    // SetterMethod
    // ----------------------

    public static SetterMethod newSetterMethod(final Field field) {
        return newSetterMethod(field.getName(), field.getType(), field.getDeclaringClass());
    }

    public static SetterMethod newSetterMethod(final String name, final Class<?> type, final Class<?> declaringClass) {
        final Method setMethod = obtainDeclaredMethod("set".concat(StringUtils.capitalize(name)), declaringClass, type);
        return newSetterMethod(setMethod, type);
    }

    public static SetterMethod newSetterMethod(final Method method) {
        return newSetterMethod(method, method.getParameterTypes()[0]);
    }

    public static SetterMethod newSetterMethod(final Method method, Class<?> type) {
        final MethodAccessor methodAccessor = newMethodAccessor(method);
        return (Object obj, Object value) -> {
            if (value == null && type.isPrimitive()) {
                return;
            }
            methodAccessor.invoke(obj, new Object[] { value });
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> BeanConstructor<T> newUnsafeConstructor(final Class<?> targetClass) {
        final Unsafe theUnsafe = getUnsafe();
        return args -> {
            try {
                return (T) theUnsafe.allocateInstance(targetClass);
            }
            catch (InstantiationException e) {
                throw new ReflectionException("Could not create a new instance of class " + targetClass, e);
            }
        };
    }

    // --------------------- Unsafe -----------------------

    public static GetterMethod newUnsafeGetterMethod(final Field field) {
        Assert.notNull(field, "field must not be null");

        Class<?> type = field.getType();
        boolean isFinal = Modifier.isFinal(field.getModifiers());
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        boolean isVolatile = Modifier.isVolatile(field.getModifiers());
        boolean isQualified = isFinal || isVolatile;
        final Unsafe theUnsafe = getUnsafe();
        final long offset = isStatic ? theUnsafe.staticFieldOffset(field) : theUnsafe.objectFieldOffset(field);

        if (isStatic) {
            // This code path does not guarantee that the field's
            // declaring class has been initialized, but it must be
            // before performing reflective operations.
            theUnsafe.ensureClassInitialized(field.getDeclaringClass());
            final Object base = theUnsafe.staticFieldBase(field); // base
            if (!isQualified) {
                if (type == Boolean.TYPE) return obj -> theUnsafe.getBoolean(base, offset);
                if (type == Byte.TYPE) return obj -> theUnsafe.getByte(base, offset);
                if (type == Short.TYPE) return obj -> theUnsafe.getShort(base, offset);
                if (type == Character.TYPE) return obj -> theUnsafe.getChar(base, offset);
                if (type == Integer.TYPE) return obj -> theUnsafe.getInt(base, offset);
                if (type == Long.TYPE) return obj -> theUnsafe.getLong(base, offset);
                if (type == Float.TYPE) return obj -> theUnsafe.getFloat(base, offset);
                if (type == Double.TYPE) return obj -> theUnsafe.getDouble(base, offset);
                return obj -> theUnsafe.getObject(base, offset);
            }
            if (type == Boolean.TYPE) return obj -> theUnsafe.getBooleanVolatile(base, offset);
            if (type == Character.TYPE) return obj -> theUnsafe.getCharVolatile(base, offset);
            if (type == Byte.TYPE) return obj -> theUnsafe.getByteVolatile(base, offset);
            if (type == Short.TYPE) return obj -> theUnsafe.getShortVolatile(base, offset);
            if (type == Integer.TYPE) return obj -> theUnsafe.getIntVolatile(base, offset);
            if (type == Long.TYPE) return obj -> theUnsafe.getLongVolatile(base, offset);
            if (type == Float.TYPE) return obj -> theUnsafe.getFloatVolatile(base, offset);
            if (type == Double.TYPE) return obj -> theUnsafe.getDoubleVolatile(base, offset);
            return obj -> theUnsafe.getObjectVolatile(base, offset);
        }
        if (!isQualified) {
            if (type == Boolean.TYPE) return obj -> theUnsafe.getBoolean(obj, offset);
            if (type == Character.TYPE) return obj -> theUnsafe.getChar(obj, offset);
            if (type == Byte.TYPE) return obj -> theUnsafe.getByte(obj, offset);
            if (type == Short.TYPE) return obj -> theUnsafe.getShort(obj, offset);
            if (type == Integer.TYPE) return obj -> theUnsafe.getInt(obj, offset);
            if (type == Long.TYPE) return obj -> theUnsafe.getLong(obj, offset);
            if (type == Float.TYPE) return obj -> theUnsafe.getFloat(obj, offset);
            if (type == Double.TYPE) return obj -> theUnsafe.getDouble(obj, offset);
            return obj -> theUnsafe.getObject(obj, offset);
        }
        if (type == Boolean.TYPE) return obj -> theUnsafe.getBooleanVolatile(obj, offset);
        if (type == Character.TYPE) return obj -> theUnsafe.getCharVolatile(obj, offset);
        if (type == Byte.TYPE) return obj -> theUnsafe.getByteVolatile(obj, offset);
        if (type == Short.TYPE) return obj -> theUnsafe.getShortVolatile(obj, offset);
        if (type == Integer.TYPE) return obj -> theUnsafe.getIntVolatile(obj, offset);
        if (type == Long.TYPE) return obj -> theUnsafe.getLongVolatile(obj, offset);
        if (type == Float.TYPE) return obj -> theUnsafe.getFloatVolatile(obj, offset);
        if (type == Double.TYPE) return obj -> theUnsafe.getDoubleVolatile(obj, offset);
        return obj -> theUnsafe.getObjectVolatile(obj, offset);
    }

    public static SetterMethod newUnsafeSetterMethod(final Field field) {
        Assert.notNull(field, "field must not be null");

        Class<?> type = field.getType();
        boolean isFinal = Modifier.isFinal(field.getModifiers());
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        boolean isVolatile = Modifier.isVolatile(field.getModifiers());
        boolean isQualified = isFinal || isVolatile;
        final Unsafe theUnsafe = getUnsafe();
        final long offset = isStatic ? theUnsafe.staticFieldOffset(field) : theUnsafe.objectFieldOffset(field);

        if (isStatic) {
            // This code path does not guarantee that the field's
            // declaring class has been initialized, but it must be
            // before performing reflective operations.
            theUnsafe.ensureClassInitialized(field.getDeclaringClass());
            final Object base = theUnsafe.staticFieldBase(field); // base
            if (!isQualified) {
                if (type == Boolean.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putBoolean(base, offset, (Boolean) value);
                };
                if (type == Character.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putChar(base, offset, (Character) value);
                };
                if (type == Byte.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putByte(base, offset, ((Number) value).byteValue());
                };
                if (type == Short.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putShort(base, offset, ((Number) value).shortValue());
                };
                if (type == Integer.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putInt(base, offset, ((Number) value).intValue());
                };
                if (type == Long.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putLong(base, offset, ((Number) value).longValue());
                };
                if (type == Float.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putFloat(base, offset, ((Number) value).floatValue());
                };
                if (type == Double.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putDouble(base, offset, ((Number) value).doubleValue());
                };
                return (obj, value) -> theUnsafe.putObject(base, offset, value);
            }
            else {
                if (type == Boolean.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putBooleanVolatile(base, offset, (Boolean) value);
                };
                if (type == Character.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putCharVolatile(base, offset, (Character) value);
                };
                if (type == Byte.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putByteVolatile(base, offset, ((Number) value).byteValue());
                };
                if (type == Short.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putShortVolatile(base, offset, ((Number) value).shortValue());
                };
                if (type == Integer.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putIntVolatile(base, offset, ((Number) value).intValue());
                };
                if (type == Long.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putLongVolatile(base, offset, ((Number) value).longValue());
                };
                if (type == Float.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putFloatVolatile(base, offset, ((Number) value).floatValue());
                };
                if (type == Double.TYPE) return (obj, value) -> {
                    Assert.notNull(value);
                    theUnsafe.putDoubleVolatile(base, offset, ((Number) value).doubleValue());
                };
                return (obj, value) -> theUnsafe.putObjectVolatile(base, offset, value);
            }
        }
        if (!isQualified) {
            if (type == Boolean.TYPE) return (obj, value) -> {
                Assert.notNull(value);
                theUnsafe.putBoolean(obj, offset, (Boolean) value);
            };
            if (type == Character.TYPE) return (obj, value) -> {
                Assert.notNull(value);
                theUnsafe.putChar(obj, offset, (Character) value);
            };
            if (type == Byte.TYPE) return (obj, value) -> {
                Assert.notNull(value);
                theUnsafe.putByte(obj, offset, ((Number) value).byteValue());
            };
            if (type == Short.TYPE) return (obj, value) -> {
                Assert.notNull(value);
                theUnsafe.putShort(obj, offset, ((Number) value).shortValue());
            };
            if (type == Integer.TYPE) return (obj, value) -> {
                Assert.notNull(value);
                theUnsafe.putInt(obj, offset, ((Number) value).intValue());
            };
            if (type == Long.TYPE) return (obj, value) -> {
                Assert.notNull(value);
                theUnsafe.putLong(obj, offset, ((Number) value).longValue());
            };
            if (type == Float.TYPE) return (obj, value) -> {
                Assert.notNull(value);
                theUnsafe.putFloat(obj, offset, ((Number) value).floatValue());
            };
            if (type == Double.TYPE) return (obj, value) -> {
                Assert.notNull(value);
                theUnsafe.putDouble(obj, offset, ((Number) value).doubleValue());
            };
            return (obj, value) -> theUnsafe.putObject(obj, offset, value);
        }
        if (type == Boolean.TYPE) return (obj, value) -> {
            Assert.notNull(value);
            theUnsafe.putBooleanVolatile(obj, offset, (Boolean) value);
        };
        if (type == Character.TYPE) return (obj, value) -> {
            Assert.notNull(value);
            theUnsafe.putCharVolatile(obj, offset, (Character) value);
        };
        if (type == Byte.TYPE) return (obj, value) -> {
            Assert.notNull(value);
            theUnsafe.putByteVolatile(obj, offset, ((Number) value).byteValue());
        };
        if (type == Short.TYPE) return (obj, value) -> {
            Assert.notNull(value);
            theUnsafe.putShortVolatile(obj, offset, ((Number) value).shortValue());
        };
        if (type == Integer.TYPE) return (obj, value) -> {
            Assert.notNull(value);
            theUnsafe.putIntVolatile(obj, offset, ((Number) value).intValue());
        };
        if (type == Long.TYPE) return (obj, value) -> {
            Assert.notNull(value);
            theUnsafe.putLongVolatile(obj, offset, ((Number) value).longValue());
        };
        if (type == Float.TYPE) return (obj, value) -> {
            Assert.notNull(value);
            theUnsafe.putFloatVolatile(obj, offset, ((Number) value).floatValue());
        };
        if (type == Double.TYPE) return (obj, value) -> {
            Assert.notNull(value);
            theUnsafe.putDoubleVolatile(obj, offset, ((Number) value).doubleValue());
        };
        return (obj, value) -> theUnsafe.putObjectVolatile(obj, offset, value);
    }

    public static Unsafe getUnsafe() {
        return UnsafeHolder.unsafe;
    }

    private static final class UnsafeHolder {
        private static final Unsafe unsafe;

        static {
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field declaredField = unsafeClass.getDeclaredField("theUnsafe");
                declaredField.setAccessible(true);
                unsafe = (Unsafe) declaredField.get(null);
            }
            catch (ClassNotFoundException e) {
                throw new ReflectionException("Can't found 'sun.misc.Unsafe' class", e);
            }
            catch (NoSuchFieldException e) {
                throw new ReflectionException("No Field 'theUnsafe' in the 'sun.misc.Unsafe' class", e);
            }
            catch (IllegalAccessException e) {
                throw new ReflectionException("Illegal Access 'theUnsafe' in the 'sun.misc.Unsafe' class", e);
            }
        }
    }

}
