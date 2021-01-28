/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.factory;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.utils.ConvertUtils;

/**
 * @author TODAY 2021/1/27 22:35
 * @since 3.0
 */
public class BeanPropertyAccessor {

  private final Object object;
  private final BeanMetadata metadata;

  public BeanPropertyAccessor(Class<?> beanClass) {
    this.metadata = new BeanMetadata(beanClass);
    this.object = metadata.newInstance();
  }

  public BeanPropertyAccessor(Object object) {
    this(new BeanMetadata(object.getClass()), object);
  }

  public BeanPropertyAccessor(BeanMetadata metadata, Object object) {
    this.object = object;
    this.metadata = metadata;
  }

  /**
   * Get property object with given requiredType
   *
   * @param propertyPath
   *         Property path
   * @param requiredType
   *         Required type
   *
   * @return property object with given requiredType
   *
   * @throws ArrayIndexOutOfBoundsException
   *         Thrown to indicate that an array has been accessed with an
   *         illegal index. The index is either negative or greater than or
   *         equal to the size of the array.
   * @throws UnsupportedOperationException
   *         Unsupported operation
   * @throws NoSuchPropertyException
   *         If there is not a property
   * @throws IndexOutOfBoundsException
   *         if the index is out of list range
   *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
   * @see #getProperty(String)
   */
  public <T> T getProperty(final String propertyPath, final Class<T> requiredType) {
    return ConvertUtils.convert(requiredType, getProperty(propertyPath));
  }

  /**
   * Get property object
   *
   * @param propertyPath
   *         Property path
   *
   * @return property object
   *
   * @throws ArrayIndexOutOfBoundsException
   *         Thrown to indicate that an array has been accessed with an
   *         illegal index. The index is either negative or greater than or
   *         equal to the size of the array.
   * @throws UnsupportedOperationException
   *         Unsupported operation
   * @throws NoSuchPropertyException
   *         If there is not a property
   * @throws IndexOutOfBoundsException
   *         if the index is out of list range (<tt>index &lt; 0 || index &gt;= size()</tt>)
   */
  public Object getProperty(final String propertyPath) {
    return getProperty(object, metadata, propertyPath);
  }

  /**
   * Get property object
   *
   * @param propertyPath
   *         Property path
   *
   * @return property object
   *
   * @throws ArrayIndexOutOfBoundsException
   *         Thrown to indicate that an array has been accessed with an
   *         illegal index. The index is either negative or greater than or
   *         equal to the size of the array.
   * @throws UnsupportedOperationException
   *         Unsupported operation
   * @throws NoSuchPropertyException
   *         If there is not a property
   * @throws IndexOutOfBoundsException
   *         if the index is out of list range (<tt>index &lt; 0 || index &gt;= size()</tt>)
   */
  public static Object getProperty(final Object root, final String propertyPath) {
    return getProperty(root, BeanMetadata.ofObject(root), propertyPath);
  }

  /**
   * Get property object
   *
   * @param propertyPath
   *         Property path
   *
   * @return property object
   *
   * @throws ArrayIndexOutOfBoundsException
   *         Thrown to indicate that an array has been accessed with an
   *         illegal index. The index is either negative or greater than or
   *         equal to the size of the array.
   * @throws UnsupportedOperationException
   *         Unsupported operation
   * @throws NoSuchPropertyException
   *         If there is not a property
   * @throws IndexOutOfBoundsException
   *         if the index is out of list range (<tt>index &lt; 0 || index &gt;= size()</tt>)
   */
  public static Object getProperty(final Object root, final BeanMetadata metadata, final String propertyPath) {
    int signIndex = propertyPath.indexOf('.');

    final BeanProperty beanProperty;
    if (signIndex != -1) {
      final String property = propertyPath.substring(0, signIndex);

      beanProperty = metadata.getBeanProperty(property);
      String newPath = propertyPath.substring(signIndex + 1);
      Object propertyValue = beanProperty.getValue(root);

      if (propertyValue == null) {
        // 上一级为空,下一级自然为空
        return null;
      }

      BeanMetadata subMetadata = BeanMetadata.ofClass(beanProperty.getType());
      return getProperty(propertyValue, subMetadata, newPath);
    }

    signIndex = propertyPath.indexOf('['); // array,list: [0]; map: [key]
    if (signIndex != -1) {
      // exist
      final String property = propertyPath.substring(0, signIndex);
      beanProperty = metadata.getBeanProperty(property);
    }
    else {
      beanProperty = metadata.getBeanProperty(propertyPath);
    }
    if (beanProperty == null) {
      throw noSuchProperty(propertyPath);
    }

    final Object value = beanProperty.getValue(root);
    if (value == null || signIndex == -1) {
      return value;
    }

    final int endIndex = propertyPath.indexOf(']');
    if (endIndex == -1 || signIndex + 1 == endIndex) {
      // key is illegal
      throw unsupported(propertyPath, value, null);
    }
    // key
    final String key = propertyPath.substring(signIndex + 1, endIndex);
    final Class<?> type = beanProperty.getType();

    if (Map.class.isAssignableFrom(type)) {
      final Map map = (Map) value;
      return map.get(key);
    }
    else if (type.isArray()) {
      try {
        final int arrayIndex = Integer.parseInt(key);
        final int length = Array.getLength(value);
        if (arrayIndex >= length) {
          throw new ArrayIndexOutOfBoundsException(length);
        }
        return Array.get(value, arrayIndex);
      }
      catch (NumberFormatException e) {
        throw unsupported(propertyPath, value, e);
      }
    }
    else if (List.class.isAssignableFrom(type)) {
      final List list = (List) value;
      try {
        return list.get(Integer.parseInt(key));
      }
      catch (NumberFormatException e) {
        throw unsupported(propertyPath, value, e);
      }
    }
    else {
      throw unsupported(propertyPath, value, null);
    }
  }

  static UnsupportedOperationException unsupported(String propertyPath, Object value, Exception e) {
    return new UnsupportedOperationException("Unsupported Operator: " + propertyPath + ", value: " + value, e);
  }

  /**
   * Set value to object's property
   *
   * @param propertyPath
   *         Property path to set
   * @param value
   *         Property value
   */
  public void setProperty(final String propertyPath, final Object value) {
    setProperty(object, metadata, propertyPath, value);
  }

  /**
   * Set value to object's property
   *
   * @param root
   *         Root object that apply to
   * @param propertyPath
   *         Property path to set
   * @param value
   *         Property value
   */
  public static void setProperty(final Object root, final String propertyPath, final Object value) {
    setProperty(root, BeanMetadata.ofObject(root), propertyPath, value);
  }

  /**
   * Set value to object's property
   *
   * @param root
   *         Root object that apply to
   * @param metadata
   *         {@link BeanMetadata}
   * @param propertyPath
   *         Property path to set
   * @param value
   *         Property value
   */
  public static void setProperty(final Object root, final BeanMetadata metadata, final String propertyPath, final Object value) {
    int index = propertyPath.indexOf('.');

    final BeanProperty beanProperty;
    if (index > 0) {
      final String property = propertyPath.substring(0, index);
      beanProperty = metadata.getBeanProperty(property);
      final Object subValue = getSubValue(root, beanProperty);

      BeanMetadata subMetadata = BeanMetadata.ofClass(beanProperty.getType());
//      BeanPropertyAccessor subPojo = new BeanPropertyAccessor(subMetadata, subValue);
      String newPath = propertyPath.substring(index + 1);
      setProperty(subValue, subMetadata, newPath, value);
    }
    else {
      beanProperty = metadata.getBeanProperty(propertyPath);
      if (beanProperty == null) {
        throw noSuchProperty(propertyPath);
      }
      beanProperty.setValue(root, value);
    }
  }

  static NoSuchPropertyException noSuchProperty(final String propertyPath) {
    return new NoSuchPropertyException("No such property: " + propertyPath);
  }

  protected static Object getSubValue(final Object object, final BeanProperty beanProperty) {
    // check if it has value
    Object subValue = beanProperty.getValue(object);
    if (subValue == null) {
      // set new value
      subValue = beanProperty.newInstance();
      beanProperty.setValue(object, subValue);
    }
    return subValue;
  }

  public Object getObject() {
    return this.object;
  }

  // static

  public static BeanPropertyAccessor ofObject(Object object) {
    return new BeanPropertyAccessor(object);
  }

  public static BeanPropertyAccessor ofClass(Class<?> beanClass) {
    return new BeanPropertyAccessor(beanClass);
  }

  public static BeanPropertyAccessor of(BeanMetadata metadata, Object object) {
    return new BeanPropertyAccessor(metadata, object);
  }
}
