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

import java.util.ArrayList;

import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.CollectionUtils;

/**
 * Maintains an ordered composite list of child <code>ELResolver</code>s.
 *
 * <p>
 * Though only a single <code>ELResolver</code> is associated with an
 * <code>ELContext</code>, there are usually multiple resolvers considered for
 * any given variable or property resolution. <code>ELResolver</code>s are
 * combined together using a <code>CompositeELResolver</code>, to define rich
 * semantics for evaluating an expression.
 * </p>
 *
 * <p>
 * For the {@link #getValue}, {@link #getType}, {@link #setValue} and
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
 * The <code>CompositeELResolver</code> initializes the
 * <code>ELContext.propertyResolved</code> flag to <code>false</code>, and uses
 * it as a stop condition for iterating through its component resolvers.
 * </p>
 *
 * @see ExpressionContext
 * @see ExpressionResolver
 * @since JSP 2.1
 */
public class ExpressionResolverComposite extends ExpressionResolver implements ArraySizeTrimmer {

  private final ArrayList<ExpressionResolver> elResolvers;

  public ExpressionResolverComposite() {
    this.elResolvers = new ArrayList<>();
  }

  public ExpressionResolverComposite(int size) {
    this.elResolvers = new ArrayList<>(size);
  }

  public ExpressionResolverComposite(ExpressionResolver... resolvers) {
    this.elResolvers = new ArrayList<>(resolvers.length);
    CollectionUtils.addAll(elResolvers, resolvers);
  }

  /**
   * Adds the given resolver to the list of component resolvers.
   *
   * <p>
   * Resolvers are consulted in the order in which they are added.
   * </p>
   *
   * @param elResolver The component resolver to add.
   * @throws NullPointerException If the provided resolver is <code>null</code>.
   */
  public void add(ExpressionResolver elResolver) {
    Assert.notNull(elResolver, "ExpressionResolver is required");
    elResolvers.add(elResolver);
  }

  /**
   * Attempts to resolve the given <code>property</code> object on the given
   * <code>base</code> object by querying all component resolvers.
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
   * First, <code>propertyResolved</code> is set to <code>false</code> on the
   * provided <code>ELContext</code>.
   * </p>
   *
   * <p>
   * Next, for each component resolver in this composite:
   * <ol>
   * <li>The <code>getValue()</code> method is called, passing in the provided
   * <code>context</code>, <code>base</code> and <code>property</code>.</li>
   * <li>If the <code>ELContext</code>'s <code>propertyResolved</code> flag is
   * <code>false</code> then iteration continues.</li>
   * <li>Otherwise, iteration stops and no more component resolvers are
   * considered. The value returned by <code>getValue()</code> is returned by this
   * method.</li>
   * </ol>
   * </p>
   *
   * <p>
   * If none of the component resolvers were able to perform this operation, the
   * value <code>null</code> is returned and the <code>propertyResolved</code>
   * flag remains set to <code>false</code>
   * </p>
   * .
   *
   * <p>
   * Any exception thrown by component resolvers during the iteration is
   * propagated to the caller of this method.
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
  @Override
  public Object getValue(ExpressionContext context, Object base, Object property) {
    context.setPropertyResolved(false);
    for (ExpressionResolver elResolver : elResolvers) {
      Object value = elResolver.getValue(context, base, property);
      if (context.isPropertyResolved()) {
        return value;
      }
    }
    return null;
  }

  /**
   * Attemps to resolve and invoke the given <code>method</code> on the given
   * <code>base</code> object by querying all component resolvers.
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
   * First, <code>propertyResolved</code> is set to <code>false</code> on the
   * provided <code>ELContext</code>.
   * </p>
   *
   * <p>
   * Next, for each component resolver in this composite:
   * <ol>
   * <li>The <code>invoke()</code> method is called, passing in the provided
   * <code>context</code>, <code>base</code>, <code>method</code>,
   * <code>paramTypes</code>, and <code>params</code>.</li>
   * <li>If the <code>ELContext</code>'s <code>propertyResolved</code> flag is
   * <code>false</code> then iteration continues.</li>
   * <li>Otherwise, iteration stops and no more component resolvers are
   * considered. The value returned by <code>getValue()</code> is returned by this
   * method.</li>
   * </ol>
   * </p>
   *
   * <p>
   * If none of the component resolvers were able to perform this operation, the
   * value <code>null</code> is returned and the <code>propertyResolved</code>
   * flag remains set to <code>false</code>
   * </p>
   * .
   *
   * <p>
   * Any exception thrown by component resolvers during the iteration is
   * propagated to the caller of this method.
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
   * @since EL 2.2
   */
  @Override
  public Object invoke(ExpressionContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
    context.setPropertyResolved(false);
    for (ExpressionResolver elResolver : elResolvers) {
      Object value = elResolver.invoke(context, base, method, paramTypes, params);
      if (context.isPropertyResolved()) {
        return value;
      }
    }
    return null;
  }

  /**
   * For a given <code>base</code> and <code>property</code>, attempts to identify
   * the most general type that is acceptable for an object to be passed as the
   * <code>value</code> parameter in a future call to the {@link #setValue}
   * method. The result is obtained by querying all component resolvers.
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
   * First, <code>propertyResolved</code> is set to <code>false</code> on the
   * provided <code>ELContext</code>.
   * </p>
   *
   * <p>
   * Next, for each component resolver in this composite:
   * <ol>
   * <li>The <code>getType()</code> method is called, passing in the provided
   * <code>context</code>, <code>base</code> and <code>property</code>.</li>
   * <li>If the <code>ELContext</code>'s <code>propertyResolved</code> flag is
   * <code>false</code> then iteration continues.</li>
   * <li>Otherwise, iteration stops and no more component resolvers are
   * considered. The value returned by <code>getType()</code> is returned by this
   * method.</li>
   * </ol>
   * </p>
   *
   * <p>
   * If none of the component resolvers were able to perform this operation, the
   * value <code>null</code> is returned and the <code>propertyResolved</code>
   * flag remains set to <code>false</code>
   * </p>
   * .
   *
   * <p>
   * Any exception thrown by component resolvers during the iteration is
   * propagated to the caller of this method.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The base object whose property value is to be analyzed, or
   * <code>null</code> to analyze a top-level variable.
   * @param property The property or variable to return the acceptable type for.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the most
   * general acceptable type; otherwise undefined.
   * @throws NullPointerException if context is <code>null</code>
   * @throws PropertyNotFoundException if the given (base, property) pair is handled by this
   * <code>ELResolver</code> but the specified variable or property
   * does not exist or is not readable.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public Class<?> getType(ExpressionContext context, Object base, Object property) {
    context.setPropertyResolved(false);
    for (ExpressionResolver elResolver : elResolvers) {
      Class<?> type = elResolver.getType(context, base, property);
      if (context.isPropertyResolved()) {
        return type;
      }
    }
    return null;
  }

  /**
   * Attempts to set the value of the given <code>property</code> object on the
   * given <code>base</code> object. All component resolvers are asked to attempt
   * to set the value.
   *
   * <p>
   * If this resolver handles the given (base, property) pair, the
   * <code>propertyResolved</code> property of the <code>ELContext</code> object
   * must be set to <code>true</code> by the resolver, before returning. If this
   * property is not <code>true</code> after this method is called, the caller can
   * safely assume no value has been set.
   * </p>
   *
   * <p>
   * First, <code>propertyResolved</code> is set to <code>false</code> on the
   * provided <code>ELContext</code>.
   * </p>
   *
   * <p>
   * Next, for each component resolver in this composite:
   * <ol>
   * <li>The <code>setValue()</code> method is called, passing in the provided
   * <code>context</code>, <code>base</code>, <code>property</code> and
   * <code>value</code>.</li>
   * <li>If the <code>ELContext</code>'s <code>propertyResolved</code> flag is
   * <code>false</code> then iteration continues.</li>
   * <li>Otherwise, iteration stops and no more component resolvers are
   * considered.</li>
   * </ol>
   * </p>
   *
   * <p>
   * If none of the component resolvers were able to perform this operation, the
   * <code>propertyResolved</code> flag remains set to <code>false</code>
   * </p>
   * .
   *
   * <p>
   * Any exception thrown by component resolvers during the iteration is
   * propagated to the caller of this method.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The base object whose property value is to be set, or
   * <code>null</code> to set a top-level variable.
   * @param property The property or variable to be set.
   * @param val The value to set the property or variable to.
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
  @Override
  public void setValue(ExpressionContext context, Object base, Object property, Object val) {
    context.setPropertyResolved(false);
    for (ExpressionResolver elResolver : elResolvers) {
      elResolver.setValue(context, base, property, val);
      if (context.isPropertyResolved()) {
        return;
      }
    }
  }

  /**
   * For a given <code>base</code> and <code>property</code>, attempts to
   * determine whether a call to {@link #setValue} will always fail. The result is
   * obtained by querying all component resolvers.
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
   * First, <code>propertyResolved</code> is set to <code>false</code> on the
   * provided <code>ELContext</code>.
   * </p>
   *
   * <p>
   * Next, for each component resolver in this composite:
   * <ol>
   * <li>The <code>isReadOnly()</code> method is called, passing in the provided
   * <code>context</code>, <code>base</code> and <code>property</code>.</li>
   * <li>If the <code>ELContext</code>'s <code>propertyResolved</code> flag is
   * <code>false</code> then iteration continues.</li>
   * <li>Otherwise, iteration stops and no more component resolvers are
   * considered. The value returned by <code>isReadOnly()</code> is returned by
   * this method.</li>
   * </ol>
   * </p>
   *
   * <p>
   * If none of the component resolvers were able to perform this operation, the
   * value <code>false</code> is returned and the <code>propertyResolved</code>
   * flag remains set to <code>false</code>
   * </p>
   * .
   *
   * <p>
   * Any exception thrown by component resolvers during the iteration is
   * propagated to the caller of this method.
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
  @Override
  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {
    context.setPropertyResolved(false);
    for (ExpressionResolver elResolver : elResolvers) {
      boolean readOnly = elResolver.isReadOnly(context, base, property);
      if (context.isPropertyResolved()) {
        return readOnly;
      }
    }
    return false; // Does not matter
  }

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
   * @since EL 3.0
   */
  @Override
  public Object convertToType(ExpressionContext context, Object obj, Class<?> targetType) {
    context.setPropertyResolved(false);
    for (ExpressionResolver elResolver : elResolvers) {
      Object value = elResolver.convertToType(context, obj, targetType);
      if (context.isPropertyResolved()) {
        return value;
      }
    }
    return null;
  }

  @Override
  public void trimToSize() {
    elResolvers.trimToSize();
  }
}
