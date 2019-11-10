/*
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.el;

import static javax.el.ELUtil.invokeMethod;

import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines property resolution behavior on objects using the JavaBeans component
 * architecture.
 *
 * <p>
 * This resolver handles base objects of any type, as long as the base is not
 * <code>null</code>. It accepts any object as a property or method, and coerces
 * it to a string.
 *
 * <p>
 * For property resolution, the property string is used to find a JavaBeans
 * compliant property on the base object. The value is accessed using JavaBeans
 * getters and setters.
 * </p>
 *
 * <p>
 * For method resolution, the method string is the name of the method in the
 * bean. The parameter types can be optionally specified to identify the method.
 * If the parameter types are not specified, the parameter objects are used in
 * the method resolution.
 * </p>
 * 
 * <p>
 * This resolver can be constructed in read-only mode, which means that
 * {@link #isReadOnly} will always return <code>true</code> and
 * {@link #setValue} will always throw
 * <code>PropertyNotWritableException</code>.
 * </p>
 *
 * <p>
 * <code>ELResolver</code>s are combined together using
 * {@link CompositeELResolver}s, to define rich semantics for evaluating an
 * expression. See the javadocs for {@link ELResolver} for details.
 * </p>
 *
 * <p>
 * Because this resolver handles base objects of any type, it should be placed
 * near the end of a composite resolver. Otherwise, it will claim to have
 * resolved a property before any resolvers that come after it get a chance to
 * test if they can do so as well.
 * </p>
 *
 * @see CompositeELResolver
 * @see ELResolver
 * @since JSP 2.1
 */
public class BeanELResolver extends ELResolver {

    private final boolean isReadOnly;

    /**
     * Creates a new read/write <code>BeanELResolver</code>.
     */
    public BeanELResolver() {
        this(false);
    }

    /**
     * Creates a new <code>BeanELResolver</code> whose read-only status is
     * determined by the given parameter.
     *
     * @param isReadOnly
     *            <code>true</code> if this resolver cannot modify beans;
     *            <code>false</code> otherwise.
     */
    public BeanELResolver(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    /**
     * If the base object is not <code>null</code>, returns the most general
     * acceptable type that can be set on this bean property.
     *
     * <p>
     * If the base is not <code>null</code>, the <code>propertyResolved</code>
     * property of the <code>ELContext</code> object must be set to
     * <code>true</code> by this resolver, before returning. If this property is not
     * <code>true</code> after this method is called, the caller should ignore the
     * return value.
     * </p>
     *
     * <p>
     * The provided property will first be coerced to a <code>String</code>. If
     * there is a <code>BeanInfoProperty</code> for this property and there were no
     * errors retrieving it, the <code>propertyType</code> of the
     * <code>propertyDescriptor</code> is returned. Otherwise, a
     * <code>PropertyNotFoundException</code> is thrown.
     * </p>
     *
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The bean to analyze.
     * @param property
     *            The name of the property to analyze. Will be coerced to a
     *            <code>String</code>.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then the most
     *         general acceptable type; otherwise undefined.
     * @throws NullPointerException
     *             if context is <code>null</code>
     * @throws PropertyNotFoundException
     *             if <code>base</code> is not <code>null</code> and the specified
     *             property does not exist or is not readable.
     * @throws ELException
     *             if an exception was thrown while performing the property or
     *             variable resolution. The thrown exception must be included as the
     *             cause property of this exception, if available.
     */
    public Class<?> getType(ELContext context, Object base, Object property) {

        if (base == null || property == null) {
            return null;
        }
        final Field field = getBeanProperty(base, property);
        Objects.requireNonNull(context).setPropertyResolved(true);
        return field.getType();
    }

    /**
     * If the base object is not <code>null</code>, returns the current value of the
     * given property on this bean.
     *
     * <p>
     * If the base is not <code>null</code>, the <code>propertyResolved</code>
     * property of the <code>ELContext</code> object must be set to
     * <code>true</code> by this resolver, before returning. If this property is not
     * <code>true</code> after this method is called, the caller should ignore the
     * return value.
     * </p>
     *
     * <p>
     * The provided property name will first be coerced to a <code>String</code>. If
     * the property is a readable property of the base object, as per the JavaBeans
     * specification, then return the result of the getter call. If the getter
     * throws an exception, it is propagated to the caller. If the property is not
     * found or is not readable, a <code>PropertyNotFoundException</code> is thrown.
     * </p>
     *
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The bean on which to get the property.
     * @param property
     *            The name of the property to get. Will be coerced to a
     *            <code>String</code>.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then the value
     *         of the given property. Otherwise, undefined.
     * @throws NullPointerException
     *             if context is <code>null</code>.
     * @throws PropertyNotFoundException
     *             if <code>base</code> is not <code>null</code> and the specified
     *             property does not exist or is not readable.
     * @throws ELException
     *             if an exception was thrown while performing the property or
     *             variable resolution. The thrown exception must be included as the
     *             cause property of this exception, if available.
     */
    public Object getValue(ELContext context, Object base, Object property) {

        if (base == null || property == null) {
            return null;
        }

        try {
            final Field field = getBeanProperty(base, property);
            final Object value = field.get(base);
            Objects.requireNonNull(context).setPropertyResolved(base, property);
            return value;
        }
        catch (Exception ex) {
            if (ex instanceof ELException) {
                throw (ELException) ex;
            }
            throw new ELException(ex);
        }
    }

    /**
     * If the base object is not <code>null</code>, attempts to set the value of the
     * given property on this bean.
     *
     * <p>
     * If the base is not <code>null</code>, the <code>propertyResolved</code>
     * property of the <code>ELContext</code> object must be set to
     * <code>true</code> by this resolver, before returning. If this property is not
     * <code>true</code> after this method is called, the caller can safely assume
     * no value was set.
     * </p>
     *
     * <p>
     * If this resolver was constructed in read-only mode, this method will always
     * throw <code>PropertyNotWritableException</code>.
     * </p>
     *
     * <p>
     * The provided property name will first be coerced to a <code>String</code>. If
     * property is a writable property of <code>base</code> (as per the JavaBeans
     * Specification), the setter method is called (passing <code>value</code>). If
     * the property exists but does not have a setter, then a
     * <code>PropertyNotFoundException</code> is thrown. If the property does not
     * exist, a <code>PropertyNotFoundException</code> is thrown.
     * </p>
     *
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The bean on which to set the property.
     * @param property
     *            The name of the property to set. Will be coerced to a
     *            <code>String</code>.
     * @param val
     *            The value to be associated with the specified key.
     * @throws NullPointerException
     *             if context is <code>null</code>.
     * @throws PropertyNotFoundException
     *             if <code>base</code> is not <code>null</code> and the specified
     *             property does not exist.
     * @throws PropertyNotWritableException
     *             if this resolver was constructed in read-only mode, or if there
     *             is no setter for the property.
     * @throws ELException
     *             if an exception was thrown while performing the property or
     *             variable resolution. The thrown exception must be included as the
     *             cause property of this exception, if available.
     */
    public void setValue(ELContext context, Object base, Object property, Object val) {

        if (base == null || property == null) {
            return;
        }

        if (isReadOnly) {
            throw new PropertyNotWritableException("The ELResolver for the class '" + base.getClass().getName() + "' is not writable.");
        }

        try {

            final Field field = getBeanProperty(base, property);
            field.set(base, val);
            Objects.requireNonNull(context).setPropertyResolved(base, property);
        }
        catch (Exception ex) {
            if (ex instanceof ELException) {
                throw (ELException) ex;
            }
            final StringBuilder message = new StringBuilder("Can't set property '")//
                    .append(property.toString())//
                    .append("' on class '")//
                    .append(base.getClass().getName())//
                    .append("' to value '")//
                    .append(val)//
                    .append("'.");

            throw new ELException(message.toString(), ex);
        }
    }

    /**
     * If the base object is not <code>null</code>, invoke the method, with the
     * given parameters on this bean. The return value from the method is returned.
     *
     * <p>
     * If the base is not <code>null</code>, the <code>propertyResolved</code>
     * property of the <code>ELContext</code> object must be set to
     * <code>true</code> by this resolver, before returning. If this property is not
     * <code>true</code> after this method is called, the caller should ignore the
     * return value.
     * </p>
     *
     * <p>
     * The provided method object will first be coerced to a <code>String</code>.
     * The methods in the bean is then examined and an attempt will be made to
     * select one for invocation. If no suitable can be found, a
     * <code>MethodNotFoundException</code> is thrown.
     *
     * If the given paramTypes is not <code>null</code>, select the method with the
     * given name and parameter types.
     *
     * Else select the method with the given name that has the same number of
     * parameters. If there are more than one such method, the method selection
     * process is undefined.
     *
     * Else select the method with the given name that takes a variable number of
     * arguments.
     *
     * Note the resolution for overloaded methods will likely be clarified in a
     * future version of the spec.
     *
     * The provide parameters are coerced to the corresponding parameter types of
     * the method, and the method is then invoked.
     *
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The bean on which to invoke the method
     * @param method
     *            The simple name of the method to invoke. Will be coerced to a
     *            <code>String</code>. If method is "&lt;init&gt;"or
     *            "&lt;clinit&gt;" a MethodNotFoundException is thrown.
     * @param paramTypes
     *            An array of Class objects identifying the method's formal
     *            parameter types, in declared order. Use an empty array if the
     *            method has no parameters. Can be <code>null</code>, in which case
     *            the method's formal parameter types are assumed to be unknown.
     * @param params
     *            The parameters to pass to the method, or <code>null</code> if no
     *            parameters.
     * @return The result of the method invocation (<code>null</code> if the method
     *         has a <code>void</code> return type).
     * @throws MethodNotFoundException
     *             if no suitable method can be found.
     * @throws ELException
     *             if an exception was thrown while performing (base, method)
     *             resolution. The thrown exception must be included as the cause
     *             property of this exception, if available. If the exception thrown
     *             is an <code>InvocationTargetException</code>, extract its
     *             <code>cause</code> and pass it to the <code>ELException</code>
     *             constructor.
     * @since EL 2.2
     */
    public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {

        if (base == null || method == null) {
            return null;
        }

        final Object ret = invokeMethod(Objects.requireNonNull(context),
                                        ELUtil.findMethod(base.getClass(), method.toString(), paramTypes, params, false),
                                        base,
                                        params);

        context.setPropertyResolved(base, method);
        return ret;
    }

    /**
     * If the base object is not <code>null</code>, returns whether a call to
     * {@link #setValue} will always fail.
     *
     * <p>
     * If the base is not <code>null</code>, the <code>propertyResolved</code>
     * property of the <code>ELContext</code> object must be set to
     * <code>true</code> by this resolver, before returning. If this property is not
     * <code>true</code> after this method is called, the caller can safely assume
     * no value was set.
     * </p>
     *
     * <p>
     * If this resolver was constructed in read-only mode, this method will always
     * return <code>true</code>.
     * </p>
     *
     * <p>
     * The provided property name will first be coerced to a <code>String</code>. If
     * property is a writable property of <code>base</code>, <code>false</code> is
     * returned. If the property is found but is not writable, <code>true</code> is
     * returned. If the property is not found, a
     * <code>PropertyNotFoundException</code> is thrown.
     * </p>
     *
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The bean to analyze.
     * @param property
     *            The name of the property to analyzed. Will be coerced to a
     *            <code>String</code>.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then
     *         <code>true</code> if calling the <code>setValue</code> method will
     *         always fail or <code>false</code> if it is possible that such a call
     *         may succeed; otherwise undefined.
     * @throws NullPointerException
     *             if context is <code>null</code>
     * @throws PropertyNotFoundException
     *             if <code>base</code> is not <code>null</code> and the specified
     *             property does not exist.
     * @throws ELException
     *             if an exception was thrown while performing the property or
     *             variable resolution. The thrown exception must be included as the
     *             cause property of this exception, if available.
     */
    public boolean isReadOnly(ELContext context, Object base, Object property) {

        if (base == null || property == null) {
            return false;
        }

        Objects.requireNonNull(context).setPropertyResolved(true);
        return isReadOnly;
    }

    /**
     * If the base object is not <code>null</code>, returns an <code>Iterator</code>
     * containing the set of JavaBeans properties available on the given object.
     * Otherwise, returns <code>null</code>.
     *
     * <p>
     * The <code>Iterator</code> returned must contain zero or more instances of
     * {@link java.beans.FeatureDescriptor}. Each info object contains information
     * about a property in the bean, as obtained by calling the
     * <code>BeanInfo.getPropertyDescriptors</code> method. The
     * <code>FeatureDescriptor</code> is initialized using the same fields as are
     * present in the <code>PropertyDescriptor</code>, with the additional required
     * named attributes "<code>type</code>" and
     * "<code>resolvableAtDesignTime</code>" set as follows:
     * <dl>
     * <li>{@link ELResolver#TYPE} - The runtime type of the property, from
     * <code>PropertyDescriptor.getPropertyType()</code>.</li>
     * <li>{@link ELResolver#RESOLVABLE_AT_DESIGN_TIME} - <code>true</code>.</li>
     * </dl>
     * </p>
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The bean to analyze.
     * @return An <code>Iterator</code> containing zero or more
     *         <code>FeatureDescriptor</code> objects, each representing a property
     *         on this bean, or <code>null</code> if the <code>base</code> object is
     *         <code>null</code>.
     */
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {

        if (base == null) {
            return null;
        }

        try {
            final BeanInfo info = Introspector.getBeanInfo(base.getClass());

            ArrayList<FeatureDescriptor> list = new ArrayList<FeatureDescriptor>(info.getPropertyDescriptors().length);
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                pd.setValue("type", pd.getPropertyType());
                pd.setValue("resolvableAtDesignTime", Boolean.TRUE);
                list.add(pd);
            }
            return list.iterator();
        }
        catch (Exception ex) {
            return null;
        }
    }

    /**
     * If the base object is not <code>null</code>, returns the most general type
     * that this resolver accepts for the <code>property</code> argument. Otherwise,
     * returns <code>null</code>.
     *
     * <p>
     * Assuming the base is not <code>null</code>, this method will always return
     * <code>Object.class</code>. This is because any object is accepted as a key
     * and is coerced into a string.
     * </p>
     *
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The bean to analyze.
     * @return <code>null</code> if base is <code>null</code>; otherwise
     *         <code>Object.class</code>.
     */
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        if (base == null) {
            return null;
        }
        return Object.class;
    }

    private Field getBeanProperty(Object base, Object prop) throws PropertyNotFoundException {

        final Class<?> baseClass = base.getClass();
        BeanProperties bps = cache.get(baseClass);

        if (bps == null) {
            bps = new BeanProperties(baseClass);
            cache.put(baseClass, bps);
        }

        final Field field = bps.getBeanProperty(prop.toString());
        if (field == null) {
            throw new PropertyNotFoundException("The class '" + baseClass.getName() //
                    + "' does not have the property '" + prop + "'.");
        }
        return field;
    }

    /**
     * Defines the properties for a bean.
     */
    private final static class BeanProperties {

        private final Map<String, Field> propertyMap = new HashMap<>();

        public BeanProperties(Class<?> baseClass) {

            do {
                for (final Field field : baseClass.getDeclaredFields()) {
                    if (!Modifier.isPublic(field.getModifiers())) {
                        field.setAccessible(true);
                    }
                    propertyMap.put(field.getName(), field); // parent class will replace same field
                }
            } while ((baseClass = baseClass.getSuperclass()) != Object.class && baseClass != null);
        }

        public final Field getBeanProperty(final String property) {
            return propertyMap.get(property);
        }
    }

    private static final ConcurrentCache<Class<?>, BeanProperties> cache;
    private static final String CACHE_SIZE_PROP = "javax.el.BeanELResolver.cache.size";

    static {

        String cacheSizeStr;
        if (System.getSecurityManager() == null) {
            cacheSizeStr = System.getProperty(CACHE_SIZE_PROP, "1024");
        }
        else {
            cacheSizeStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(CACHE_SIZE_PROP, "1024");
                }
            });
        }
        cache = new ConcurrentCache<>(Integer.parseInt(cacheSizeStr));
    }

    private static final class ConcurrentCache<K, V> {

        private final int size;
        private final Map<K, V> eden;
        private final Map<K, V> longterm;

        public ConcurrentCache(int size) {
            this.size = size;
            this.eden = new ConcurrentHashMap<>(size);
            this.longterm = new WeakHashMap<>(size);
        }

        public V get(K key) {
            V value = this.eden.get(key);
            if (value == null) {
                synchronized (longterm) {
                    value = this.longterm.get(key);
                }
                if (value != null) {
                    this.eden.put(key, value);
                }
            }
            return value;
        }

        public void put(K key, V value) {
            if (this.eden.size() >= this.size) {
                synchronized (longterm) {
                    this.longterm.putAll(this.eden);
                }
                this.eden.clear();
            }
            this.eden.put(key, value);
        }
    }

}
