/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.view;

import java.util.Iterator;
import java.util.Map;

import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Model that defines a holder for model attributes.
 * Primarily designed for adding attributes to the model.
 * Allows for accessing the overall model as a {@code java.util.Map}.
 *
 * @author TODAY <br>
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
   * Return the names of all attributes.
   *
   * @since 4.0
   */
  default String[] getAttributeNames() {
    return CollectionUtils.toArray(attributeNames(), Constant.EMPTY_STRING_ARRAY);
  }

  /**
   * Return the names Iterator.
   *
   * @since 4.0
   */
  Iterator<String> attributeNames();

}
