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

package cn.taketoday.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

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
  @Nullable
  protected Map<String, Object> attributes;

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
  public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
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
  public Iterator<String> attributeNames() {
    if (attributes != null) {
      return attributes.keySet().iterator();
    }
    return Collections.emptyIterator();
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
    return ObjectUtils.nullSafeHashCode(attributes);
  }

  @Override
  public boolean equals(Object other) {
    return (this == other
            || (other instanceof AttributeAccessorSupport &&
            Objects.equals(attributes, (((AttributeAccessorSupport) other).attributes))));
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
