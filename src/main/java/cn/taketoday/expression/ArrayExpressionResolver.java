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

package cn.taketoday.expression;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 * Defines property resolution behavior on arrays.
 *
 * <p>
 * This resolver handles base objects that are Java language arrays. It accepts
 * any object as a property and coerces that object into an integer index into
 * the array. The resulting value is the value in the array at that index.
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
 * {@link CompositeExpressionResolver}s, to define rich semantics for evaluating an
 * expression. See the javadocs for {@link ExpressionResolver} for details.
 * </p>
 *
 * @see CompositeExpressionResolver
 * @see ExpressionResolver
 * @since JSP 2.1
 */
public class ArrayExpressionResolver extends ExpressionResolver {

    /**
     * Creates a new read/write <code>ArrayELResolver</code>.
     */
    public ArrayExpressionResolver() {
        this.isReadOnly = false;
    }

    /**
     * Creates a new <code>ArrayELResolver</code> whose read-only status is
     * determined by the given parameter.
     *
     * @param isReadOnly
     *            <code>true</code> if this resolver cannot modify arrays;
     *            <code>false</code> otherwise.
     */
    public ArrayExpressionResolver(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    /**
     * If the base object is an array, returns the most general acceptable type for
     * a value in this array.
     *
     * <p>
     * If the base is a <code>array</code>, the <code>propertyResolved</code>
     * property of the <code>ELContext</code> object must be set to
     * <code>true</code> by this resolver, before returning. If this property is not
     * <code>true</code> after this method is called, the caller should ignore the
     * return value.
     * </p>
     *
     * <p>
     * Assuming the base is an <code>array</code>, this method will always return
     * <code>base.getClass().getComponentType()</code>, which is the most general
     * type of component that can be stored at any given index in the array.
     * </p>
     *
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The array to analyze. Only bases that are Java language arrays are
     *            handled by this resolver.
     * @param property
     *            The index of the element in the array to return the acceptable
     *            type for. Will be coerced into an integer, but otherwise ignored
     *            by this resolver.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then the most
     *         general acceptable type; otherwise undefined.
     * @throws PropertyNotFoundException
     *             if the given index is out of bounds for this array.
     * @throws NullPointerException
     *             if context is <code>null</code>
     * @throws ExpressionException
     *             if an exception was thrown while performing the property or
     *             variable resolution. The thrown exception must be included as the
     *             cause property of this exception, if available.
     */
    public Class<?> getType(ExpressionContext context, Object base, Object property) {

        if (base != null) {
            final Class<? extends Object> beanClass = base.getClass();
            if (beanClass.isArray()) {

                Objects.requireNonNull(context).setPropertyResolved(true);
                final int index = toInteger(property);
                if (index < 0 || index >= Array.getLength(base)) {
                    throw new PropertyNotFoundException();
                }
                return beanClass.getComponentType();
            }
        }
        return null;
    }

    /**
     * If the base object is a Java language array, returns the value at the given
     * index. The index is specified by the <code>property</code> argument, and
     * coerced into an integer. If the coercion could not be performed, an
     * <code>IllegalArgumentException</code> is thrown. If the index is out of
     * bounds, <code>null</code> is returned.
     *
     * <p>
     * If the base is a Java language array, the <code>propertyResolved</code>
     * property of the <code>ELContext</code> object must be set to
     * <code>true</code> by this resolver, before returning. If this property is not
     * <code>true</code> after this method is called, the caller should ignore the
     * return value.
     * </p>
     *
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The array to analyze. Only bases that are Java language arrays are
     *            handled by this resolver.
     * @param property
     *            The index of the value to be returned. Will be coerced into an
     *            integer.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then the value
     *         at the given index or <code>null</code> if the index was out of
     *         bounds. Otherwise, undefined.
     * @throws IllegalArgumentException
     *             if the property could not be coerced into an integer.
     * @throws NullPointerException
     *             if context is <code>null</code>.
     * @throws ExpressionException
     *             if an exception was thrown while performing the property or
     *             variable resolution. The thrown exception must be included as the
     *             cause property of this exception, if available.
     */
    public Object getValue(ExpressionContext context, Object base, Object property) {

        Objects.requireNonNull(context);

        if (base != null && base.getClass().isArray()) {
            Objects.requireNonNull(context).setPropertyResolved(base, property);

            final int index = toInteger(property);
            if (index >= 0 && index < Array.getLength(base)) {
                return Array.get(base, index);
            }
        }
        return null;
    }

    /**
     * If the base object is a Java language array, attempts to set the value at the
     * given index with the given value. The index is specified by the
     * <code>property</code> argument, and coerced into an integer. If the coercion
     * could not be performed, an <code>IllegalArgumentException</code> is thrown.
     * If the index is out of bounds, a <code>PropertyNotFoundException</code> is
     * thrown.
     *
     * <p>
     * If the base is a Java language array, the <code>propertyResolved</code>
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
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The array to be modified. Only bases that are Java language arrays
     *            are handled by this resolver.
     * @param property
     *            The index of the value to be set. Will be coerced into an integer.
     * @param val
     *            The value to be set at the given index.
     * @throws ClassCastException
     *             if the class of the specified element prevents it from being
     *             added to this array.
     * @throws NullPointerException
     *             if context is <code>null</code>.
     * @throws IllegalArgumentException
     *             if the property could not be coerced into an integer, or if some
     *             aspect of the specified element prevents it from being added to
     *             this array.
     * @throws PropertyNotWritableException
     *             if this resolver was constructed in read-only mode.
     * @throws PropertyNotFoundException
     *             if the given index is out of bounds for this array.
     * @throws ExpressionException
     *             if an exception was thrown while performing the property or
     *             variable resolution. The thrown exception must be included as the
     *             cause property of this exception, if available.
     */
    public void setValue(ExpressionContext context, Object base, Object property, Object val) {

        if (base != null) {
            final Class<? extends Object> beanClass = base.getClass();
            if (beanClass.isArray()) {
                Objects.requireNonNull(context).setPropertyResolved(base, property);
                if (isReadOnly) {
                    throw new PropertyNotWritableException();
                }
                // .isAssignableFrom(val.getClass())
                if (val != null && !beanClass.getComponentType().isInstance(val)) {
                    throw new ClassCastException();
                }
                final int index = toInteger(property);
                if (index < 0 || index >= Array.getLength(base)) {
                    throw new PropertyNotFoundException();
                }
                Array.set(base, index, val);
            }
        }
    }

    /**
     * If the base object is a Java language array, returns whether a call to
     * {@link #setValue} will always fail.
     *
     * <p>
     * If the base is a Java language array, the <code>propertyResolved</code>
     * property of the <code>ELContext</code> object must be set to
     * <code>true</code> by this resolver, before returning. If this property is not
     * <code>true</code> after this method is called, the caller should ignore the
     * return value.
     * </p>
     *
     * <p>
     * If this resolver was constructed in read-only mode, this method will always
     * return <code>true</code>. Otherwise, it returns <code>false</code>.
     * </p>
     *
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The array to analyze. Only bases that are a Java language array
     *            are handled by this resolver.
     * @param property
     *            The index of the element in the array to return the acceptable
     *            type for. Will be coerced into an integer, but otherwise ignored
     *            by this resolver.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then
     *         <code>true</code> if calling the <code>setValue</code> method will
     *         always fail or <code>false</code> if it is possible that such a call
     *         may succeed; otherwise undefined.
     * @throws PropertyNotFoundException
     *             if the given index is out of bounds for this array.
     * @throws NullPointerException
     *             if context is <code>null</code>
     * @throws ExpressionException
     *             if an exception was thrown while performing the property or
     *             variable resolution. The thrown exception must be included as the
     *             cause property of this exception, if available.
     */
    public boolean isReadOnly(ExpressionContext context, Object base, Object property) {

        if (base != null && base.getClass().isArray()) {
            Objects.requireNonNull(context).setPropertyResolved(true);
            int index = toInteger(property);
            if (index < 0 || index >= Array.getLength(base)) {
                throw new PropertyNotFoundException();
            }
        }
        return isReadOnly;
    }

    private int toInteger(Object p) {

        if (p instanceof Integer) {
            return ((Integer) p).intValue();
        }
        if (p instanceof Character) {
            return ((Character) p).charValue();
        }
        if (p instanceof Boolean) {
            return ((Boolean) p).booleanValue() ? 1 : 0;
        }
        if (p instanceof Number) {
            return ((Number) p).intValue();
        }
        if (p instanceof String) {
            return Integer.parseInt((String) p);
        }
        throw new IllegalArgumentException();
    }

    private boolean isReadOnly;
}
