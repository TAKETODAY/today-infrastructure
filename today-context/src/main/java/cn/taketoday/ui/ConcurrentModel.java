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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Implementation of the {@link Model} interface based on a {@link ConcurrentHashMap}
 * for use in concurrent scenarios.
 *
 * <p>Exposed to handler methods by Infra WebFlux, typically via a declaration of the
 * {@link Model} interface. There is typically no need to create it within user code.
 * If necessary a handler method can return a regular {@code java.util.Map},
 * likely a {@code java.util.ConcurrentMap}, for a pre-determined model.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ConcurrentModel extends ConcurrentHashMap<String, Object> implements Model {

  /**
   * Construct a new, empty {@code ConcurrentModel}.
   */
  public ConcurrentModel() { }

  /**
   * Construct a new {@code ModelMap} containing the supplied attribute
   * under the supplied name.
   *
   * @see #addAttribute(String, Object)
   */
  public ConcurrentModel(String attributeName, Object attributeValue) {
    addAttribute(attributeName, attributeValue);
  }

  /**
   * Construct a new {@code ModelMap} containing the supplied attribute.
   * Uses attribute name generation to generate the key for the supplied model
   * object.
   *
   * @see #addAttribute(Object)
   */
  public ConcurrentModel(Object attributeValue) {
    addAttribute(attributeValue);
  }

  @Override
  @Nullable
  public Object put(String key, @Nullable Object value) {
    if (value != null) {
      return super.put(key, value);
    }
    else {
      return remove(key);
    }
  }

  @Override
  public void putAll(Map<? extends String, ?> map) {
    for (Entry<? extends String, ?> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Add the supplied attribute under the supplied name.
   *
   * @param attributeName the name of the model attribute (never {@code null})
   * @param attributeValue the model attribute value (ignored if {@code null},
   * just removing an existing entry if any)
   */
  @Override
  public ConcurrentModel addAttribute(String attributeName, @Nullable Object attributeValue) {
    Assert.notNull(attributeName, "Model attribute name is required");
    put(attributeName, attributeValue);
    return this;
  }

  @Override
  public Object removeAttribute(String name) {
    return remove(name);
  }

  /**
   * Add the supplied attribute to this {@code Map} using a
   * {@link cn.taketoday.core.Conventions#getVariableName generated name}.
   * <p><i>Note: Empty {@link Collection Collections} are not added to
   * the model when using this method because we cannot correctly determine
   * the true convention name. View code should check for {@code null} rather
   * than for empty collections as is already done by JSTL tags.</i>
   *
   * @param attributeValue the model attribute value (never {@code null})
   */
  @Override
  public ConcurrentModel addAttribute(Object attributeValue) {
    Assert.notNull(attributeValue, "Model attribute value is required");
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
  @Override
  public ConcurrentModel addAllAttributes(@Nullable Collection<?> attributeValues) {
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
  @Override
  public ConcurrentModel addAllAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      putAll(attributes);
    }
    return this;
  }

  /**
   * Copy all attributes in the supplied {@code Map} into this {@code Map},
   * with existing objects of the same name taking precedence (i.e. not getting
   * replaced).
   */
  @Override
  public ConcurrentModel mergeAttributes(@Nullable Map<String, ?> attributes) {
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
  @Override
  public boolean containsAttribute(String attributeName) {
    return containsKey(attributeName);
  }

  @Override
  @Nullable
  public Object getAttribute(String attributeName) {
    return get(attributeName);
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    put(name, value);
  }

  @Override
  public Map<String, Object> asMap() {
    return this;
  }

  @Override
  public Iterator<String> attributeNames() {
    return keySet().iterator();
  }

}
