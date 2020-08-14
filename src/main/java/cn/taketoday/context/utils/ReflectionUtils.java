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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.context.reflect.BeanConstructor;
import cn.taketoday.context.reflect.GetterMethod;
import cn.taketoday.context.reflect.ReflectionException;
import cn.taketoday.context.reflect.SetterMethod;
import sun.misc.Unsafe;
import sun.reflect.ConstructorAccessor;
import sun.reflect.FieldAccessor;
import sun.reflect.MethodAccessor;

/**
 * Fast reflection operation
 * 
 * @author TODAY <br>
 *         2020-08-13 18:45
 */
//@SuppressWarnings("restriction")
public abstract class ReflectionUtils {

    private static final MethodAccessor CONSTRUCTOR;
    private static final MethodAccessor FIELD_ACCESSOR;
    private static final MethodAccessor GENERATE_METHOD;
    private static final MethodAccessor SERIAL_CONSTRUCTOR;
    private static final ThreadLocal<Object> GENERATOR_OBJECT_HOLDER;

    static {
        try {
            Class<?> aClass = Class.forName("sun.reflect.MethodAccessorGenerator");
            Constructor<?> declaredConstructor = aClass.getDeclaredConstructors()[0];
            declaredConstructor.setAccessible(true);
            Object generatorObject = declaredConstructor.newInstance();
            Method bar = aClass.getMethod("generateMethod", Class.class, String.class, Class[].class, Class.class, Class[].class,
                                          int.class);
            bar.setAccessible(true);
            GENERATE_METHOD = (MethodAccessor) bar.invoke(generatorObject,
                                                          bar.getDeclaringClass(),
                                                          bar.getName(),
                                                          bar.getParameterTypes(),
                                                          bar.getReturnType(),
                                                          bar.getExceptionTypes(),
                                                          bar.getModifiers());
            bar = aClass.getMethod("generateConstructor", Class.class, Class[].class, Class[].class, Integer.TYPE);
            CONSTRUCTOR = newMethodAccessor(generatorObject, bar);
            bar = aClass.getMethod("generateSerializationConstructor",
                                   Class.class,
                                   Class[].class,
                                   Class[].class,
                                   Integer.TYPE,
                                   Class.class);
            final ConstructorAccessor goc = newConstructorAccessor(generatorObject, declaredConstructor);
            GENERATOR_OBJECT_HOLDER = ThreadLocal.withInitial(() -> {
                try {
                    return goc.newInstance(null);
                }
                catch (Throwable e) {
                    throw new ReflectionException(e);
                }
            });
            SERIAL_CONSTRUCTOR = newMethodAccessor(generatorObject, bar);
            aClass = Class.forName("sun.reflect.UnsafeFieldAccessorFactory");
            bar = aClass.getDeclaredMethod("newFieldAccessor", Field.class, boolean.class);
            FIELD_ACCESSOR = newMethodAccessor(generatorObject, bar);
        }
        catch (Throwable e) {
            throw new ReflectionException(e);
        }
    }

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
     *
     * @throws ReflectionException
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
        throw new ReflectionException("No such method named: " + methodName + " in class: " + targetClass.getName());
    }

    // MethodAccessor
    // --------------------------------

    public static FieldAccessor newFieldAccessor(Field field, boolean overrideFinalCheck) {
        try {
            return (FieldAccessor) FIELD_ACCESSOR.invoke(null, new Object[] { field, overrideFinalCheck });
        }
        catch (InvocationTargetException e) {
            throw new ReflectionException(e.getCause());
        }
    }

    public static MethodAccessor newMethodAccessor(Method bar) {
        try {
            return newMethodAccessor(getGeneratorObject(), bar);
        }
        catch (InvocationTargetException e) {
            throw new ReflectionException(e.getCause());
        }
    }

    private static MethodAccessor newMethodAccessor(Object generatorObject, Method method) throws InvocationTargetException {
        Assert.notNull(method, "method must not be null");
        return (MethodAccessor) GENERATE_METHOD.invoke(generatorObject, new Object[] { //
            method.getDeclaringClass(), //
            method.getName(), //
            method.getParameterTypes(), //
            method.getReturnType(), //
            method.getExceptionTypes(), //
            method.getModifiers()//
        });
    }

    public static ConstructorAccessor newConstructorAccessor(Constructor<?> constructor) {
        Assert.notNull(constructor, "constructor must not be null");

        try {
            return newConstructorAccessor(getGeneratorObject(), constructor);
        }
        catch (InvocationTargetException e) {
            throw new ReflectionException(e.getCause());
        }
    }

    private static ConstructorAccessor newConstructorAccessor(Object generatorObject, Constructor<?> bar) throws InvocationTargetException {
        Assert.notNull(generatorObject, "generatorObject must not be null");
        return (ConstructorAccessor) CONSTRUCTOR.invoke(generatorObject, new Object[] { //
            bar.getDeclaringClass(), //
            bar.getParameterTypes(), //
            bar.getExceptionTypes(), //
            bar.getModifiers()//
        });
    }

    public static ConstructorAccessor newSerialConstructorAccessor(Constructor<?> constructor, Class<?> targetClass) {
        Assert.notNull(constructor, "constructor must not be null");
        try {
            return (ConstructorAccessor) SERIAL_CONSTRUCTOR.invoke(getGeneratorObject(), new Object[] { //
                targetClass, //
                constructor.getParameterTypes(), //
                constructor.getExceptionTypes(), //
                constructor.getModifiers(), //
                constructor.getDeclaringClass() //
            });
        }
        catch (InvocationTargetException e) {
            throw new ReflectionException(e.getCause());
        }
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
            catch (InstantiationException e) {
                throw new ReflectionException(targetClass + " is an abstract, interface, array , primitive, or {@code void}", e);
            }
            catch (InvocationTargetException e) {
                throw new ReflectionException("Exception occurred in constructor: " + targetClass, e.getTargetException());
            }
        };
    }

    // GetterMethod
    // ------------------------------

    public static GetterMethod newGetterMethod(final Field field) {
        Assert.notNull(field, "field must not be null");
        return newGetterMethod(field.getName(), field.getDeclaringClass());
    }

    public static GetterMethod newGetterMethod(final String name, final Class<?> declaringClass) {
        return newGetterMethod(getDeclaredMethod("get".concat(StringUtils.capitalize(name)), declaringClass));
    }

    public static GetterMethod newGetterMethod(final Method method) {
        final MethodAccessor methodAccessor = newMethodAccessor(method);
        return obj -> {
            try {
                return methodAccessor.invoke(obj, null);
            }
            catch (InvocationTargetException e) {
                throw new ReflectionException("error while calling getter method with name " + method.getName() + " on class " + obj
                        .getClass().toString(), e);
            }
        };
    }

    public static SetterMethod newSetterMethod(final Field field) {
        return newSetterMethod(field.getName(), field.getType(), field.getDeclaringClass());
    }

    public static SetterMethod newSetterMethod(final String name, final Class<?> type, final Class<?> declaringClass) {
        final Method setMethod = getDeclaredMethod("set".concat(StringUtils.capitalize(name)), declaringClass, type);
        return newSetterMethod(setMethod, type);
    }

    public static SetterMethod newSetterMethod(final Method method) {
        return newSetterMethod(method, method.getParameterTypes()[0]);
    }

    public static SetterMethod newSetterMethod(final Method method, Class<?> type) {
        final MethodAccessor methodAccessor = newMethodAccessor(method);
        return (Object obj, Object value) -> {
            if (value == null && type.isPrimitive()) return;
            try {
                methodAccessor.invoke(obj, new Object[] { value });
            }
            catch (InvocationTargetException e) {
                throw new ReflectionException("error while calling setter method with name " + method.getName() +
                        " on class " + obj.getClass(), e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> BeanConstructor<T> getUnsafeConstructor(final Class<?> targetClass) {
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

    protected static Object getGeneratorObject() {
        return GENERATOR_OBJECT_HOLDER.get();
    }

    // --------------------- Unsafe -----------------------

    public static GetterMethod newUnsafeGetterMethod(final Field field) {
        final Class<?> type = field.getType();
        final Unsafe theUnsafe = getUnsafe();

        final long offset = Modifier.isStatic(field.getModifiers())
                ? theUnsafe.staticFieldOffset(field)
                : theUnsafe.objectFieldOffset(field);

        if (Modifier.isVolatile(field.getModifiers())) {
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

    public static SetterMethod newUnsafeSetterMethod(final Field field) {
        final Class<?> type = field.getType();
        final boolean isStatic = Modifier.isStatic(field.getModifiers());
        final Unsafe theUnsafe = ReflectionUtils.getUnsafe();
        final long offset = isStatic
                ? theUnsafe.staticFieldOffset(field)
                : theUnsafe.objectFieldOffset(field);

        if (!Modifier.isVolatile(field.getModifiers())) {
            if (type == Boolean.TYPE) {
                return (obj, value) -> {
                    if (value == null) return;
                    theUnsafe.putBoolean(obj, offset, (Boolean) value);
                };
            }
            if (type == Character.TYPE) {
                return (obj, value) -> {
                    if (value == null) return;
                    theUnsafe.putChar(obj, offset, (Character) value);
                };
            }
            if (type == Byte.TYPE) {
                return (obj, value) -> {
                    if (value == null) return;
                    theUnsafe.putByte(obj, offset, ((Number) value).byteValue());
                };
            }
            if (type == Short.TYPE) {
                return (obj, value) -> {
                    if (value == null) return;
                    theUnsafe.putShort(obj, offset, ((Number) value).shortValue());
                };
            }
            if (type == Integer.TYPE) {
                return (obj, value) -> {
                    if (value == null) return;
                    theUnsafe.putInt(obj, offset, ((Number) value).intValue());
                };
            }
            if (type == Long.TYPE) {
                return (obj, value) -> {
                    if (value == null) return;
                    theUnsafe.putLong(obj, offset, ((Number) value).longValue());
                };
            }
            if (type == Float.TYPE) {
                return (obj, value) -> {
                    if (value == null) return;
                    theUnsafe.putFloat(obj, offset, ((Number) value).floatValue());
                };
            }
            if (type == Double.TYPE) {
                return (obj, value) -> {
                    if (value == null) return;
                    theUnsafe.putDouble(obj, offset, ((Number) value).doubleValue());
                };
            }
            return (obj, value) -> theUnsafe.putObject(obj, offset, value);
        }

        if (type == Boolean.TYPE) {
            return (obj, value) -> {
                if (value == null) return;
                theUnsafe.putBooleanVolatile(obj, offset, (Boolean) value);
            };
        }
        if (type == Character.TYPE) {
            return (obj, value) -> {
                if (value == null) return;
                theUnsafe.putCharVolatile(obj, offset, (Character) value);
            };
        }
        if (type == Byte.TYPE) {
            return (obj, value) -> {
                if (value == null) return;
                theUnsafe.putByteVolatile(obj, offset, ((Number) value).byteValue());
            };
        }
        if (type == Short.TYPE) {
            return (obj, value) -> {
                if (value == null) return;
                theUnsafe.putShortVolatile(obj, offset, ((Number) value).shortValue());
            };
        }
        if (type == Integer.TYPE) {
            return (obj, value) -> {
                if (value == null) return;
                theUnsafe.putIntVolatile(obj, offset, ((Number) value).intValue());
            };
        }
        if (type == Long.TYPE) {
            return (obj, value) -> {
                if (value == null) return;
                theUnsafe.putLongVolatile(obj, offset, ((Number) value).longValue());
            };
        }
        if (type == Float.TYPE) {
            return (obj, value) -> {
                if (value == null) return;
                theUnsafe.putFloatVolatile(obj, offset, ((Number) value).floatValue());
            };
        }
        if (type == Double.TYPE) {
            return (obj, value) -> {
                if (value == null) return;
                theUnsafe.putDoubleVolatile(obj, offset, ((Number) value).doubleValue());
            };
        }
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
