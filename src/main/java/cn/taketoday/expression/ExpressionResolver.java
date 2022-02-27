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

import cn.taketoday.lang.Nullable;

/**
 * Enables customization of variable, property, method call, and type conversion
 * resolution behavior for EL expression evaluation.
 *
 * <p>
 * While evaluating an expression, the <code>ELResolver</code> associated with
 * the {@link ExpressionContext} is consulted to do the initial resolution of the first
 * variable of an expression. It is also consulted when a <code>.</code> or
 * <code>[]</code> operator is encountered.
 *
 * <p>
 * For example, in the EL expression <code>${employee.lastName}</code>, the
 * <code>ELResolver</code> determines what object <code>employee</code> refers
 * to, and what it means to get the <code>lastName</code> property on that
 * object.
 * </p>
 *
 * <p>
 * Most methods in this class accept a <code>base</code> and
 * <code>property</code> parameter. In the case of variable resolution (e.g.
 * determining what <code>employee</code> refers to in
 * <code>${employee.lastName}</code>), the <code>base</code> parameter will be
 * <code>null</code> and the <code>property</code> parameter will always be of
 * type <code>String</code>. In this case, if the <code>property</code> is not a
 * <code>String</code>, the behavior of the <code>ELResolver</code> is
 * undefined.
 * </p>
 *
 * <p>
 * In the case of property resolution, the <code>base</code> parameter
 * identifies the base object and the <code>property</code> object identifies
 * the property on that base. For example, in the expression
 * <code>${employee.lastName}</code>, <code>base</code> is the result of the
 * variable resolution for <code>employee</code> and <code>property</code> is
 * the string <code>"lastName"</code>. In the expression <code>${y[x]}</code>,
 * <code>base</code> is the result of the variable resolution for <code>y</code>
 * and <code>property</code> is the result of the variable resolution for
 * <code>x</code>.
 * </p>
 *
 * <p>
 * In the case of method call resolution, the <code>base</code> parameter
 * identifies the base object and the <code>method</code> parameter identifies a
 * method on that base. In the case of overloaded methods, the <code>
 * paramTypes</code> parameter can be optionally used to identify a method. The
 * <code>params</code>parameter are the parameters for the method call, and can
 * also be used for resolving overloaded methods when the
 * <code>paramTypes</code> parameter is not specified.
 *
 * <p>
 * In the case of type conversion resolution, the <code>obj</code> parameter
 * identifies the source object and the <code>targetType</code> parameter
 * identifies the target type the source to covert to.
 *
 * <p>
 * Though only a single <code>ELResolver</code> is associated with an
 * <code>ELContext</code>, there are usually multiple resolvers considered for
 * any given variable or property resolution. <code>ELResolver</code>s are
 * combined together using {@link ExpressionResolverComposite}s, to define rich
 * semantics for evaluating an expression.
 * </p>
 *
 * <p>
 * For the {@link #getValue}, {@link #getType}, {@link #setValue}, and
 * {@link #isReadOnly} methods, an <code>ELResolver</code> is not responsible
 * for resolving all possible (base, property) pairs. In fact, most resolvers
 * will only handle a <code>base</code> of a single type. To indicate that a
 * resolver has successfully resolved a particular (base, property) pair, it
 * must set the <code>propertyResolved</code> property of the
 * <code>ELContext</code> to <code>true</code>. If it could not handle the given
 * pair, it must leave this property alone. The caller must ignore the return
 * value of the method if <code>propertyResolved</code> is <code>false</code>.
 * </p>
 *
 * <p>
 * Similarly, for the {@link #convertToType} method an <code>ELResolver</code>
 * must set the <code>propertyResolved</code> to <code>true</code> to indicate
 * that it handles the conversion of the object to the target type.
 * </p>
 *
 * @see ExpressionResolverComposite
 * @see ExpressionContext#getResolver
 * @since JSP 2.1
 */
public abstract class ExpressionResolver {

  // --------------------------------------------------------- Constants

  /**
   * Attempts to resolve the given <code>property</code> object on the given
   * <code>base</code> object.
   *
   * <p>
   * If this resolver handles the given (base, property) pair, the
   * <code>propertyResolved</code> property of the <code>ELContext</code> object
   * must be set to <code>true</code> by the resolver, before returning. If this
   * property is not <code>true</code> after this method is called, the caller
   * should ignore the return value.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The base object whose property value is to be returned, or
   * <code>null</code> to resolve a top-level variable.
   * @param property The property or variable to be resolved.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the result
   * of the variable or property resolution; otherwise undefined.
   * @throws NullPointerException if context is <code>null</code>
   * @throws PropertyNotFoundException if the given (base, property) pair is handled by this
   * <code>ELResolver</code> but the specified variable or property
   * does not exist or is not readable.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  public abstract Object getValue(
          ExpressionContext context, @Nullable Object base, Object property);

  /**
   * Attempts to resolve and invoke the given <code>method</code> on the given
   * <code>base</code> object.
   *
   * <p>
   * If this resolver handles the given (base, method) pair, the
   * <code>propertyResolved</code> property of the <code>ELContext</code> object
   * must be set to <code>true</code> by the resolver, before returning. If this
   * property is not <code>true</code> after this method is called, the caller
   * should ignore the return value.
   * </p>
   *
   * <p>
   * A default implementation is provided that returns null so that existing
   * classes that extend ELResolver can continue to function.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The bean on which to invoke the method
   * @param method The simple name of the method to invoke. Will be coerced to a
   * <code>String</code>.
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
  public Object invoke(ExpressionContext context,
          Object base, Object method, Class<?>[] paramTypes, Object[] params) {
    return null;
  }

  /**
   * For a given <code>base</code> and <code>property</code>, attempts to identify
   * the most general type that is acceptable for an object to be passed as the
   * <code>value</code> parameter in a future call to the {@link #setValue}
   * method.
   *
   * <p>
   * If this resolver handles the given (base, property) pair, the
   * <code>propertyResolved</code> property of the <code>ELContext</code> object
   * must be set to <code>true</code> by the resolver, before returning. If this
   * property is not <code>true</code> after this method is called, the caller
   * should ignore the return value.
   * </p>
   *
   * <p>
   * This is not always the same as <code>getValue().getClass()</code>. For
   * example, in the case of an {@link ArrayExpressionResolver}, the <code>getType</code>
   * method will return the element type of the array, which might be a superclass
   * of the type of the actual element that is currently in the specified array
   * element.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The base object whose property value is to be analyzed, or
   * <code>null</code> to analyze a top-level variable.
   * @param property The property or variable to return the acceptable type for.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the most
   * general acceptable type; otherwise undefined.
   * @throws PropertyNotFoundException if the given (base, property) pair is handled by this
   * <code>ELResolver</code> but the specified variable or property
   * does not exist or is not readable.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  public abstract Class<?> getType(ExpressionContext context, Object base, Object property);

  /**
   * Attempts to set the value of the given <code>property</code> object on the
   * given <code>base</code> object.
   *
   * <p>
   * If this resolver handles the given (base, property) pair, the
   * <code>propertyResolved</code> property of the <code>ELContext</code> object
   * must be set to <code>true</code> by the resolver, before returning. If this
   * property is not <code>true</code> after this method is called, the caller can
   * safely assume no value has been set.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The base object whose property value is to be set, or
   * <code>null</code> to set a top-level variable.
   * @param property The property or variable to be set.
   * @param value The value to set the property or variable to.
   * @throws NullPointerException if context is <code>null</code>
   * @throws PropertyNotFoundException if the given (base, property) pair is handled by this
   * <code>ELResolver</code> but the specified variable or property
   * does not exist.
   * @throws PropertyNotWritableException if the given (base, property) pair is handled by this
   * <code>ELResolver</code> but the specified variable or property is
   * not writable.
   * @throws ExpressionException if an exception was thrown while attempting to set the property
   * or variable. The thrown exception must be included as the cause
   * property of this exception, if available.
   */
  public abstract void setValue(ExpressionContext context, Object base, Object property, Object value);

  /**
   * For a given <code>base</code> and <code>property</code>, attempts to
   * determine whether a call to {@link #setValue} will always fail.
   *
   * <p>
   * If this resolver handles the given (base, property) pair, the
   * <code>propertyResolved</code> property of the <code>ELContext</code> object
   * must be set to <code>true</code> by the resolver, before returning. If this
   * property is not <code>true</code> after this method is called, the caller
   * should ignore the return value.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The base object whose property value is to be analyzed, or
   * <code>null</code> to analyze a top-level variable.
   * @param property The property or variable to return the read-only status for.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then
   * <code>true</code> if the property is read-only or <code>false</code>
   * if not; otherwise undefined.
   * @throws NullPointerException if context is <code>null</code>
   * @throws PropertyNotFoundException if the given (base, property) pair is handled by this
   * <code>ELResolver</code> but the specified variable or property
   * does not exist.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  public abstract boolean isReadOnly(ExpressionContext context, Object base, Object property);

  /**
   * Converts an object to a specific type.
   *
   * <p>
   * An <code>ELException</code> is thrown if an error occurs during the
   * conversion.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param obj The object to convert.
   * @param targetType The target type for the convertion.
   * @throws ExpressionException thrown if errors occur.
   */
  public Object convertToType(ExpressionContext context, Object obj, Class<?> targetType) {
    return null;
  }
}
