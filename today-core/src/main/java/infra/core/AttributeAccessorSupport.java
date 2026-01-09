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

package infra.core;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * Support class for {@link AttributeAccessor AttributeAccessors}, providing a
 * base implementation of all methods. To be extended by subclasses.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.1.7 2020-02-22 12:47
 */
public abstract class AttributeAccessorSupport implements AttributeAccessor {

  /** Map with String keys and Object values. */
  protected @Nullable Map<String, Object> attributes;

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    if (value != null) {
      getAttributes().put(name, value);
    }
    else {
      removeAttribute(name);
    }
  }

  @Override
  public void setAttributes(@Nullable Map<String, Object> attributes) {
    if (CollectionUtils.isNotEmpty(attributes)) {
      getAttributes().putAll(attributes);
    }
  }

  @Nullable
  @Override
  public Object getAttribute(final String name) {
    if (attributes == null) {
      return null;
    }
    return attributes.get(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T computeAttribute(String name, Function<String, @Nullable T> computeFunction) {
    Assert.notNull(name, "Name is required");
    Assert.notNull(computeFunction, "Compute function is required");
    if (attributes == null) {
      T value = computeFunction.apply(name);
      if (value == null) {
        throw new IllegalStateException("Compute function must not return null for attribute named '%s'".formatted(name));
      }
      setAttribute(name, value);
      return value;
    }
    else {
      Object value = attributes.computeIfAbsent(name, computeFunction);
      if (value == null) {
        throw new IllegalStateException("Compute function must not return null for attribute named '%s'".formatted(name));
      }
      return (T) value;
    }
  }

  @Nullable
  @Override
  public Object removeAttribute(String name) {
    if (attributes != null) {
      return attributes.remove(name);
    }
    return null;
  }

  @Override
  public boolean hasAttribute(String name) {
    if (attributes != null) {
      return attributes.containsKey(name);
    }
    return false;
  }

  @Override
  public String[] getAttributeNames() {
    if (attributes != null) {
      return StringUtils.toStringArray(attributes.keySet());
    }
    return Constant.EMPTY_STRING_ARRAY;
  }

  @Override
  public Iterable<String> attributeNames() {
    if (attributes != null) {
      return attributes.keySet();
    }
    return Collections.emptyList();
  }

  /**
   * Copy the attributes from the supplied AttributeAccessor to this accessor.
   *
   * @param source the AttributeAccessor to copy from
   */
  public void copyFrom(AttributeAccessor source) {
    Assert.notNull(source, "Source is required");
    Map<String, Object> attributes;
    if (source instanceof AttributeAccessorSupport) {
      attributes = ((AttributeAccessorSupport) source).attributes;
      if (attributes == null) {
        return;
      }
    }
    else {
      attributes = source.getAttributes();
    }

    if (CollectionUtils.isNotEmpty(attributes)) {
      getAttributes().putAll(attributes);
    }
  }

  /**
   * @since 3.0
   */
  @Override
  public void clearAttributes() {
    if (attributes != null) {
      attributes.clear();
    }
  }

  /**
   * Returns {@code true} if this map contains no key-value mappings.
   *
   * @return {@code true} if this map contains no key-value mappings
   * @since 4.0
   */
  @Override
  public boolean hasAttributes() {
    return attributes != null && !attributes.isEmpty();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(attributes);
  }

  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof AttributeAccessorSupport that
            && Objects.equals(attributes, that.attributes)));
  }

  @Override
  public Map<String, Object> getAttributes() {
    Map<String, Object> attributes = this.attributes;
    if (attributes == null) {
      attributes = createAttributes();
      this.attributes = attributes;
    }
    return attributes;
  }

  protected Map<String, Object> createAttributes() {
    return new HashMap<>();
  }

}
