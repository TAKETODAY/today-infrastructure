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

import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Defines property resolution behavior on instances of
 * {@link java.util.ResourceBundle}.
 * 
 * <p>
 * This resolver handles base objects of type
 * <code>java.util.ResourceBundle</code>. It accepts any object as a property
 * and coerces it to a <code>java.lang.String</code> for invoking
 * {@link java.util.ResourceBundle#getObject(java.lang.String)}.
 * </p>
 * 
 * <p>
 * This resolver is read only and will throw a
 * {@link PropertyNotWritableException} if <code>setValue</code> is called.
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
 * @see java.util.ResourceBundle
 * @since JSP 2.1
 */
public class ResourceBundleExpressionResolver extends ExpressionResolver {

    /**
     * If the base object is an instance of <code>ResourceBundle</code>, the
     * provided property will first be coerced to a <code>String</code>. The
     * <code>Object</code> returned by <code>getObject</code> on the base
     * <code>ResourceBundle</code> will be returned.
     * </p>
     * If the base is <code>ResourceBundle</code>, the <code>propertyResolved</code>
     * property of the <code>ELContext</code> object must be set to
     * <code>true</code> by this resolver, before returning. If this property is not
     * <code>true</code> after this method is called, the caller should ignore the
     * return value.
     * </p>
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The ResourceBundle to analyze.
     * @param property
     *            The name of the property to analyze. Will be coerced to a
     *            <code>String</code>.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then
     *         <code>null</code> if property is <code>null</code>; otherwise the
     *         <code>Object</code> for the given key (property coerced to
     *         <code>String</code>) from the <code>ResourceBundle</code>. If no
     *         object for the given key can be found, then the <code>String</code>
     *         "???" + key + "???".
     * @throws NullPointerException
     *             if context is <code>null</code>
     * @throws ExpressionException
     *             if an exception was thrown while performing the property or
     *             variable resolution. The thrown exception must be included as the
     *             cause property of this exception, if available.
     */
    public Object getValue(ExpressionContext context, Object base, Object property) {

        if (base instanceof ResourceBundle) {
            Objects.requireNonNull(context).setPropertyResolved(true);
            if (property != null) {
                try {
                    return ((ResourceBundle) base).getObject(property.toString());
                }
                catch (MissingResourceException e) {
                    return "???" + property + "???";
                }
            }
        }
        return null;
    }

    /**
     * If the base object is an instance of <code>ResourceBundle</code>, return
     * <code>null</code>, since the resolver is read only.
     * 
     * <p>
     * If the base is <code>ResourceBundle</code>, the <code>propertyResolved</code>
     * property of the <code>ELContext</code> object must be set to
     * <code>true</code> by this resolver, before returning. If this property is not
     * <code>true</code> after this method is called, the caller should ignore the
     * return value.
     * </p>
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The ResourceBundle to analyze.
     * @param property
     *            The name of the property to analyze.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then
     *         <code>null</code>; otherwise undefined.
     * @throws NullPointerException
     *             if context is <code>null</code>
     */
    public Class<?> getType(ExpressionContext context, Object base, Object property) {

        if (base instanceof ResourceBundle) {
            Objects.requireNonNull(context).setPropertyResolved(true);
        }
        return null;
    }

    /**
     * If the base object is a ResourceBundle, throw a
     * {@link PropertyNotWritableException}.
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The ResourceBundle to be modified. Only bases that are of type
     *            ResourceBundle are handled.
     * @param property
     *            The String property to use.
     * @param value
     *            The value to be set.
     * @throws NullPointerException
     *             if context is <code>null</code>.
     * @throws PropertyNotWritableException
     *             Always thrown if base is an instance of ReasourceBundle.
     */
    public void setValue(ExpressionContext context, Object base, Object property, Object value) {

        if (base instanceof ResourceBundle) {
            Objects.requireNonNull(context).setPropertyResolved(true);
            throw new PropertyNotWritableException("ResourceBundles are immutable");
        }
    }

    /**
     * If the base object is not null and an instanceof {@link ResourceBundle},
     * return <code>true</code>.
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The ResourceBundle to be modified. Only bases that are of type
     *            ResourceBundle are handled.
     * @param property
     *            The String property to use.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then
     *         <code>true</code>; otherwise undefined.
     * @throws NullPointerException
     *             if context is <code>null</code>
     */
    public boolean isReadOnly(ExpressionContext context, Object base, Object property) {

        if (base instanceof ResourceBundle) {
            Objects.requireNonNull(context).setPropertyResolved(true);
            return true;
        }
        return false;
    }

}
