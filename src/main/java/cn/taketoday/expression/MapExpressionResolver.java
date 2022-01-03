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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines property resolution behavior on instances of {@link java.util.Map}.
 *
 * <p>
 * This resolver handles base objects of type <code>java.util.Map</code>. It
 * accepts any object as a property and uses that object as a key in the map.
 * The resulting value is the value in the map that is associated with that key.
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
 * @see java.util.Map
 * @since JSP 2.1
 */
public class MapExpressionResolver extends ExpressionResolver {
  private static final Class<?> theUnmodifiableMapClass = Collections.unmodifiableMap(new HashMap<>()).getClass();
  private final boolean isReadOnly;

  /**
   * Creates a new read/write <code>MapELResolver</code>.
   */
  public MapExpressionResolver() {
    this.isReadOnly = false;
  }

  /**
   * Creates a new <code>MapELResolver</code> whose read-only status is determined
   * by the given parameter.
   *
   * @param isReadOnly <code>true</code> if this resolver cannot modify maps;
   * <code>false</code> otherwise.
   */
  public MapExpressionResolver(boolean isReadOnly) {
    this.isReadOnly = isReadOnly;
  }

  /**
   * If the base object is a map, returns the most general acceptable type for a
   * value in this map.
   *
   * <p>
   * If the base is a <code>Map</code>, the <code>propertyResolved</code> property
   * of the <code>ELContext</code> object must be set to <code>true</code> by this
   * resolver, before returning. If this property is not <code>true</code> after
   * this method is called, the caller should ignore the return value.
   * </p>
   *
   * <p>
   * Assuming the base is a <code>Map</code>, this method will always return
   * <code>Object.class</code>. This is because <code>Map</code>s accept any
   * object as the value for a given key.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The map to analyze. Only bases of type <code>Map</code> are
   * handled by this resolver.
   * @param property The key to return the acceptable type for. Ignored by this
   * resolver.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the most
   * general acceptable type; otherwise undefined.
   * @throws NullPointerException if context is <code>null</code>
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  public Class<?> getType(ExpressionContext context, Object base, Object property) {
    if (base instanceof Map) {
      context.setPropertyResolved(true);
      return Object.class;
    }
    return null;
  }

  /**
   * If the base object is a map, returns the value associated with the given key,
   * as specified by the <code>property</code> argument. If the key was not found,
   * <code>null</code> is returned.
   *
   * <p>
   * If the base is a <code>Map</code>, the <code>propertyResolved</code> property
   * of the <code>ELContext</code> object must be set to <code>true</code> by this
   * resolver, before returning. If this property is not <code>true</code> after
   * this method is called, the caller should ignore the return value.
   * </p>
   *
   * <p>
   * Just as in {@link java.util.Map#get}, just because <code>null</code> is
   * returned doesn't mean there is no mapping for the key; it's also possible
   * that the <code>Map</code> explicitly maps the key to <code>null</code>.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The map to be analyzed. Only bases of type <code>Map</code> are
   * handled by this resolver.
   * @param property The key whose associated value is to be returned.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the value
   * associated with the given key or <code>null</code> if the key was not
   * found. Otherwise, undefined.
   * @throws ClassCastException if the key is of an inappropriate type for this map (optionally
   * thrown by the underlying <code>Map</code>).
   * @throws NullPointerException if context is <code>null</code>, or if the key is null and this
   * map does not permit null keys (the latter is optionally thrown by
   * the underlying <code>Map</code>).
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @SuppressWarnings("rawtypes")
  public Object getValue(ExpressionContext context, Object base, Object property) {
    if (base instanceof Map) {
      context.setPropertyResolved(base, property);
      return ((Map) base).get(property);
    }
    return null;
  }

  /**
   * If the base object is a map, attempts to set the value associated with the
   * given key, as specified by the <code>property</code> argument.
   *
   * <p>
   * If the base is a <code>Map</code>, the <code>propertyResolved</code> property
   * of the <code>ELContext</code> object must be set to <code>true</code> by this
   * resolver, before returning. If this property is not <code>true</code> after
   * this method is called, the caller can safely assume no value was set.
   * </p>
   *
   * <p>
   * If this resolver was constructed in read-only mode, this method will always
   * throw <code>PropertyNotWritableException</code>.
   * </p>
   *
   * <p>
   * If a <code>Map</code> was created using
   * {@link java.util.Collections#unmodifiableMap}, this method must throw
   * <code>PropertyNotWritableException</code>. Unfortunately, there is no
   * Collections API method to detect this. However, an implementation can create
   * a prototype unmodifiable <code>Map</code> and query its runtime type to see
   * if it matches the runtime type of the base object as a workaround.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The map to be modified. Only bases of type <code>Map</code> are
   * handled by this resolver.
   * @param property The key with which the specified value is to be associated.
   * @param val The value to be associated with the specified key.
   * @throws ClassCastException if the class of the specified key or value prevents it from being
   * stored in this map.
   * @throws NullPointerException if context is <code>null</code>, or if this map does not permit
   * <code>null</code> keys or values, and the specified key or value
   * is <code>null</code>.
   * @throws IllegalArgumentException if some aspect of this key or value prevents it from being stored
   * in this map.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   * @throws PropertyNotWritableException if this resolver was constructed in read-only mode, or if the put
   * operation is not supported by the underlying map.
   */
  @SuppressWarnings({ "unchecked" })
  public void setValue(ExpressionContext context, Object base, Object property, Object val) {
    if (base instanceof Map map) {
      context.setPropertyResolved(base, property);
      // The cast is safe
      if (isReadOnly || map.getClass() == theUnmodifiableMapClass) {
        throw new PropertyNotWritableException();
      }
      try {
        map.put(property, val);
      }
      catch (UnsupportedOperationException ex) {
        throw new PropertyNotWritableException();
      }
    }
  }

  /**
   * If the base object is a map, returns whether a call to {@link #setValue} will
   * always fail.
   *
   * <p>
   * If the base is a <code>Map</code>, the <code>propertyResolved</code> property
   * of the <code>ELContext</code> object must be set to <code>true</code> by this
   * resolver, before returning. If this property is not <code>true</code> after
   * this method is called, the caller should ignore the return value.
   * </p>
   *
   * <p>
   * If this resolver was constructed in read-only mode, this method will always
   * return <code>true</code>.
   * </p>
   *
   * <p>
   * If a <code>Map</code> was created using
   * {@link java.util.Collections#unmodifiableMap}, this method must return
   * <code>true</code>. Unfortunately, there is no Collections API method to
   * detect this. However, an implementation can create a prototype unmodifiable
   * <code>Map</code> and query its runtime type to see if it matches the runtime
   * type of the base object as a workaround.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base The map to analyze. Only bases of type <code>Map</code> are
   * handled by this resolver.
   * @param property The key to return the read-only status for. Ignored by this
   * resolver.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then
   * <code>true</code> if calling the <code>setValue</code> method will
   * always fail or <code>false</code> if it is possible that such a call
   * may succeed; otherwise undefined.
   * @throws NullPointerException if context is <code>null</code>
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @SuppressWarnings("rawtypes")
  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {
    if (base instanceof Map) {
      context.setPropertyResolved(true);
      return isReadOnly || ((Map) base).getClass() == theUnmodifiableMapClass;
    }
    return false;
  }
}
