/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.ui;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

import infra.core.Conventions;
import infra.lang.Unmodifiable;

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
   * Checks whether the model contains an attribute with the given name.
   *
   * @param name the name of the attribute to check
   * @return {@code true} if the attribute exists in the model, {@code false} otherwise
   */
  boolean containsAttribute(String name);

  /**
   * Add all attributes from the given map to the model.
   *
   * @param attributes the map containing attributes to be added
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
  @Nullable
  Object removeAttribute(String name);

  /**
   * Convert this model to a {@link Map}
   *
   * @return a {@link Map} representation of this model
   */
  Map<String, @Nullable Object> asMap();

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
  String[] getAttributeNames();

  /**
   * Return the names of all attributes.
   *
   * @return an {@code Iterable} of attribute names
   * @since 4.0
   */
  @Unmodifiable
  Iterable<String> attributeNames();

  /**
   * Add the supplied attribute to this {@code Map} using a
   * {@link Conventions#getVariableName generated name}.
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
