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

import java.util.Map;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Nullable;

/**
 * Common interface for classes that can access named properties
 * (such as bean properties of an object or fields in an object).
 *
 * <p>Serves as base interface for {@link BeanWrapper}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanWrapper
 * @see PropertyAccessorFactory#forBeanPropertyAccess
 * @see PropertyAccessorFactory#forDirectFieldAccess
 * @since 4.0 2022/2/17 17:37
 */
public interface PropertyAccessor {

  /**
   * Path separator for nested properties.
   * Follows normal Java conventions: getFoo().getBar() would be "foo.bar".
   */
  String NESTED_PROPERTY_SEPARATOR = ".";

  /**
   * Path separator for nested properties.
   * Follows normal Java conventions: getFoo().getBar() would be "foo.bar".
   */
  char NESTED_PROPERTY_SEPARATOR_CHAR = '.';

  /**
   * Marker that indicates the start of a property key for an
   * indexed or mapped property like "person.addresses[0]".
   */
  String PROPERTY_KEY_PREFIX = "[";

  /**
   * Marker that indicates the start of a property key for an
   * indexed or mapped property like "person.addresses[0]".
   */
  char PROPERTY_KEY_PREFIX_CHAR = '[';

  /**
   * Marker that indicates the end of a property key for an
   * indexed or mapped property like "person.addresses[0]".
   */
  String PROPERTY_KEY_SUFFIX = "]";

  /**
   * Marker that indicates the end of a property key for an
   * indexed or mapped property like "person.addresses[0]".
   */
  char PROPERTY_KEY_SUFFIX_CHAR = ']';

  /**
   * Determine whether the specified property is readable.
   * <p>Returns {@code false} if the property doesn't exist.
   *
   * @param propertyName the property to check
   * (may be a nested path and/or an indexed/mapped property)
   * @return whether the property is readable
   */
  boolean isReadableProperty(String propertyName);

  /**
   * Determine whether the specified property is writable.
   * <p>Returns {@code false} if the property doesn't exist.
   *
   * @param propertyName the property to check
   * (may be a nested path and/or an indexed/mapped property)
   * @return whether the property is writable
   */
  boolean isWritableProperty(String propertyName);

  /**
   * Determine the property type for the specified property,
   * either checking the property descriptor or checking the value
   * in case of an indexed or mapped element.
   *
   * @param propertyName the property to check
   * (may be a nested path and/or an indexed/mapped property)
   * @return the property type for the particular property,
   * or {@code null} if not determinable
   * @throws PropertyAccessException if the property was valid but the
   * accessor method failed
   */
  @Nullable
  Class<?> getPropertyType(String propertyName) throws BeansException;

  /**
   * Return a type descriptor for the specified property:
   * preferably from the read method, falling back to the write method.
   *
   * @param propertyName the property to check
   * (may be a nested path and/or an indexed/mapped property)
   * @return the property type for the particular property,
   * or {@code null} if not determinable
   * @throws PropertyAccessException if the property was valid but the
   * accessor method failed
   */
  @Nullable
  TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException;

  /**
   * Get the current value of the specified property.
   *
   * @param propertyName the name of the property to get the value of
   * (may be a nested path and/or an indexed/mapped property)
   * @return the value of the property
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't readable
   * @throws PropertyAccessException if the property was valid but the
   * accessor method failed
   */
  @Nullable
  Object getPropertyValue(String propertyName) throws BeansException;

  /**
   * Set the specified value as current property value.
   *
   * @param propertyName the name of the property to set the value of
   * (may be a nested path and/or an indexed/mapped property)
   * @param value the new value
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't writable
   * @throws PropertyAccessException if the property was valid but the
   * accessor method failed or a type mismatch occurred
   */
  void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException;

  /**
   * Set the specified value as current property value.
   *
   * @param pv an object containing the new property value
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't writable
   * @throws PropertyAccessException if the property was valid but the
   * accessor method failed or a type mismatch occurred
   */
  void setPropertyValue(PropertyValue pv) throws BeansException;

  /**
   * Perform a batch update from a Map.
   * <p>Bulk updates from PropertyValues are more powerful: This method is
   * provided for convenience. Behavior will be identical to that of
   * the {@link #setPropertyValues(PropertyValues)} method.
   *
   * @param map a Map to take properties from. Contains property value objects,
   * keyed by property name
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't writable
   * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
   * occurred for specific properties during the batch update. This exception bundles
   * all individual PropertyAccessExceptions. All other properties will have been
   * successfully updated.
   */
  void setPropertyValues(Map<?, ?> map) throws BeansException;

  /**
   * Perform a batch update from a Map.
   * <p>Bulk updates from PropertyValues are more powerful: This method is
   * provided for convenience. Behavior will be identical to that of
   * the {@link #setPropertyValues(PropertyValues)} method.
   *
   * @param map a Map to take properties from. Contains property value objects,
   * keyed by property name
   * @param ignoreUnknown should we ignore unknown properties (not found in the bean)
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't writable
   * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
   * occurred for specific properties during the batch update. This exception bundles
   * all individual PropertyAccessExceptions. All other properties will have been
   * successfully updated.
   */
  void setPropertyValues(Map<?, ?> map, boolean ignoreUnknown)
          throws BeansException;

  /**
   * Perform a batch update from a Map.
   * <p>Note that performing a batch update differs from performing a single update,
   * in that an implementation of this class will continue to update properties
   * if a <b>recoverable</b> error (such as a type mismatch, but <b>not</b> an
   * invalid field name or the like) is encountered, throwing a
   * {@link PropertyBatchUpdateException} containing all the individual errors.
   * This exception can be examined later to see all binding errors.
   * Properties that were successfully updated remain changed.
   *
   * @param map a Map to take properties from. Contains property value objects,
   * keyed by property name
   * @param ignoreUnknown should we ignore unknown properties (not found in the bean)
   * @param ignoreInvalid should we ignore invalid properties (found but not accessible)
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't writable
   * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
   * occurred for specific properties during the batch update. This exception bundles
   * all individual PropertyAccessExceptions. All other properties will have been
   * successfully updated.
   */
  void setPropertyValues(Map<?, ?> map, boolean ignoreUnknown, boolean ignoreInvalid)
          throws BeansException;

  /**
   * The preferred way to perform a batch update.
   * <p>Note that performing a batch update differs from performing a single update,
   * in that an implementation of this class will continue to update properties
   * if a <b>recoverable</b> error (such as a type mismatch, but <b>not</b> an
   * invalid field name or the like) is encountered, throwing a
   * {@link PropertyBatchUpdateException} containing all the individual errors.
   * This exception can be examined later to see all binding errors.
   * Properties that were successfully updated remain changed.
   * <p>Does not allow unknown fields or invalid fields.
   *
   * @param pvs a PropertyValues to set on the target object
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't writable
   * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
   * occurred for specific properties during the batch update. This exception bundles
   * all individual PropertyAccessExceptions. All other properties will have been
   * successfully updated.
   * @see #setPropertyValues(PropertyValues, boolean, boolean)
   */
  void setPropertyValues(PropertyValues pvs) throws BeansException;

  /**
   * Perform a batch update with more control over behavior.
   * <p>Note that performing a batch update differs from performing a single update,
   * in that an implementation of this class will continue to update properties
   * if a <b>recoverable</b> error (such as a type mismatch, but <b>not</b> an
   * invalid field name or the like) is encountered, throwing a
   * {@link PropertyBatchUpdateException} containing all the individual errors.
   * This exception can be examined later to see all binding errors.
   * Properties that were successfully updated remain changed.
   *
   * @param pvs a PropertyValues to set on the target object
   * @param ignoreUnknown should we ignore unknown properties (not found in the bean)
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't writable
   * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
   * occurred for specific properties during the batch update. This exception bundles
   * all individual PropertyAccessExceptions. All other properties will have been
   * successfully updated.
   * @see #setPropertyValues(PropertyValues, boolean, boolean)
   */
  void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown)
          throws BeansException;

  /**
   * Perform a batch update with full control over behavior.
   * <p>Note that performing a batch update differs from performing a single update,
   * in that an implementation of this class will continue to update properties
   * if a <b>recoverable</b> error (such as a type mismatch, but <b>not</b> an
   * invalid field name or the like) is encountered, throwing a
   * {@link PropertyBatchUpdateException} containing all the individual errors.
   * This exception can be examined later to see all binding errors.
   * Properties that were successfully updated remain changed.
   *
   * @param pvs a PropertyValues to set on the target object
   * @param ignoreUnknown should we ignore unknown properties (not found in the bean)
   * @param ignoreInvalid should we ignore invalid properties (found but not accessible)
   * @throws InvalidPropertyException if there is no such property or
   * if the property isn't writable
   * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
   * occurred for specific properties during the batch update. This exception bundles
   * all individual PropertyAccessExceptions. All other properties will have been
   * successfully updated.
   */
  void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
          throws BeansException;

}

