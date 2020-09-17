/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.context.cglib.core;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.STATIC;
import static java.security.AccessController.doPrivileged;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.Attribute;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * @version $Id: ReflectUtils.java,v 1.30 2009/01/11 19:47:49 herbyderby Exp $
 */
@SuppressWarnings("all")
public abstract class ReflectUtils {

    private static final HashMap<String, Class> primitives = new HashMap<>();
    private static final HashMap<String, String> transforms = new HashMap<>();

    private static final ClassLoader defaultLoader = ReflectUtils.class.getClassLoader();
    private static Method DEFINE_CLASS, DEFINE_CLASS_UNSAFE;
    private static final ProtectionDomain PROTECTION_DOMAIN;
    private static final Object UNSAFE;
    private static final Throwable THROWABLE;

    private static final ArrayList<Method> OBJECT_METHODS = new ArrayList<>();

    static {

        ProtectionDomain protectionDomain;
        Method defineClass, defineClassUnsafe;
        Object unsafe;
        Throwable throwable = null;
        try {
            protectionDomain = getProtectionDomain(ReflectUtils.class);
            try {
                defineClass = doPrivileged((PrivilegedExceptionAction<Method>) () -> {
                    final Class loader = Class.forName("java.lang.ClassLoader"); // JVM crash w/o this
                    final Method ret = loader.getDeclaredMethod("defineClass",
                                                                String.class,
                                                                byte[].class,
                                                                Integer.TYPE,
                                                                Integer.TYPE,
                                                                ProtectionDomain.class);
                    ret.setAccessible(true);
                    return ret;
                });
                defineClassUnsafe = null;
                unsafe = null;
            }
            catch (Throwable t) {
                // Fallback on Jigsaw where this method is not available.
                throwable = t;
                defineClass = null;
                unsafe = doPrivileged((PrivilegedExceptionAction) () -> {
                    Class u = Class.forName("sun.misc.Unsafe");
                    Field theUnsafe = u.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    return theUnsafe.get(null);
                });

                Class u = Class.forName("sun.misc.Unsafe");
                defineClassUnsafe = u.getMethod("defineClass",
                                                String.class,
                                                byte[].class,
                                                Integer.TYPE,
                                                Integer.TYPE,
                                                ClassLoader.class,
                                                ProtectionDomain.class);
            }
            doPrivileged((PrivilegedExceptionAction) () -> {
                for (Method method : Object.class.getDeclaredMethods()) {
                    if ("finalize".equals(method.getName()) || (method.getModifiers() & (FINAL | STATIC)) > 0) {
                        continue;
                    }
                    OBJECT_METHODS.add(method);
                }
                return null;
            });
        }
        catch (Throwable t) {
            if (throwable == null) {
                throwable = t;
            }
            protectionDomain = null;
            defineClass = null;
            defineClassUnsafe = null;
            unsafe = null;
        }
        UNSAFE = unsafe;
        THROWABLE = throwable;
        DEFINE_CLASS = defineClass;
        PROTECTION_DOMAIN = protectionDomain;
        DEFINE_CLASS_UNSAFE = defineClassUnsafe;
    }

    private static final String[] CGLIB_PACKAGES = { "java.lang" };

    static {
        primitives.put("byte", Byte.TYPE);
        primitives.put("char", Character.TYPE);
        primitives.put("double", Double.TYPE);
        primitives.put("float", Float.TYPE);
        primitives.put("int", Integer.TYPE);
        primitives.put("long", Long.TYPE);
        primitives.put("short", Short.TYPE);
        primitives.put("boolean", Boolean.TYPE);

        transforms.put("byte", "B");
        transforms.put("char", "C");
        transforms.put("double", "D");
        transforms.put("float", "F");
        transforms.put("int", "I");
        transforms.put("long", "J");
        transforms.put("short", "S");
        transforms.put("boolean", "Z");
    }

    public static ProtectionDomain getProtectionDomain(final Class<?> source) {
        return source == null ? null : doPrivileged((PrivilegedAction<ProtectionDomain>) () -> source.getProtectionDomain());
    }

    public static Type[] getExceptionTypes(Member member) {
        if (member instanceof Executable) {
            return TypeUtils.getTypes(((Executable) member).getExceptionTypes());
        }
        throw new IllegalArgumentException("Cannot get exception types of a field");
    }

    public static Signature getSignature(Member member) {
        if (member instanceof Method) {
            return new Signature(member.getName(), Type.getMethodDescriptor((Method) member));
        }
        if (member instanceof Constructor) {
            Type[] types = TypeUtils.getTypes(((Constructor) member).getParameterTypes());
            return new Signature(Constant.CONSTRUCTOR_NAME, Type.getMethodDescriptor(Type.VOID_TYPE, types));
        }
        throw new IllegalArgumentException("Cannot get signature of a field");
    }

    public static Constructor findConstructor(String desc) {
        return findConstructor(desc, defaultLoader);
    }

    public static Constructor findConstructor(String desc, ClassLoader loader) {
        try {
            String className = desc.substring(0, desc.indexOf('(')).trim();
            return getClass(className, loader).getConstructor(parseTypes(desc, loader));
        }
        catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new CodeGenerationException(e);
        }
    }

    public static Method findMethod(String desc) {
        return findMethod(desc, defaultLoader);
    }

    public static Method findMethod(String desc, ClassLoader loader) {
        try {
            int lparen = desc.indexOf('(');
            int dot = desc.lastIndexOf('.', lparen);
            String className = desc.substring(0, dot).trim();
            String methodName = desc.substring(dot + 1, lparen).trim();
            return getClass(className, loader).getDeclaredMethod(methodName, parseTypes(desc, loader));
        }
        catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new CodeGenerationException(e);
        }
    }

    private static Class[] parseTypes(String desc, ClassLoader loader) throws ClassNotFoundException {
        int lparen = desc.indexOf('(');
        int rparen = desc.indexOf(')', lparen);
        ArrayList<String> params = new ArrayList<>();
        int start = lparen + 1;
        for (;;) {
            int comma = desc.indexOf(',', start);
            if (comma < 0) {
                break;
            }
            params.add(desc.substring(start, comma).trim());
            start = comma + 1;
        }
        if (start < rparen) {
            params.add(desc.substring(start, rparen).trim());
        }
        int i = 0;
        Class<?>[] types = new Class[params.size()];
        for (final String name : params) {
            types[i++] = getClass(name, loader);
        }

        return types;
    }

    private static Class getClass(String className, ClassLoader loader) throws ClassNotFoundException {
        return getClass(className, loader, CGLIB_PACKAGES);
    }

    private static Class getClass(String className, ClassLoader loader, String[] packages)
            throws ClassNotFoundException //
    {
        String save = className;
        int dimensions = 0;
        int index = 0;
        while ((index = className.indexOf("[]", index) + 1) > 0) {
            dimensions++;
        }
        StringBuilder brackets = new StringBuilder(className.length() - dimensions);
        for (int i = 0; i < dimensions; i++) {
            brackets.append('[');
        }
        className = className.substring(0, className.length() - 2 * dimensions);

        final String prefix = (dimensions > 0) ? brackets + "L" : Constant.BLANK;
        final String suffix = (dimensions > 0) ? ";" : Constant.BLANK;
        try {//@off
            return Class.forName(new StringBuilder(prefix)
                                         .append(className)
                                         .append(suffix)
                                         .toString(), false, loader);//@on
        }
        catch (ClassNotFoundException ignore) {}
        for (int i = 0; i < packages.length; i++) {
            try {
//@off
                return Class.forName(new StringBuilder(prefix)
                                     .append(packages[i])
                                     .append('.')
                                     .append(className)
                                     .append(suffix).toString(), false, loader);//@on
            }
            catch (ClassNotFoundException ignore) {}
        }
        if (dimensions == 0) {
            Class c = primitives.get(className);
            if (c != null) {
                return c;
            }
        }
        else {
            String transform = transforms.get(className);
            if (transform != null) {
                try {
                    return Class.forName(brackets.append(transform).toString(), false, loader);
                }
                catch (ClassNotFoundException ignore) {}
            }
        }
        throw new ClassNotFoundException(save);
    }

    public static <T> T newInstance(Class<T> type) {
        return newInstance(type, Constant.EMPTY_CLASS_ARRAY, null);
    }

    public static <T> T newInstance(Class<T> type, Class[] parameterTypes, Object[] args) {
        return newInstance(getConstructor(type, parameterTypes), args);
    }

    public static <T> T newInstance(final Constructor<T> cstruct, final Object[] args) {

        try {
            return cstruct.newInstance(args);
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw new CodeGenerationException(e);
        }
        catch (InvocationTargetException e) {
            throw new CodeGenerationException(e.getTargetException());
        }
    }

    public static <T> Constructor<T> getConstructor(Class<T> type, Class[] parameterTypes) {
        try {
            return ReflectionUtils.accessibleConstructor(type, parameterTypes);
        }
        catch (NoSuchMethodException e) {
            throw new CodeGenerationException(e);
        }
    }

    public static String[] getNames(final Class[] classes) {
        if (classes == null) {
            return null;
        }
        int i = 0;
        final String[] names = new String[classes.length];
        for (final Class clazz : classes) {
            names[i++] = clazz.getName();
        }
        return names;
    }

    public static Class[] getClasses(final Object[] objects) {
        int i = 0;
        final Class[] classes = new Class[objects.length];
        for (final Object obj : objects) {
            classes[i++] = obj.getClass();
        }
        return classes;
    }

    public static Method findNewInstance(Class<?> iface) {
        final Method m = findInterfaceMethod(iface);
        if (m.getName().equals("newInstance")) {
            return m;
        }
        throw new IllegalArgumentException(iface + " missing newInstance method");
    }

    public static Method[] getPropertyMethods(PropertyDescriptor[] properties, boolean read, boolean write) {
        final Set<Method> methods = new HashSet<>();
        for (int i = 0; i < properties.length; i++) {
            PropertyDescriptor pd = properties[i];
            if (read) {
                methods.add(pd.getReadMethod());
            }
            if (write) {
                methods.add(pd.getWriteMethod());
            }
        }
        methods.remove(null);
        return methods.toArray(new Method[methods.size()]);
    }

    public static PropertyDescriptor[] getBeanProperties(Class<?> type) {
        return getPropertiesHelper(type, true, true);
    }

    public static PropertyDescriptor[] getBeanGetters(Class<?> type) {
        return getPropertiesHelper(type, true, false);
    }

    public static PropertyDescriptor[] getBeanSetters(Class<?> type) {
        return getPropertiesHelper(type, false, true);
    }

    private static PropertyDescriptor[] getPropertiesHelper(Class<?> type, boolean read, boolean write) {
        try {
            PropertyDescriptor[] all = Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors();
            if (read && write) {
                return all;
            }
            final ArrayList<PropertyDescriptor> properties = new ArrayList<>(all.length);
            for (final PropertyDescriptor pd : all) {
                if ((read && pd.getReadMethod() != null) || (write && pd.getWriteMethod() != null)) {
                    properties.add(pd);
                }
            }
            return properties.toArray(new PropertyDescriptor[properties.size()]);
        }
        catch (IntrospectionException e) {
            throw new CodeGenerationException(e);
        }
    }

    public static Method findDeclaredMethod(final Class<?> type,
                                            final String methodName,
                                            final Class<?>[] parameterTypes) throws NoSuchMethodException {

        Class<?> cl = type;
        while (cl != null) {
            try {
                return cl.getDeclaredMethod(methodName, parameterTypes);
            }
            catch (NoSuchMethodException e) {
                cl = cl.getSuperclass();
            }
        }
        throw new NoSuchMethodException(methodName);

    }

    public static List<Method> addAllMethods(final Class<?> type, final List<Method> list) {

        if (type == Object.class) {
            list.addAll(OBJECT_METHODS);
        }
        else {
            Collections.addAll(list, type.getDeclaredMethods());
        }

        final Class<?> superclass = type.getSuperclass();
        if (superclass != null) {
            addAllMethods(superclass, list);
        }
        for (final Class<?> interface_ : type.getInterfaces()) {
            addAllMethods(interface_, list);
        }
        return list;
    }

    public static List<Class<?>> addAllInterfaces(Class<?> type, List<Class<?>> list) {
        final Class<?> superclass = type.getSuperclass();
        if (superclass != null) {
            Collections.addAll(list, type.getInterfaces());
            addAllInterfaces(superclass, list);
        }
        return list;
    }

    public static Method findInterfaceMethod(Class iface) {

        if (iface.isInterface()) {
            final Method[] methods = iface.getDeclaredMethods();
            if (methods.length != 1) {
                throw new IllegalArgumentException("expecting exactly 1 method in " + iface);
            }
            return methods[0];
        }
        throw new IllegalArgumentException(iface + " is not an interface");
    }

    public static <T> Class<T> defineClass(String className, byte[] b, //
                                           ClassLoader loader, ProtectionDomain protection) throws Exception //
    {
        final ProtectionDomain protectionDomainToUse = protection == null ? PROTECTION_DOMAIN : protection;

        Class<T> c;
        if (DEFINE_CLASS != null) {
            c = (Class<T>) DEFINE_CLASS.invoke(loader, className, b, 0, b.length, protectionDomainToUse);
        }
        else if (DEFINE_CLASS_UNSAFE != null) {
            c = (Class<T>) DEFINE_CLASS_UNSAFE.invoke(UNSAFE, className, b, 0, b.length, loader, protectionDomainToUse);
        }
        else {
            throw new CodeGenerationException(THROWABLE);
        }
        // Force static initializers to run.
        Class.forName(className, true, loader);
        return c;
    }

    public static int findPackageProtected(Class[] classes) {
        for (int i = 0; i < classes.length; i++) {
            if (!Modifier.isPublic(classes[i].getModifiers())) {
                return i;
            }
        }
        return 0;
    }

    public static MethodInfo getMethodInfo(final Member member, final int modifiers) {

        final Signature sig = getSignature(member);
        return new MethodInfo() {
            private ClassInfo ci;

            public ClassInfo getClassInfo() {
                if (ci == null) ci = ReflectUtils.getClassInfo(member.getDeclaringClass());
                return ci;
            }

            public int getModifiers() {
                return modifiers;
            }

            public Signature getSignature() {
                return sig;
            }

            public Type[] getExceptionTypes() {
                return ReflectUtils.getExceptionTypes(member);
            }

            public Attribute getAttribute() {
                return null;
            }
        };
    }

    public static MethodInfo getMethodInfo(Member member) {
        return getMethodInfo(member, member.getModifiers());
    }

    public static ClassInfo getClassInfo(final Class clazz) {
        final Type type = Type.getType(clazz);
        final Type sc = (clazz.getSuperclass() == null) ? null : Type.getType(clazz.getSuperclass());
        return new ClassInfo() {
            public Type getType() {
                return type;
            }

            public Type getSuperType() {
                return sc;
            }

            public Type[] getInterfaces() {
                return TypeUtils.getTypes(clazz.getInterfaces());
            }

            public int getModifiers() {
                return clazz.getModifiers();
            }
        };
    }

    // used by MethodInterceptorGenerated generated code
    public static Method[] findMethods(String[] namesAndDescriptors, Method[] methods) {

        final HashMap<String, Method> map = new HashMap<>();
        for (final Method method : methods) {
            map.put(method.getName().concat(Type.getMethodDescriptor(method)), method);
        }

        final Method[] result = new Method[namesAndDescriptors.length / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = map.get(namesAndDescriptors[i * 2] + namesAndDescriptors[i * 2 + 1]);
        }
        return result;
    }

}
