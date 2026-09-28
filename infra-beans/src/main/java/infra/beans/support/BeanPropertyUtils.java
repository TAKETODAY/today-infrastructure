/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.support;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.beans.BeanWrapperImpl;
import infra.beans.SimpleTypeConverter;
import infra.beans.TypeConverter;
import infra.util.Assert;
import infra.util.ObjectUtils;

/**
 * Utilities for copying property values between beans or from a map into a bean.
 * Copying matches properties by name and skips source properties that are not
 * writable on the destination. Population delegates to {@link BeanWrapperImpl}
 * and supports nested property paths.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0.2 2021/5/2 22:14
 */
public abstract class BeanPropertyUtils {

  /**
   * Copy matching property values from a source bean or map to a destination bean.
   * Properties missing from or not writable on the destination are ignored.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   */
  public static void copy(Object source, Object destination) {
    copy(source, destination, (TypeConverter) null);
  }

  /**
   * Copy matching property values from a source bean or map to a destination bean.
   * Properties missing from or not writable on the destination are ignored.
   * A {@link SimpleTypeConverter} is used when {@code converter} is {@code null}.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param converter the converter for source bean property values, or {@code null}
   * to use the default; map values are assigned directly
   */
  public static void copy(Object source, Object destination, @Nullable TypeConverter converter) {
    Assert.notNull(source, "source object is required");
    Assert.notNull(destination, "destination object is required");

    BeanMetadata destinationMetadata = BeanMetadata.forInstance(destination);
    copy(source, destinationMetadata, destination, converter, null);
  }

  /**
   * Copy matching property values, excluding the specified property names.
   * Properties missing from or not writable on the destination are ignored.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param ignoreProperties property names to exclude, or {@code null} for none
   */
  public static void copy(Object source, Object destination, String @Nullable ... ignoreProperties) {
    copy(source, destination, null, ignoreProperties);
  }

  /**
   * Copy matching property values, excluding the specified property names.
   * Properties missing from or not writable on the destination are ignored.
   * A {@link SimpleTypeConverter} is used when {@code converter} is {@code null}.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param converter the converter for source bean property values, or {@code null}
   * to use the default; map values are assigned directly
   * @param ignoreProperties property names to exclude, or {@code null} for none
   */
  public static void copy(Object source, Object destination,
          @Nullable TypeConverter converter, String @Nullable ... ignoreProperties) {
    Assert.notNull(source, "source object is required");
    Assert.notNull(destination, "destination object is required");

    BeanMetadata destinationMetadata = BeanMetadata.forInstance(destination);
    copy(source, destinationMetadata, destination, converter, ignoreProperties);
  }

  /**
   * Create a destination bean and copy matching property values into it.
   * Properties missing from or not writable on the destination are ignored.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @return the populated destination bean
   */
  public static <T> T copy(Object source, Class<T> destination) {
    return copy(source, destination, (TypeConverter) null);
  }

  /**
   * Create a destination bean and copy matching property values into it.
   * Properties missing from or not writable on the destination are ignored.
   * A {@link SimpleTypeConverter} is used when {@code converter} is {@code null}.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @param converter the converter for source bean property values, or {@code null}
   * to use the default; map values are assigned directly
   * @return the populated destination bean
   */
  @SuppressWarnings("unchecked")
  public static <T> T copy(Object source, Class<T> destination, @Nullable TypeConverter converter) {
    Assert.notNull(source, "source object is required");
    Assert.notNull(destination, "destination class is required");

    BeanMetadata destinationMetadata = BeanMetadata.forClass(destination);
    Object destinationInstance = destinationMetadata.newInstance(); // destination
    copy(source, destinationMetadata, destinationInstance, converter, null);
    return (T) destinationInstance;
  }

  /**
   * Create a destination bean and copy matching property values, excluding
   * the specified property names. Unmatched or read-only properties are ignored.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @param ignoreProperties property names to exclude, or {@code null} for none
   * @return the populated destination bean
   */
  public static <T> T copy(Object source, Class<T> destination, String @Nullable ... ignoreProperties) {
    return copy(source, destination, null, ignoreProperties);
  }

  /**
   * Create a destination bean and copy matching property values, excluding
   * the specified property names. Unmatched or read-only properties are ignored.
   * A {@link SimpleTypeConverter} is used when {@code converter} is {@code null}.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @param converter the converter for source bean property values, or {@code null}
   * to use the default; map values are assigned directly
   * @param ignoreProperties property names to exclude, or {@code null} for none
   * @return the populated destination bean
   */
  @SuppressWarnings("unchecked")
  public static <T> T copy(Object source, Class<T> destination,
          @Nullable TypeConverter converter, String @Nullable ... ignoreProperties) {
    Assert.notNull(source, "source object is required");
    Assert.notNull(destination, "destination class is required");

    BeanMetadata destinationMetadata = BeanMetadata.forClass(destination);
    Object destinationInstance = destinationMetadata.newInstance(); // destination
    copy(source, destinationMetadata, destinationInstance, converter, ignoreProperties);
    return (T) destinationInstance;
  }

  /**
   * Copy readable source values into matching writable destination properties.
   * Unknown, read-only, and excluded properties are skipped.
   */
  @SuppressWarnings("unchecked")
  private static void copy(Object source, BeanMetadata destination,
          Object destinationInstance, @Nullable TypeConverter converter, String @Nullable [] ignoreProperties) {
    if (converter == null) {
      converter = new SimpleTypeConverter();
    }
    if (ObjectUtils.isNotEmpty(ignoreProperties)) {
      Set<String> ignorePropertiesSet = Set.of(ignoreProperties);
      if (source instanceof Map) {
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) source).entrySet()) {
          String propertyName = entry.getKey();
          if (!ignorePropertiesSet.contains(propertyName)) {
            BeanProperty beanProperty = destination.getProperty(propertyName);
            if (beanProperty != null && beanProperty.isWriteable()) {
              beanProperty.setValue(destinationInstance, entry.getValue(), converter);
            }
          }
        }
      }
      else {
        BeanMetadata sourceMetadata = BeanMetadata.forInstance(source);
        for (BeanProperty property : sourceMetadata.getPropertyList()) {
          if (property.isReadable()) {
            String propertyName = property.getName();
            if (!ignorePropertiesSet.contains(propertyName)) {
              BeanProperty beanProperty = destination.getProperty(propertyName);
              if (beanProperty != null && beanProperty.isWriteable()) {
                beanProperty.setValue(destinationInstance, property.getValue(source), converter);
              }
            }
          }
        }
      }
    }
    else {
      if (source instanceof Map) {
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) source).entrySet()) {
          String propertyName = entry.getKey();
          BeanProperty beanProperty = destination.getProperty(propertyName);
          if (beanProperty != null && beanProperty.isWriteable()) {
            beanProperty.setValue(destinationInstance, entry.getValue(), converter);
          }
        }
      }
      else {
        BeanMetadata sourceMetadata = BeanMetadata.forInstance(source);
        for (BeanProperty property : sourceMetadata.getPropertyList()) {
          if (property.isReadable()) {
            String propertyName = property.getName();
            BeanProperty beanProperty = destination.getProperty(propertyName);
            if (beanProperty != null && beanProperty.isWriteable()) {
              beanProperty.setValue(destinationInstance, property.getValue(source), converter);
            }
          }
        }
      }
    }

  }

  //

  /**
   * Populate a bean from property names and values, ignoring unknown properties.
   * Nested property paths are supported and intermediate objects may be created.
   *
   * @param bean the bean to populate
   * @param properties property names and values to apply
   * @see #populate(Object, Map, boolean)
   */
  public static void populate(Object bean, Map<String, Object> properties) {
    populate(bean, properties, true);
  }

  /**
   * Populate a bean from property names and values using a {@link BeanWrapperImpl}.
   * Nested property paths are supported and intermediate objects may be created.
   * Unknown or non-writable properties are ignored when {@code ignoreUnknown}
   * is {@code true}; missing intermediate values in nested paths are ignored.
   * Conversion and property access errors may still be reported.
   *
   * @param bean the bean to populate
   * @param properties property names and values to apply
   * @param ignoreUnknown whether to ignore unknown or non-writable properties
   * @see BeanWrapperImpl
   */
  public static void populate(Object bean, Map<String, Object> properties, boolean ignoreUnknown) {
    Assert.notNull(bean, "target bean is required");
    Assert.notNull(properties, "properties is required");
    BeanWrapperImpl beanWrapper = new BeanWrapperImpl(bean);
    beanWrapper.setAutoGrowNestedPaths(true);

    beanWrapper.setPropertyValues(properties, ignoreUnknown, true);
  }

}
