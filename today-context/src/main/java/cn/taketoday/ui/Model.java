/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.ui;

import java.util.Collection;
import java.util.Map;

import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Unmodifiable;
import cn.taketoday.util.CollectionUtils;

/**
 * Interface that defines a holder for model attributes.
 *
 * <p>Primarily designed for adding attributes to the model.
 * <p>Allows for accessing the overall model as a {@code java.util.Map}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-14 20:30
 */
public interface Model {

  /**
   * Contains a attribute with given name
   *
   * @param name Attribute name
   * @return if contains the attribute
   */
  default boolean containsAttribute(String name) {
    return getAttribute(name) == null;
  }

  /**
   * Add the attributes from map
   *
   * @param attributes The attributes
   */
  default void setAttributes(Map<String, Object> attributes) {
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      setAttribute(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Returns the value of the named attribute as an <code>Object</code>, or
   * <code>null</code> if no attribute of the given name exists.
   *
   * @param name a <code>String</code> specifying the name of the attribute
   * @return an <code>Object</code> containing the value of the attribute, or
   * <code>null</code> if the attribute does not exist
   */
  @Nullable
  Object getAttribute(String name);

  /**
   * Stores an attribute in this request. Attributes are reset between requests..
   *
   * @param name a <code>String</code> specifying the name of the attribute
   * @param value the <code>Object</code> to be stored
   */
  void setAttribute(String name, @Nullable Object value);

  /**
   * Add the supplied attribute under the supplied name.
   *
   * @param attributeName the name of the model attribute (never {@code null})
   * @param attributeValue the model attribute value (can be {@code null})
   * @since 4.0
   */
  Model addAttribute(String attributeName, @Nullable Object attributeValue);

  /**
   * Removes an attribute from this request. This method is not generally needed
   * as attributes only persist as long as the request is being handled.
   *
   * @param name a <code>String</code> specifying the name of the attribute to
   * remove
   * @return the last value of the attribute, if any
   */
  Object removeAttribute(String name);

  /**
   * Convert this model to a {@link Map}
   */
  Map<String, Object> asMap();

  /**
   * Clear all attributes
   */
  void clear();

  /**
   * Returns {@code true} if this map contains no key-value mappings.
   *
   * @return {@code true} if this map contains no key-value mappings
   * @since 4.0
   */
  boolean isEmpty();

  /**
   * Return the names of all attributes.
   *
   * @since 4.0
   */
  default String[] getAttributeNames() {
    return CollectionUtils.toArray(attributeNames().iterator(), Constant.EMPTY_STRING_ARRAY);
  }

  /**
   * Return the names.
   *
   * @since 4.0
   */
  @Unmodifiable
  Iterable<String> attributeNames();

  /**
   * Add the supplied attribute to this {@code Map} using a
   * {@link cn.taketoday.core.Conventions#getVariableName generated name}.
   * <p><i>Note: Empty {@link java.util.Collection Collections} are not added to
   * the model when using this method because we cannot correctly determine
   * the true convention name. View code should check for {@code null} rather
   * than for empty collections as is already done by JSTL tags.</i>
   *
   * @param attributeValue the model attribute value
   * @since 4.0
   */
  Model addAttribute(Object attributeValue);

  /**
   * Copy all attributes in the supplied {@code Collection} into this
   * {@code Map}, using attribute name generation for each element.
   *
   * @see #addAttribute(Object)
   * @since 4.0
   */
  Model addAllAttributes(@Nullable Collection<?> attributeValues);

  /**
   * Copy all attributes in the supplied {@code Map} into this {@code Map}.
   *
   * @see #setAttribute(String, Object)
   * @since 4.0
   */
  Model addAllAttributes(@Nullable Map<String, ?> attributes);

  /**
   * Copy all attributes in the supplied {@code Map} into this {@code Map},
   * with existing objects of the same name taking precedence (i.e. not getting
   * replaced).
   *
   * @since 4.0
   */
  Model mergeAttributes(@Nullable Map<String, ?> attributes);

}
