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

package cn.taketoday.beans;

import java.io.Serial;
import java.io.Serializable;

import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.lang.Nullable;

/**
 * Extension of {@link AttributeAccessorSupport},
 * holding attributes as {@link BeanMetadataAttribute} objects in order
 * to keep track of the definition source.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 12:30
 */
public class BeanMetadataAttributeAccessor extends AttributeAccessorSupport implements BeanMetadataElement, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private Object source;

  /**
   * Set the configuration source {@code Object} for this metadata element.
   * <p>The exact type of the object will depend on the configuration mechanism used.
   */
  public void setSource(@Nullable Object source) {
    this.source = source;
  }

  @Override
  @Nullable
  public Object getSource() {
    return this.source;
  }

  /**
   * Add the given BeanMetadataAttribute to this accessor's set of attributes.
   *
   * @param attribute the BeanMetadataAttribute object to register
   */
  public void addMetadataAttribute(BeanMetadataAttribute attribute) {
    super.setAttribute(attribute.getName(), attribute);
  }

  /**
   * Look up the given BeanMetadataAttribute in this accessor's set of attributes.
   *
   * @param name the name of the attribute
   * @return the corresponding BeanMetadataAttribute object,
   * or {@code null} if no such attribute defined
   */
  @Nullable
  public BeanMetadataAttribute getMetadataAttribute(String name) {
    return (BeanMetadataAttribute) super.getAttribute(name);
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    super.setAttribute(name, new BeanMetadataAttribute(name, value));
  }

  @Override
  @Nullable
  public Object getAttribute(String name) {
    if (super.getAttribute(name) instanceof BeanMetadataAttribute attribute) {
      return attribute.getValue();
    }
    return null;
  }

  @Override
  @Nullable
  public Object removeAttribute(String name) {
    if (super.removeAttribute(name) instanceof BeanMetadataAttribute attribute) {
      return attribute.getValue();
    }
    return null;
  }

}
