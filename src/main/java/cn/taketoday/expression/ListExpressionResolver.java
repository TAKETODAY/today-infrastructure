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
import java.util.Collections;
import java.util.List;

/**
 * Defines property resolution behavior on instances of {@link java.util.List}.
 *
 * <p>
 * This resolver handles base objects of type <code>java.util.List</code>. It
 * accepts any object as a property and coerces that object into an integer
 * index into the list. The resulting value is the value in the list at that
 * index.
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
 * {@link ExpressionResolverComposite}s, to define rich semantics for evaluating an
 * expression. See the javadocs for {@link ExpressionResolver} for details.
 * </p>
 *
 * @see ExpressionResolverComposite
 * @see ExpressionResolver
 * @see java.util.List
 * @since JSP 2.1
 */
public class ListExpressionResolver extends ExpressionResolver {
  private static final Class<?> theUnmodifiableListClass = Collections.unmodifiableList(new ArrayList<>()).getClass();

  private final boolean isReadOnly;

  /**
   * Creates a new read/write <code>ListELResolver</code>.
   */
  public ListExpressionResolver() {
    this.isReadOnly = false;
  }

  /**
   * Creates a new <code>ListELResolver</code> whose read-only status is
   * determined by the given parameter.
   *
   * @param isReadOnly <code>true</code> if this resolver cannot modify lists;
   * <code>false</code> otherwise.
   */
  public ListExpressionResolver(boolean isReadOnly) {
    this.isReadOnly = isReadOnly;
  }

  /**
   * If the base object is a list, returns the most general acceptable type for a
   * value in this list.
   *
   * <p>
   * If the base is a <code>List</code>, the <code>propertyResolved</code>
   * property of the <code>ELContext</code> object must be set to
   * <code>true</code> by this resolver, before returning. If this property is not
   * <code>true</code> after this method is called, the caller should ignore the
   * return value.
   * </p>
   *
   * <p>
   * Assuming the base is a <code>List</code>, this method will always return
   * <code>Object.class</code>. This is because <code>List</code>s accept any
   * object as an element.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The list to analyze. Only bases of type <code>List</code> are
   * handled by this resolver.
   * @param property The index of the element in the list to return the acceptable type
   * for. Will be coerced into an integer, but otherwise ignored by
   * this resolver.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the most
   * general acceptable type; otherwise undefined.
   * @throws PropertyNotFoundException if the given index is out of bounds for this list.
   * @throws NullPointerException if context is <code>null</code>
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public Class<?> getType(ExpressionContext context, Object base, Object property) {
    if (base instanceof List) {
      context.setPropertyResolved(true);
      final int index = toInteger(property);
      if (index < 0 || index >= ((List<?>) base).size()) {
        throw new PropertyNotFoundException();
      }
      return Object.class;
    }
    return null;
  }

  /**
   * If the base object is a list, returns the value at the given index. The index
   * is specified by the <code>property</code> argument, and coerced into an
   * integer. If the coercion could not be performed, an
   * <code>IllegalArgumentException</code> is thrown. If the index is out of
   * bounds, <code>null</code> is returned.
   *
   * <p>
   * If the base is a <code>List</code>, the <code>propertyResolved</code>
   * property of the <code>ELContext</code> object must be set to
   * <code>true</code> by this resolver, before returning. If this property is not
   * <code>true</code> after this method is called, the caller should ignore the
   * return value.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The list to be analyzed. Only bases of type <code>List</code> are
   * handled by this resolver.
   * @param property The index of the value to be returned. Will be coerced into an
   * integer.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the value
   * at the given index or <code>null</code> if the index was out of
   * bounds. Otherwise, undefined.
   * @throws IllegalArgumentException if the property could not be coerced into an integer.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public Object getValue(ExpressionContext context, Object base, Object property) {
    if (base instanceof final List<?> list) {
      context.setPropertyResolved(base, property);
      final int index = toInteger(property);
      if (index < 0 || index >= list.size()) {
        return null;
      }
      return list.get(index);
    }
    return null;
  }

  /**
   * If the base object is a list, attempts to set the value at the given index
   * with the given value. The index is specified by the <code>property</code>
   * argument, and coerced into an integer. If the coercion could not be
   * performed, an <code>IllegalArgumentException</code> is thrown. If the index
   * is out of bounds, a <code>PropertyNotFoundException</code> is thrown.
   *
   * <p>
   * If the base is a <code>List</code>, the <code>propertyResolved</code>
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
   * If a <code>List</code> was created using
   * {@link java.util.Collections#unmodifiableList}, this method must throw
   * <code>PropertyNotWritableException</code>. Unfortunately, there is no
   * Collections API method to detect this. However, an implementation can create
   * a prototype unmodifiable <code>List</code> and query its runtime type to see
   * if it matches the runtime type of the base object as a workaround.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The list to be modified. Only bases of type <code>List</code> are
   * handled by this resolver.
   * @param property The index of the value to be set. Will be coerced into an integer.
   * @param val The value to be set at the given index.
   * @throws ClassCastException if the class of the specified element prevents it from being
   * added to this list.
   * @throws NullPointerException if context is <code>null</code>, or if the value is
   * <code>null</code> and this <code>List</code> does not support
   * <code>null</code> elements.
   * @throws IllegalArgumentException if the property could not be coerced into an integer, or if some
   * aspect of the specified element prevents it from being added to
   * this list.
   * @throws PropertyNotWritableException if this resolver was constructed in read-only mode, or if the set
   * operation is not supported by the underlying list.
   * @throws PropertyNotFoundException if the given index is out of bounds for this list.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void setValue(ExpressionContext context, Object base, Object property, Object val) {

    if (base instanceof List) {
      context.setPropertyResolved(base, property);
      int index = toInteger(property);
      if (isReadOnly) {
        throw new PropertyNotWritableException();
      }
      try {
        ((List) base).set(index, val);
      }
      catch (UnsupportedOperationException ex) {
        throw new PropertyNotWritableException();
      }
      catch (IndexOutOfBoundsException ex) {
        throw new PropertyNotFoundException();
      }
      catch (ClassCastException | NullPointerException | IllegalArgumentException ex) {
        throw ex;
      }
    }
  }

  /**
   * If the base object is a list, returns whether a call to {@link #setValue}
   * will always fail.
   *
   * <p>
   * If the base is a <code>List</code>, the <code>propertyResolved</code>
   * property of the <code>ELContext</code> object must be set to
   * <code>true</code> by this resolver, before returning. If this property is not
   * <code>true</code> after this method is called, the caller should ignore the
   * return value.
   * </p>
   *
   * <p>
   * If this resolver was constructed in read-only mode, this method will always
   * return <code>true</code>.
   * </p>
   *
   * <p>
   * If a <code>List</code> was created using
   * {@link java.util.Collections#unmodifiableList}, this method must return
   * <code>true</code>. Unfortunately, there is no Collections API method to
   * detect this. However, an implementation can create a prototype unmodifiable
   * <code>List</code> and query its runtime type to see if it matches the runtime
   * type of the base object as a workaround.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The list to analyze. Only bases of type <code>List</code> are
   * handled by this resolver.
   * @param property The index of the element in the list to return the acceptable type
   * for. Will be coerced into an integer, but otherwise ignored by
   * this resolver.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then
   * <code>true</code> if calling the <code>setValue</code> method will
   * always fail or <code>false</code> if it is possible that such a call
   * may succeed; otherwise undefined.
   * @throws PropertyNotFoundException if the given index is out of bounds for this list.
   * @throws NullPointerException if context is <code>null</code>
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {
    if (base instanceof final List<?> list) {
      context.setPropertyResolved(true);
      final int index = toInteger(property);
      if (index < 0 || index >= list.size()) {
        throw new PropertyNotFoundException();
      }
      return list.getClass() == theUnmodifiableListClass || isReadOnly;
    }
    return false;
  }

  private static int toInteger(Object p) {
    if (p instanceof Integer) {
      return (Integer) p;
    }
    if (p instanceof Character) {
      return (Character) p;
    }
    if (p instanceof Number) {
      return ((Number) p).intValue();
    }
    if (p instanceof String) {
      return Integer.parseInt((String) p);
    }
    throw new IllegalArgumentException();
  }
}
