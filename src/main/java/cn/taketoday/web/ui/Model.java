/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.ui;

import java.util.Enumeration;
import java.util.Map;

import cn.taketoday.context.utils.ConvertUtils;

/**
 * @author TODAY <br>
 *         2018-10-14 20:30
 */
public interface Model {

  /**
   * Contains a attribute with given name
   *
   * @param name
   *            Attribute name
   * @return if contains the attribute
   */
  default boolean containsAttribute(String name) {
    return attribute(name) == null;
  }

  /**
   * Add the attributes from map
   *
   * @param attributes
   *            The attributes
   * @return this
   */
  Model attributes(Map<String, Object> attributes);

  /**
   * Returns an <code>Enumeration</code> containing the names of the attributes
   * available to this request. This method returns an empty
   * <code>Enumeration</code> if the request has no attributes available to it.
   *
   * @return an <code>Enumeration</code> of strings containing the names of the
   *         request's attributes
   */
  Enumeration<String> attributes();

  /**
   * Returns the value of the named attribute as an <code>Object</code>, or
   * <code>null</code> if no attribute of the given name exists.
   *
   * @param name
   *            a <code>String</code> specifying the name of the attribute
   *
   * @return an <code>Object</code> containing the value of the attribute, or
   *         <code>null</code> if the attribute does not exist
   */
  Object attribute(String name);

  /**
   * Returns the value of the named attribute as an <code>Object</code>, or
   * <code>null</code> if no attribute of the given name exists.
   *
   * @param name
   *            a <code>String</code> specifying the name of the attribute
   *
   * @param targetClass
   *            attribute will be use {@link ConvertUtils} convert to target class
   * @return an converted <code>Object</code> containing the value of the
   *         attribute, or <code>null</code> if the attribute does not exist
   */
  <T> T attribute(String name, Class<T> targetClass);

  /**
   * Stores an attribute in this request. Attributes are reset between requests..
   *
   * @param name
   *            a <code>String</code> specifying the name of the attribute
   * @param value
   *            the <code>Object</code> to be stored
   */
  Model attribute(String name, Object value);

  /**
   *
   * Removes an attribute from this request. This method is not generally needed
   * as attributes only persist as long as the request is being handled.
   *
   * @param name
   *            a <code>String</code> specifying the name of the attribute to
   *            remove
   */
  Model removeAttribute(String name);

  /**
   * Convert this model to a {@link Map}
   */
  Map<String, Object> asMap();

  /**
   * Clear all attributes
   */
  void clear();

}
