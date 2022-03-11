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

import java.lang.reflect.Method;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.expression.util.ReflectionUtil.findMethod;
import static cn.taketoday.expression.util.ReflectionUtil.invokeMethod;

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
 * {@link ExpressionResolverComposite}s, to define rich semantics for evaluating
 * an expression. See the javadocs for {@link ExpressionResolver} for details.
 * </p>
 *
 * <p>
 * Because this resolver handles base objects of any type, it should be placed
 * near the end of a composite resolver. Otherwise, it will claim to have
 * resolved a property before any resolvers that come after it get a chance to
 * test if they can do so as well.
 * </p>
 *
 * @see ExpressionResolverComposite
 * @see ExpressionResolver
 * @since JSP 2.1
 */
public class BeanPropertyExpressionResolver extends ExpressionResolver {

  private final boolean isReadOnly;

  /**
   * @since 4.0
   */
  private final boolean ignoreUnknownProperty;

  /**
   * Creates a new read/write <code>BeanPropertyExpressionResolver</code>.
   */
  public BeanPropertyExpressionResolver() {
    this.isReadOnly = false;
    this.ignoreUnknownProperty = false;
  }

  public BeanPropertyExpressionResolver(boolean ignoreUnknownProperty) {
    this.isReadOnly = false;
    this.ignoreUnknownProperty = ignoreUnknownProperty;
  }

  /**
   * Creates a new <code>BeanPropertyExpressionResolver</code> whose read-only status is
   * determined by the given parameter.
   *
   * @param isReadOnly <code>true</code> if this resolver cannot modify beans;
   * <code>false</code> otherwise.
   */
  public BeanPropertyExpressionResolver(boolean isReadOnly, boolean ignoreUnknownProperty) {
    this.isReadOnly = isReadOnly;
    this.ignoreUnknownProperty = ignoreUnknownProperty;
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
   * @param context The context of this evaluation.
   * @param base The bean to analyze.
   * @param property The name of the property to analyze. Will be coerced to a
   * <code>String</code>.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the most
   * general acceptable type; otherwise undefined.
   * @throws NullPointerException if context is <code>null</code>
   * @throws PropertyNotFoundException if <code>base</code> is not <code>null</code> and the specified
   * property does not exist or is not readable.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public Class<?> getType(ExpressionContext context, Object base, Object property) {
    if (base == null || property == null) {
      return null;
    }

    BeanProperty beanProperty = getProperty(base, property);
    if (beanProperty != null) {
      context.setPropertyResolved(true);
      return beanProperty.getType();
    }
    return null;
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
   * @param context The context of this evaluation.
   * @param base The bean on which to get the property.
   * @param property The name of the property to get. Will be coerced to a
   * <code>String</code>.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the value
   * of the given property. Otherwise, undefined.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws PropertyNotFoundException if <code>base</code> is not <code>null</code> and the specified
   * property does not exist or is not readable.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public Object getValue(ExpressionContext context, Object base, Object property) {
    if (base == null || property == null) {
      return null;
    }
    BeanProperty beanProperty = getProperty(base, property);
    if (beanProperty != null) {
      try {
        Object value = beanProperty.getValue(base);
        context.setPropertyResolved(base, property);
        return value;
      }
      catch (Exception ex) {
        throw new ExpressionException(
                "Can't get property: '" + property + "' from '" + base.getClass() + "'", ex);
      }
    }
    return null;
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
   * @param context The context of this evaluation.
   * @param base The bean on which to set the property.
   * @param property The name of the property to set. Will be coerced to a
   * <code>String</code>.
   * @param val The value to be associated with the specified key.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws PropertyNotFoundException if <code>base</code> is not <code>null</code> and the specified
   * property does not exist.
   * @throws PropertyNotWritableException if this resolver was constructed in read-only mode, or if there
   * is no setter for the property.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public void setValue(ExpressionContext context, Object base, Object property, Object val) {
    if (base == null || property == null) {
      return;
    }
    if (isReadOnly) {
      throw new PropertyNotWritableException(
              "The ExpressionResolver for the class '" + base.getClass().getName() + "' is not writable.");
    }

    BeanProperty beanProperty = getProperty(base, property);
    if (beanProperty != null) {
      try {
        beanProperty.setValue(base, val);
        context.setPropertyResolved(base, property);
      }
      catch (Exception ex) {
        StringBuilder message = new StringBuilder("Can't set property '")//
                .append(property)//
                .append("' on class '")//
                .append(base.getClass().getName())//
                .append("' to value '")//
                .append(val)//
                .append("'.");
        throw new ExpressionException(message.toString(), ex);
      }
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
   * @param context The context of this evaluation.
   * @param base The bean on which to invoke the method
   * @param method The simple name of the method to invoke. Will be coerced to a
   * <code>String</code>. If method is "&lt;init&gt;"or
   * "&lt;clinit&gt;" a MethodNotFoundException is thrown.
   * @param paramTypes An array of Class objects identifying the method's formal
   * parameter types, in declared order. Use an empty array if the
   * method has no parameters. Can be <code>null</code>, in which case
   * the method's formal parameter types are assumed to be unknown.
   * @param params The parameters to pass to the method, or <code>null</code> if no
   * parameters.
   * @return The result of the method invocation (<code>null</code> if the method
   * has a <code>void</code> return type).
   * @throws MethodNotFoundException if no suitable method can be found.
   * @throws ExpressionException if an exception was thrown while performing (base, method)
   * resolution. The thrown exception must be included as the cause
   * property of this exception, if available. If the exception thrown
   * is an <code>InvocationTargetException</code>, extract its
   * <code>cause</code> and pass it to the <code>ELException</code>
   * constructor.
   * @since EL 2.2
   */
  @Override
  public Object invoke(ExpressionContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
    if (base == null || method == null) {
      return null;
    }
    Method methodToUse = findMethod(base.getClass(), method.toString(), paramTypes, params, false);
    Object ret = invokeMethod(context, methodToUse, base, params);
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
   * @param context The context of this evaluation.
   * @param base The bean to analyze.
   * @param property The name of the property to analyzed. Will be coerced to a
   * <code>String</code>.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then
   * <code>true</code> if calling the <code>setValue</code> method will
   * always fail or <code>false</code> if it is possible that such a call
   * may succeed; otherwise undefined.
   * @throws NullPointerException if context is <code>null</code>
   * @throws PropertyNotFoundException if <code>base</code> is not <code>null</code> and the specified
   * property does not exist.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {
    if (base == null || property == null) {
      return false;
    }
    context.setPropertyResolved(true);
    return isReadOnly;
  }

  @Nullable
  private BeanProperty getProperty(Object base, Object prop) throws PropertyNotFoundException {
    BeanProperty beanProperty = BeanMetadata.from(base).getBeanProperty(prop.toString());
    if (beanProperty == null && !ignoreUnknownProperty) {
      throw new PropertyNotFoundException(
              "The class '" + base.getClass().getName() + "' does not have the property '" + prop + "'.");
    }
    return beanProperty;
  }

  /**
   * @since 4.0
   */
  public boolean isIgnoreUnknownProperty() {
    return ignoreUnknownProperty;
  }

  /**
   * @since 4.0
   */
  public boolean isReadOnly() {
    return isReadOnly;
  }
}
