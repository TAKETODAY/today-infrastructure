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

import java.io.Serial;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.core.Conventions;
import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * Implementation of {@link java.util.Map} for use when building model data for use
 * with UI tools. Supports chained calls and generation of model attribute names.
 *
 * <p>This is an implementation class exposed to handler methods by Web MVC, typically via
 * a declaration of the {@link Model} interface. There is no need to
 * build it within user code; a plain {@link ModelMap} or even a just
 * a regular {@link Map} with String keys will be good enough to return a user model.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Conventions#getVariableName
 * @since 4.0 2022/4/8 22:58
 */
public class ModelMap extends LinkedHashMap<String, @Nullable Object> implements Model {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Construct a new, empty {@code ModelMap}.
   */
  public ModelMap() {
  }

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
  @Override
  public ModelMap addAttribute(String attributeName, @Nullable Object attributeValue) {
    Assert.notNull(attributeName, "Model attribute name is required");
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
  @Override
  public ModelMap addAttribute(Object attributeValue) {
    Assert.notNull(attributeValue, "Model object is required");
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
  @Override
  public ModelMap addAllAttributes(@Nullable Map<String, ?> attributes) {
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
  public ModelMap mergeAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      for (Map.Entry<String, ?> entry : attributes.entrySet()) {
        String key = entry.getKey();
        if (!containsKey(key)) {
          put(key, entry.getValue());
        }
      }
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

  /**
   * Return the attribute value for the given name, if any.
   *
   * @param attributeName the name of the model attribute (never {@code null})
   * @return the corresponding attribute value, or {@code null} if none
   */
  @Nullable
  @Override
  public Object getAttribute(String attributeName) {
    return get(attributeName);
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    put(name, value);
  }

  @Override
  public @Nullable Object removeAttribute(String name) {
    return remove(name);
  }

  @Override
  public Map<String, @Nullable Object> asMap() {
    return this;
  }

  @Override
  public String[] getAttributeNames() {
    return StringUtils.toStringArray(keySet());
  }

  @Override
  public Iterable<String> attributeNames() {
    return keySet();
  }

}
