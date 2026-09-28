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
   * @param converter the converter for source property values, or {@code null}
   * to use the default
   */
  public static void copy(Object source, Object destination, @Nullable TypeConverter converter) {
    copy(source, destination, converter, BeanPropertyCopyStrategy.always());
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
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param ignoreProperties property names to exclude
   */
  public static void copy(Object source, Object destination, @Nullable Set<String> ignoreProperties) {
    copy(source, destination, null, ignoreProperties);
  }

  /**
   * Copy matching property values, excluding the specified property names.
   * Properties missing from or not writable on the destination are ignored.
   * A {@link SimpleTypeConverter} is used when {@code converter} is {@code null}.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param converter the converter for source property values, or {@code null}
   * to use the default
   * @param ignoreProperties property names to exclude, or {@code null} for none
   */
  public static void copy(Object source, Object destination, @Nullable TypeConverter converter, String @Nullable ... ignoreProperties) {
    copy(source, destination, converter, toPropertySet(ignoreProperties));
  }

  /**
   * Copy matching property values, excluding the specified property names.
   * Properties missing from or not writable on the destination are ignored.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param converter the converter for source property values, or {@code null}
   * to use a {@link SimpleTypeConverter}
   * @param ignoreProperties property names to exclude
   */
  public static void copy(Object source, Object destination, @Nullable TypeConverter converter, @Nullable Set<String> ignoreProperties) {
    copy(source, destination, converter, BeanPropertyCopyStrategy.ignoreProperties(ignoreProperties));
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
   * @param converter the converter for source property values, or {@code null}
   * to use the default
   * @return the populated destination bean
   */
  public static <T> T copy(Object source, Class<T> destination, @Nullable TypeConverter converter) {
    return copy(source, destination, converter, BeanPropertyCopyStrategy.always());
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
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @param ignoreProperties property names to exclude
   * @return the populated destination bean
   */
  public static <T> T copy(Object source, Class<T> destination, @Nullable Set<String> ignoreProperties) {
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
   * @param converter the converter for source property values, or {@code null}
   * to use the default
   * @param ignoreProperties property names to exclude, or {@code null} for none
   * @return the populated destination bean
   */
  public static <T> T copy(Object source, Class<T> destination, @Nullable TypeConverter converter, String @Nullable ... ignoreProperties) {
    return copy(source, destination, converter, toPropertySet(ignoreProperties));
  }

  /**
   * Create a destination bean and copy matching property values, excluding
   * the specified property names. Unmatched or read-only properties are ignored.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @param converter the converter for source property values, or {@code null}
   * to use a {@link SimpleTypeConverter}
   * @param ignoreProperties property names to exclude
   * @return the populated destination bean
   */
  public static <T> T copy(Object source, Class<T> destination, @Nullable TypeConverter converter, @Nullable Set<String> ignoreProperties) {
    return copy(source, destination, converter, BeanPropertyCopyStrategy.ignoreProperties(ignoreProperties));
  }

  /**
   * Copy matching non-null property values from a source bean or map into a destination bean.
   * Null source values leave the destination unchanged; missing and read-only
   * destination properties are ignored.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   */
  public static void copyNonNull(Object source, Object destination) {
    copyNonNull(source, destination, (TypeConverter) null);
  }

  /**
   * Copy matching non-null property values from a source bean or map into a destination bean.
   * Null source values leave the destination unchanged.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param converter the converter for source property values, or {@code null}
   * to use a {@link SimpleTypeConverter}
   */
  public static void copyNonNull(Object source, Object destination, @Nullable TypeConverter converter) {
    copyNonNull(source, destination, converter, (String[]) null);
  }

  /**
   * Copy matching non-null property values, excluding the specified property names.
   * Null source values and excluded properties leave the destination unchanged.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param ignoreProperties property names to exclude, or {@code null} for none
   */
  public static void copyNonNull(Object source, Object destination, String @Nullable ... ignoreProperties) {
    copyNonNull(source, destination, null, ignoreProperties);
  }

  /**
   * Copy matching non-null property values, excluding the specified property names.
   * Null source values and excluded properties leave the destination unchanged.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param ignoreProperties property names to exclude
   */
  public static void copyNonNull(Object source, Object destination, @Nullable Set<String> ignoreProperties) {
    copyNonNull(source, destination, null, ignoreProperties);
  }

  /**
   * Copy matching non-null property values, excluding the specified property names.
   * Null source values and excluded properties leave the destination unchanged.
   * Missing and read-only destination properties are ignored.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param converter the converter for source property values, or {@code null}
   * to use a {@link SimpleTypeConverter}
   * @param ignoreProperties property names to exclude, or {@code null} for none
   */
  public static void copyNonNull(Object source, Object destination, @Nullable TypeConverter converter, String @Nullable ... ignoreProperties) {
    copyNonNull(source, destination, converter, toPropertySet(ignoreProperties));
  }

  /**
   * Copy matching non-null property values, excluding the specified property names.
   * Null source values and excluded properties leave the destination unchanged.
   * Missing and read-only destination properties are ignored.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param converter the converter for source property values, or {@code null}
   * to use a {@link SimpleTypeConverter}
   * @param ignoreProperties property names to exclude
   */
  public static void copyNonNull(Object source, Object destination, @Nullable TypeConverter converter, @Nullable Set<String> ignoreProperties) {
    copy(source, destination, converter,
            BeanPropertyCopyStrategy.ignoreProperties(ignoreProperties).and(BeanPropertyCopyStrategy.nonNull()));
  }

  /**
   * Create a destination bean and copy matching non-null property values into it.
   * Null source values leave the new bean's initial values unchanged.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @return the populated destination bean
   */
  public static <T> T copyNonNull(Object source, Class<T> destination) {
    return copyNonNull(source, destination, (TypeConverter) null);
  }

  /**
   * Create a destination bean and copy matching non-null property values into it.
   * Null source values leave the new bean's initial values unchanged.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @param converter the converter for source property values, or {@code null}
   * to use a {@link SimpleTypeConverter}
   * @return the populated destination bean
   */
  public static <T> T copyNonNull(Object source, Class<T> destination, @Nullable TypeConverter converter) {
    return copy(source, destination, converter, BeanPropertyCopyStrategy.nonNull());
  }

  /**
   * Create a destination bean and copy matching non-null property values,
   * excluding the specified property names.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @param ignoreProperties property names to exclude
   * @return the populated destination bean
   */
  public static <T> T copyNonNull(Object source, Class<T> destination, @Nullable Set<String> ignoreProperties) {
    return copyNonNull(source, destination, null, ignoreProperties);
  }

  /**
   * Create a destination bean and copy matching non-null property values,
   * excluding the specified property names.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @param converter the converter for source property values, or {@code null}
   * to use a {@link SimpleTypeConverter}
   * @param ignoreProperties property names to exclude
   * @return the populated destination bean
   */
  public static <T> T copyNonNull(Object source, Class<T> destination, @Nullable TypeConverter converter, @Nullable Set<String> ignoreProperties) {
    return copy(source, destination, converter,
            BeanPropertyCopyStrategy.ignoreProperties(ignoreProperties).and(BeanPropertyCopyStrategy.nonNull()));
  }

  /**
   * Copy properties selected by a strategy from a source bean or map into a destination bean.
   * Missing or read-only destination properties are ignored. The strategy sees
   * each matching source value before type conversion, including null values.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param strategy the strategy deciding which values to copy
   */
  public static void copy(Object source, Object destination, @Nullable BeanPropertyCopyStrategy strategy) {
    copy(source, destination, null, strategy);
  }

  /**
   * Create a destination bean and copy properties selected by a strategy.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @param strategy the strategy deciding which values to copy
   * @return the populated destination bean
   */
  public static <T> T copy(Object source, Class<T> destination, @Nullable BeanPropertyCopyStrategy strategy) {
    return copy(source, destination, null, strategy);
  }

  /**
   * Copy properties selected by a strategy. The strategy sees values before
   * type conversion, including null values. Use
   * {@link BeanPropertyCopyStrategy#ignoreProperties(Set)} to exclude names.
   *
   * @param source the source bean or map of property names to values
   * @param destination the destination bean
   * @param converter the converter for source values, or {@code null} for the default
   * @param strategy the strategy deciding which values to copy
   */
  public static void copy(Object source, Object destination, @Nullable TypeConverter converter, @Nullable BeanPropertyCopyStrategy strategy) {
    Assert.notNull(source, "source object is required");
    Assert.notNull(destination, "destination object is required");
    copy(source, BeanMetadata.forInstance(destination), destination, converter, strategy);
  }

  /**
   * Create a destination bean and copy properties selected by a strategy.
   * Use {@link BeanPropertyCopyStrategy#ignoreProperties(Set)} to exclude names.
   *
   * @param <T> the destination bean type
   * @param source the source bean or map of property names to values
   * @param destination the class to instantiate
   * @param converter the converter for source values, or {@code null} for the default
   * @param strategy the strategy deciding which values to copy
   * @return the populated destination bean
   */
  @SuppressWarnings("unchecked")
  public static <T> T copy(Object source, Class<T> destination, @Nullable TypeConverter converter, @Nullable BeanPropertyCopyStrategy strategy) {
    Assert.notNull(source, "source object is required");
    Assert.notNull(destination, "destination class is required");
    BeanMetadata metadata = BeanMetadata.forClass(destination);
    Object instance = metadata.newInstance();
    copy(source, metadata, instance, converter, strategy);
    return (T) instance;
  }

  /**
   * Copy readable source values into matching writable destination properties.
   * Unknown and read-only properties are skipped before evaluating the strategy.
   */
  @SuppressWarnings("unchecked")
  private static void copy(Object source, BeanMetadata destination, Object destinationInstance,
          @Nullable TypeConverter converter, @Nullable BeanPropertyCopyStrategy strategy) {
    if (converter == null) {
      converter = new SimpleTypeConverter();
    }

    if (strategy == null) {
      strategy = BeanPropertyCopyStrategy.always();
    }

    if (source instanceof Map) {
      for (var entry : ((Map<String, @Nullable Object>) source).entrySet()) {
        BeanProperty beanProperty = destination.getProperty(entry.getKey());
        if (beanProperty != null && beanProperty.isWriteable()
                && strategy.shouldCopy(source, beanProperty, entry.getValue())) {
          beanProperty.setValue(destinationInstance, entry.getValue(), converter);
        }
      }
    }
    else {
      BeanMetadata sourceMetadata = BeanMetadata.forInstance(source);
      for (BeanProperty property : sourceMetadata.getPropertyList()) {
        if (property.isReadable()) {
          BeanProperty beanProperty = destination.getProperty(property.getName());
          if (beanProperty != null && beanProperty.isWriteable()) {
            Object value = property.getValue(source);
            if (strategy.shouldCopy(source, beanProperty, value)) {
              beanProperty.setValue(destinationInstance, value, converter);
            }
          }
        }
      }
    }
  }

  private static @Nullable Set<String> toPropertySet(String @Nullable [] ignoreProperties) {
    return ObjectUtils.isEmpty(ignoreProperties) ? null : Set.of(ignoreProperties);
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
