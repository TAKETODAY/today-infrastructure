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
 * An <code>Expression</code> that can get or set a value.
 *
 * <p>
 * In previous incarnations of this API, expressions could only be read.
 * <code>ValueExpression</code> objects can now be used both to retrieve a value
 * and to set a value. Expressions that can have a value set on them are
 * referred to as l-value expressions. Those that cannot are referred to as
 * r-value expressions. Not all r-value expressions can be used as l-value
 * expressions (e.g. <code>"${1+1}"</code> or
 * <code>"${firstName} ${lastName}"</code>). See the EL Specification for
 * details. Expressions that cannot be used as l-values must always return
 * <code>true</code> from <code>isReadOnly()</code>.
 * </p>
 *
 * <p>
 * The <code>{@link ExpressionFactory#createValueExpression}</code> method can
 * be used to parse an expression string and return a concrete instance of
 * <code>ValueExpression</code> that encapsulates the parsed expression. The
 * {@link FunctionMapper} is used at parse time, not evaluation time, so one is
 * not needed to evaluate an expression using this class. However, the
 * {@link ExpressionContext} is needed at evaluation time.
 * </p>
 *
 * <p>
 * The {@link #getValue}, {@link #setValue}, {@link #isReadOnly},
 * {@link #getType} and {@link #getValueReference} methods will evaluate the
 * expression each time they are called. The {@link ExpressionResolver} in the
 * <code>ELContext</code> is used to resolve the top-level variables and to
 * determine the behavior of the <code>.</code> and <code>[]</code> operators.
 * For any of the five methods, the {@link ExpressionResolver#getValue} method is used
 * to resolve all properties up to but excluding the last one. This provides the
 * <code>base</code> object. For all methods other than the
 * {@link #getValueReference} method, at the last resolution, the
 * <code>ValueExpression</code> will call the corresponding
 * {@link ExpressionResolver#getValue}, {@link ExpressionResolver#setValue},
 * {@link ExpressionResolver#isReadOnly} or {@link ExpressionResolver#getType} method, depending
 * on which was called on the <code>ValueExpression</code>. For the
 * {@link #getValueReference} method, the (base, property) is not resolved by
 * the ELResolver, but an instance of {@link ValueReference} is created to
 * encapsulate this (base ,property), and returned.
 * </p>
 *
 * <p>
 * See the notes about comparison, serialization and immutability in the
 * {@link Expression} javadocs.
 *
 * @see ExpressionResolver
 * @see Expression
 * @see ExpressionFactory
 * @since JSP 2.1
 */
@SuppressWarnings("serial")
public abstract class ValueExpression extends Expression {

  /**
   * Evaluates the expression relative to the provided context, and returns the
   * resulting value.
   *
   * <p>
   * The resulting value is automatically coerced to the type returned by
   * <code>getExpectedType()</code>, which was provided to the
   * <code>ExpressionFactory</code> when this expression was created.
   * </p>
   *
   * @param context The context of this evaluation.
   * @return The result of the expression evaluation.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws PropertyNotFoundException if one of the property resolutions failed because a specified
   * variable or property does not exist or is not readable.
   * @throws ExpressionException if an exception was thrown while performing property or variable
   * resolution. The thrown exception must be included as the cause
   * property of this exception, if available.
   */
  public abstract Object getValue(ExpressionContext context);

  /**
   * Evaluates the expression relative to the provided context, and returns the
   * resulting value.
   *
   * <p>
   * The resulting value is automatically coerced to the type returned by
   * <code>getExpectedType()</code>, which was provided to the
   * <code>ExpressionFactory</code> when this expression was created.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param requiredType required return type.
   * @return The result of the expression evaluation.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws PropertyNotFoundException if one of the property resolutions failed because a specified
   * variable or property does not exist or is not readable.
   * @throws ExpressionException if an exception was thrown while performing property or variable
   * resolution. The thrown exception must be included as the cause
   * property of this exception, if available.
   * @since 4.0
   */
  public abstract <T> T getValue(ExpressionContext context, @Nullable Class<T> requiredType);

  /**
   * Evaluates the expression relative to the provided context, and sets the
   * result to the provided value.
   *
   * @param context The context of this evaluation.
   * @param value The new value to be set.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws PropertyNotFoundException if one of the property resolutions failed because a specified
   * variable or property does not exist or is not readable.
   * @throws PropertyNotWritableException if the final variable or property resolution failed because the
   * specified variable or property is not writable.
   * @throws ExpressionException if an exception was thrown while attempting to set the property
   * or variable. The thrown exception must be included as the cause
   * property of this exception, if available.
   */
  public abstract void setValue(ExpressionContext context, Object value);

  /**
   * Evaluates the expression relative to the provided context, and returns
   * <code>true</code> if a call to {@link #setValue} will always fail.
   *
   * @param context The context of this evaluation.
   * @return <code>true</code> if the expression is read-only or
   * <code>false</code> if not.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws PropertyNotFoundException if one of the property resolutions failed because a specified
   * variable or property does not exist or is not readable.
   * @throws ExpressionException if an exception was thrown while performing property or variable
   * resolution. The thrown exception must be included as the cause
   * property of this exception, if available. * @throws
   * NullPointerException if context is <code>null</code>
   */
  public abstract boolean isReadOnly(ExpressionContext context);

  /**
   * Evaluates the expression relative to the provided context, and returns the
   * most general type that is acceptable for an object to be passed as the
   * <code>value</code> parameter in a future call to the {@link #setValue}
   * method.
   *
   * <p>
   * This is not always the same as <code>getValue().getClass()</code>. For
   * example, in the case of an expression that references an array element, the
   * <code>getType</code> method will return the element type of the array, which
   * might be a superclass of the type of the actual element that is currently in
   * the specified array element.
   * </p>
   *
   * @param context The context of this evaluation.
   * @return the most general acceptable type; otherwise undefined.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws PropertyNotFoundException if one of the property resolutions failed because a specified
   * variable or property does not exist or is not readable.
   * @throws ExpressionException if an exception was thrown while performing property or variable
   * resolution. The thrown exception must be included as the cause
   * property of this exception, if available.
   */
  public abstract Class<?> getType(ExpressionContext context);

  /**
   * Returns the type the result of the expression will be coerced to after
   * evaluation.
   *
   * @return the <code>expectedType</code> passed to the
   * <code>ExpressionFactory.createValueExpression</code> method that
   * created this <code>ValueExpression</code>.
   */
  @Nullable
  public abstract Class<?> getExpectedType();

  /**
   * Returns a {@link ValueReference} for this expression instance.
   *
   * @param context the context of this evaluation
   * @return the <code>ValueReference</code> for this
   * <code>ValueExpression</code>, or <code>null</code> if this
   * <code>ValueExpression</code> is not a reference to a base (null or
   * non-null) and a property. If the base is null, and the property is a
   * EL variable, return the <code>ValueReference</code> for the
   * <code>ValueExpression</code> associated with this EL variable.
   * @since EL 2.2
   */
  public ValueReference getValueReference(ExpressionContext context) {
    return null;
  }
}
