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

package cn.taketoday.jdbc;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.PropertyAccessorUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/30 20:31
 */
final class PropertyPath {
  static final String emptyPlaceholder = "<not-found>";

  @Nullable
  public final PropertyPath next;

  // @Nullable check first
  public final BeanProperty beanProperty;

  public PropertyPath(Class<?> objectType, String propertyPath) {
    BeanMetadata metadata = BeanMetadata.from(objectType);
    int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
    String name = propertyPath.substring(0, pos);
    this.beanProperty = metadata.obtainBeanProperty(name);

    BeanMetadata nextMetadata = BeanMetadata.from(beanProperty.getType());
    this.next = new PropertyPath(propertyPath.substring(pos + 1), nextMetadata);
  }

  public PropertyPath(String propertyPath, BeanMetadata metadata) {
    int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
    if (pos > -1) {
      // compute next PropertyPath
      String propertyName = propertyPath.substring(0, pos);
      this.beanProperty = metadata.getBeanProperty(propertyName);
      if (beanProperty != null) {
        BeanMetadata nextMetadata = BeanMetadata.from(beanProperty.getType());
        this.next = new PropertyPath(propertyPath.substring(pos + 1), nextMetadata);
      }
      else {
        this.next = null;
      }
    }
    else {
      // terminated (last PropertyPath)
      this.next = null;
      this.beanProperty = metadata.getBeanProperty(propertyPath); // maybe null
    }
  }

  public BeanProperty getNestedBeanProperty() {
    if (next != null) {
      return next.getNestedBeanProperty();
    }
    return beanProperty;
  }

  public Object getNestedObject(Object parent) {
    if (next != null) {
      Object nextParent = getProperty(parent);
      return next.getNestedObject(nextParent);
    }
    return parent;
  }

  public void set(Object obj, Object result) {
    PropertyPath current = this;
    while (current.next != null) {
      obj = getProperty(obj);
      current = current.next;
    }

    // set current object's property
    current.beanProperty.setValue(obj, result);
  }

  private Object getProperty(Object obj) {
    Object property = beanProperty.getValue(obj);
    if (property == null) {
      // nested object maybe null
      property = beanProperty.instantiate();
      beanProperty.setValue(obj, property);
    }
    return property;
  }

  @Override
  public String toString() {
    if (next != null) {
      StringBuilder sb = new StringBuilder();
      if (beanProperty == null) {
        sb.append(emptyPlaceholder);
      }
      else {
        sb.append(beanProperty.getName());
      }
      return sb.append('.').append(next).toString();
    }
    if (beanProperty == null) {
      return emptyPlaceholder;
    }
    return beanProperty.getName();
  }
}
