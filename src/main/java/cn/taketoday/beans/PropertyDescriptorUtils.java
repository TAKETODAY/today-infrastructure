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

package cn.taketoday.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Enumeration;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Common delegate methods for Spring's internal {@link PropertyDescriptor} implementations.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/23 11:31
 */
abstract class PropertyDescriptorUtils {

  /**
   * See {@link java.beans.FeatureDescriptor}.
   */
  public static void copyNonMethodProperties(PropertyDescriptor source, PropertyDescriptor target) {
    target.setExpert(source.isExpert());
    target.setHidden(source.isHidden());
    target.setPreferred(source.isPreferred());
    target.setName(source.getName());
    target.setShortDescription(source.getShortDescription());
    target.setDisplayName(source.getDisplayName());

    // Copy all attributes (emulating behavior of private FeatureDescriptor#addTable)
    Enumeration<String> keys = source.attributeNames();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      target.setValue(key, source.getValue(key));
    }

    // See java.beans.PropertyDescriptor#PropertyDescriptor(PropertyDescriptor)
    target.setPropertyEditorClass(source.getPropertyEditorClass());
    target.setBound(source.isBound());
    target.setConstrained(source.isConstrained());
  }

  /**
   * See {@link java.beans.PropertyDescriptor#findPropertyType}.
   */
  @Nullable
  public static Class<?> findPropertyType(@Nullable Method readMethod, @Nullable Method writeMethod)
          throws IntrospectionException {

    Class<?> propertyType = null;

    if (readMethod != null) {
      if (readMethod.getParameterCount() != 0) {
        throw new IntrospectionException("Bad read method arg count: " + readMethod);
      }
      propertyType = readMethod.getReturnType();
      if (propertyType == Void.TYPE) {
        throw new IntrospectionException("Read method returns void: " + readMethod);
      }
    }

    if (writeMethod != null) {
      Class<?>[] params = writeMethod.getParameterTypes();
      if (params.length != 1) {
        throw new IntrospectionException("Bad write method arg count: " + writeMethod);
      }
      if (propertyType != null) {
        if (propertyType.isAssignableFrom(params[0])) {
          // Write method's property type potentially more specific
          propertyType = params[0];
        }
        else if (params[0].isAssignableFrom(propertyType)) {
          // Proceed with read method's property type
        }
        else {
          throw new IntrospectionException(
                  "Type mismatch between read and write methods: " + readMethod + " - " + writeMethod);
        }
      }
      else {
        propertyType = params[0];
      }
    }

    return propertyType;
  }

  /**
   * See {@link java.beans.IndexedPropertyDescriptor#findIndexedPropertyType}.
   */
  @Nullable
  public static Class<?> findIndexedPropertyType(
          String name, @Nullable Class<?> propertyType,
          @Nullable Method indexedReadMethod, @Nullable Method indexedWriteMethod) throws IntrospectionException {

    Class<?> indexedPropertyType = null;

    if (indexedReadMethod != null) {
      Class<?>[] params = indexedReadMethod.getParameterTypes();
      if (params.length != 1) {
        throw new IntrospectionException("Bad indexed read method arg count: " + indexedReadMethod);
      }
      if (params[0] != Integer.TYPE) {
        throw new IntrospectionException("Non int index to indexed read method: " + indexedReadMethod);
      }
      indexedPropertyType = indexedReadMethod.getReturnType();
      if (indexedPropertyType == Void.TYPE) {
        throw new IntrospectionException("Indexed read method returns void: " + indexedReadMethod);
      }
    }

    if (indexedWriteMethod != null) {
      Class<?>[] params = indexedWriteMethod.getParameterTypes();
      if (params.length != 2) {
        throw new IntrospectionException("Bad indexed write method arg count: " + indexedWriteMethod);
      }
      if (params[0] != Integer.TYPE) {
        throw new IntrospectionException("Non int index to indexed write method: " + indexedWriteMethod);
      }
      if (indexedPropertyType != null) {
        if (indexedPropertyType.isAssignableFrom(params[1])) {
          // Write method's property type potentially more specific
          indexedPropertyType = params[1];
        }
        else if (params[1].isAssignableFrom(indexedPropertyType)) {
          // Proceed with read method's property type
        }
        else {
          throw new IntrospectionException("Type mismatch between indexed read and write methods: " +
                  indexedReadMethod + " - " + indexedWriteMethod);
        }
      }
      else {
        indexedPropertyType = params[1];
      }
    }

    if (propertyType != null && (!propertyType.isArray() ||
            propertyType.getComponentType() != indexedPropertyType)) {
      throw new IntrospectionException("Type mismatch between indexed and non-indexed methods: " +
              indexedReadMethod + " - " + indexedWriteMethod);
    }

    return indexedPropertyType;
  }

  /**
   * Compare the given {@code PropertyDescriptors} and return {@code true} if
   * they are equivalent, i.e. their read method, write method, property type,
   * property editor and flags are equivalent.
   *
   * @see java.beans.PropertyDescriptor#equals(Object)
   */
  public static boolean equals(PropertyDescriptor pd, PropertyDescriptor otherPd) {
    return ObjectUtils.nullSafeEquals(pd.getReadMethod(), otherPd.getReadMethod())
            && ObjectUtils.nullSafeEquals(pd.getWriteMethod(), otherPd.getWriteMethod())
            && ObjectUtils.nullSafeEquals(pd.getPropertyType(), otherPd.getPropertyType())
            && ObjectUtils.nullSafeEquals(pd.getPropertyEditorClass(), otherPd.getPropertyEditorClass())
            && pd.isBound() == otherPd.isBound() && pd.isConstrained() == otherPd.isConstrained();
  }

}

