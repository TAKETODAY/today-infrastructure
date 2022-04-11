/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Implementation of {@link java.util.Map} for use when building model data for use
 * with UI tools. Supports chained calls and generation of model attribute names.
 *
 * <p>This class serves as generic model holder for Servlet MVC but is not tied to it.
 * Check out the {@link Model} interface for an interface variant.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Conventions#getVariableName
 * @see ModelAndView
 * @since 4.0 2022/4/8 22:58
 */
@SuppressWarnings("serial")
public class ModelMap extends LinkedHashMap<String, Object> implements Model {

  /**
   * Construct a new, empty {@code ModelMap}.
   */
  public ModelMap() { }

  /**
   * Construct a new {@code ModelMap} containing the supplied attribute
   * under the supplied name.
   *
   * @see #addAttribute(String, Object)
   */
  public ModelMap(String attributeName, @Nullable Object attributeValue) {
    addAttribute(attributeName, attributeValue);
  }

  /**
   * Construct a new {@code ModelMap} containing the supplied attribute.
   * Uses attribute name generation to generate the key for the supplied model
   * object.
   *
   * @see #addAttribute(Object)
   */
  public ModelMap(Object attributeValue) {
    addAttribute(attributeValue);
  }

  /**
   * Add the supplied attribute under the supplied name.
   *
   * @param attributeName the name of the model attribute (never {@code null})
   * @param attributeValue the model attribute value (can be {@code null})
   */
  public ModelMap addAttribute(String attributeName, @Nullable Object attributeValue) {
    Assert.notNull(attributeName, "Model attribute name must not be null");
    put(attributeName, attributeValue);
    return this;
  }

  /**
   * Add the supplied attribute to this {@code Map} using a
   * {@link Conventions#getVariableName generated name}.
   * <p><i>Note: Empty {@link Collection Collections} are not added to
   * the model when using this method because we cannot correctly determine
   * the true convention name. View code should check for {@code null} rather
   * than for empty collections as is already done by JSTL tags.</i>
   *
   * @param attributeValue the model attribute value (never {@code null})
   */
  public ModelMap addAttribute(Object attributeValue) {
    Assert.notNull(attributeValue, "Model object must not be null");
    if (attributeValue instanceof Collection && ((Collection<?>) attributeValue).isEmpty()) {
      return this;
    }
    return addAttribute(Conventions.getVariableName(attributeValue), attributeValue);
  }

  /**
   * Copy all attributes in the supplied {@code Collection} into this
   * {@code Map}, using attribute name generation for each element.
   *
   * @see #addAttribute(Object)
   */
  public ModelMap addAllAttributes(@Nullable Collection<?> attributeValues) {
    if (attributeValues != null) {
      for (Object attributeValue : attributeValues) {
        addAttribute(attributeValue);
      }
    }
    return this;
  }

  /**
   * Copy all attributes in the supplied {@code Map} into this {@code Map}.
   *
   * @see #addAttribute(String, Object)
   */
  public ModelMap addAllAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      putAll(attributes);
    }
    return this;
  }

  @Override
  public Model addAllAttributes(@Nullable Model attributes) {
    if (attributes != null && !attributes.isEmpty()) {
      putAll(attributes.asMap());
    }
    return this;
  }

  /**
   * Copy all attributes in the supplied {@code Map} into this {@code Map},
   * with existing objects of the same name taking precedence (i.e. not getting
   * replaced).
   */
  public ModelMap mergeAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      attributes.forEach((key, value) -> {
        if (!containsKey(key)) {
          put(key, value);
        }
      });
    }
    return this;
  }

  /**
   * Does this model contain an attribute of the given name?
   *
   * @param attributeName the name of the model attribute (never {@code null})
   * @return whether this model contains a corresponding attribute
   */
  public boolean containsAttribute(String attributeName) {
    return containsKey(attributeName);
  }

  /**
   * Return the attribute value for the given name, if any.
   *
   * @param attributeName the name of the model attribute (never {@code null})
   * @return the corresponding attribute value, or {@code null} if none
   */
  @Nullable
  public Object getAttribute(String attributeName) {
    return get(attributeName);
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    put(name, value);
  }

  @Override
  public Object removeAttribute(String name) {
    return null;
  }

  @Override
  public Map<String, Object> asMap() {
    return null;
  }

  @Override
  public Iterator<String> attributeNames() {
    return null;
  }

}
