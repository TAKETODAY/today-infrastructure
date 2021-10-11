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
package cn.taketoday.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Support class for {@link AttributeAccessor AttributeAccessors}, providing a
 * base implementation of all methods. To be extended by subclasses.
 *
 * <p>
 * {@link Serializable} if subclasses and all attribute values are
 * {@link Serializable}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author TODAY <br>
 * 2020-02-22 12:47
 * @since 2.1.7
 */
public abstract class AttributeAccessorSupport implements AttributeAccessor {

  /** Map with String keys and Object values. */
  protected HashMap<String, Object> attributes;

  @Override
  public void setAttribute(final String name, final Object value) {
    if (value != null) {
      getAttributes().put(name, value);
    }
    else {
      removeAttribute(name);
    }
  }

  @Override
  public Object getAttribute(final String name) {
    if (attributes == null) {
      return null;
    }
    return attributes.get(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T computeAttribute(final String name, final Function<String, T> computeFunction) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(computeFunction, "Compute function must not be null");
    Object value = getAttributes().computeIfAbsent(name, computeFunction);
    Assert.state(value != null,
                 () -> String.format("Compute function must not return null for attribute named '%s'", name));
    return (T) value;
  }

  @Override
  public Object removeAttribute(final String name) {
    if (attributes != null) {
      return attributes.remove(name);
    }
    return null;
  }

  @Override
  public boolean hasAttribute(final String name) {
    if (attributes != null) {
      return attributes.containsKey(name);
    }
    return false;
  }

  @Override
  public String[] attributeNames() {
    if (attributes != null) {
      return StringUtils.toStringArray(attributes.keySet());
    }
    return Constant.EMPTY_STRING_ARRAY;
  }

  /**
   * Copy the attributes from the supplied AttributeAccessor to this accessor.
   *
   * @param source
   *         the AttributeAccessor to copy from
   */
  public void copyAttributesFrom(final AttributeAccessor source) {
    Assert.notNull(source, "Source must not be null");

    final Map<String, Object> attributes = source.getAttributes();
    if (CollectionUtils.isNotEmpty(attributes)) {
      for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
        setAttribute(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * @since 3.0
   */
  public void clear() {
    if (attributes != null) {
      attributes.clear();
    }
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(attributes);
  }

  @Override
  public boolean equals(final Object other) {
    return (this == other
            || (other instanceof AttributeAccessorSupport &&
            Objects.equals(attributes, (((AttributeAccessorSupport) other).attributes))));
  }

  @Override
  public Map<String, Object> getAttributes() {
    if (attributes == null) {
      return this.attributes = createAttributes();
    }
    return attributes;
  }

  protected HashMap<String, Object> createAttributes() {
    return new HashMap<>();
  }

}
