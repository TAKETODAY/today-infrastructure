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

package cn.taketoday.beans.support;

import java.util.Map;

import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.factory.InvalidPropertyValueException;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Bean properties utils
 *
 * @author TODAY 2021/5/2 22:14
 * @since 3.0.2
 */
public class BeanProperties {

  /**
   * Copy the property values of the given source bean into the given target bean.
   * <p>Note: The source and target classes do not have to match or even be derived
   * from each other, as long as the properties match. Any bean properties that the
   * source bean exposes but the target bean does not will silently be ignored.
   *
   * @param source
   *         source object
   * @param destination
   *         destination object
   */
  public static void copy(Object source, Object destination) {
    Assert.notNull(source, "source object must not be null");
    Assert.notNull(destination, "destination object must not be null");

    final BeanMetadata destinationMetadata = BeanMetadata.ofObject(destination);
    copy(source, destinationMetadata, destination, null);
  }

  /**
   * Copy the property values of the given source bean into the given target bean.
   * <p>Note: The source and target classes do not have to match or even be derived
   * from each other, as long as the properties match. Any bean properties that the
   * source bean exposes but the target bean does not will silently be ignored.
   *
   * @param source
   *         the source bean
   * @param ignoreProperties
   *         array of property names to ignore
   */
  public static void copy(Object source, Object destination, @Nullable String... ignoreProperties) {
    Assert.notNull(source, "source object must not be null");
    Assert.notNull(destination, "destination object must not be null");

    final BeanMetadata destinationMetadata = BeanMetadata.ofObject(destination);
    copy(source, destinationMetadata, destination, ignoreProperties);
  }

  /**
   * Copy the property values of the given source bean into the given target bean.
   * <p>Note: The source and target classes do not have to match or even be derived
   * from each other, as long as the properties match. Any bean properties that the
   * source bean exposes but the target bean does not will silently be ignored.
   *
   * @param source
   *         source object
   * @param destination
   *         destination class
   *
   * @return returns a destination type object
   */
  @SuppressWarnings("unchecked")
  public static <T> T copy(Object source, Class<T> destination) {
    Assert.notNull(source, "source object must not be null");
    Assert.notNull(destination, "destination class must not be null");

    final BeanMetadata destinationMetadata = BeanMetadata.ofClass(destination);
    final Object destinationInstance = destinationMetadata.newInstance(); // destination
    copy(source, destinationMetadata, destinationInstance, null);
    return (T) destinationInstance;
  }

  /**
   * Copy the property values of the given source bean into the given target bean.
   * <p>Note: The source and target classes do not have to match or even be derived
   * from each other, as long as the properties match. Any bean properties that the
   * source bean exposes but the target bean does not will silently be ignored.
   */
  @SuppressWarnings("unchecked")
  public static <T> T copy(Object source, Class<T> destination, String... ignoreProperties) {
    Assert.notNull(source, "source object must not be null");
    Assert.notNull(destination, "destination class must not be null");

    final BeanMetadata destinationMetadata = BeanMetadata.ofClass(destination);
    final Object destinationInstance = destinationMetadata.newInstance(); // destination
    copy(source, destinationMetadata, destinationInstance, ignoreProperties);
    return (T) destinationInstance;
  }

  /**
   * Ignore read-only properties
   */
  @SuppressWarnings("unchecked")
  private static void copy(
          Object source, BeanMetadata destinationMetadata,
          Object destinationInstance, @Nullable String[] ignoreProperties) {
    if (source instanceof Map) {
      for (final Map.Entry<String, Object> entry : ((Map<String, Object>) source).entrySet()) {
        final String propertyName = entry.getKey();
        if (allowCopy(ignoreProperties, propertyName)) {
          final BeanProperty beanProperty = destinationMetadata.getBeanProperty(propertyName);
          if (beanProperty != null && !beanProperty.isReadOnly()) {
            final Object value = entry.getValue();
            beanProperty.setValue(destinationInstance, value);
          }
        }
      }
    }
    else {
      final BeanMetadata sourceMetadata = BeanMetadata.ofObject(source);
      for (final Map.Entry<String, BeanProperty> entry : sourceMetadata.getBeanProperties().entrySet()) {
        final String propertyName = entry.getKey();
        if (allowCopy(ignoreProperties, propertyName)) {
          final BeanProperty beanProperty = destinationMetadata.getBeanProperty(propertyName);
          if (beanProperty != null && !beanProperty.isReadOnly()) {
            final Object value = entry.getValue().getValue(source);
            beanProperty.setValue(destinationInstance, value);
          }
        }
      }
    }
  }

  private static boolean allowCopy(@Nullable String[] ignoreProperties, String propertyName) {
    if (ObjectUtils.isNotEmpty(ignoreProperties)) {
      for (final String ignoreProperty : ignoreProperties) {
        if (propertyName.equals(ignoreProperty)) {
          return false;
        }
      }
    }
    return true;
  }

  //

  /**
   * <p>Populate the JavaBeans properties of the specified bean, based on
   * the specified name/value pairs.  This method uses Java reflection APIs
   * to identify corresponding "property setter" method names, and deals
   * with setter arguments of type <code>String</code>, <code>boolean</code>,
   * <code>int</code>, <code>long</code>, <code>float</code>, and
   * <code>double</code>.  In addition, array setters for these types (or the
   * corresponding primitive types) can also be identified.</p>
   *
   * <p>The particular setter method to be called for each property is
   * determined using the usual JavaBeans introspection mechanisms.  Thus,
   * you may identify custom setter methods using a BeanInfo class that is
   * associated with the class of the bean itself.  If no such BeanInfo
   * class is available, the standard method name conversion ("set" plus
   * the capitalized name of the property in question) is used.</p>
   *
   * <p>
   * default is ignoreUnknownProperty
   * </p>
   *
   * @param bean
   *         JavaBean whose properties are being populated
   * @param properties
   *         Map keyed by property name, with the
   *         corresponding (String or String[]) value(s) to be set
   *
   * @throws NoSuchPropertyException
   *         If no such property
   * @throws InvalidPropertyValueException
   *         Invalid property value
   */
  public static void populate(final Object bean, final Map<String, Object> properties) {
    populate(bean, properties, true);
  }

  /**
   * <p>Populate the JavaBeans properties of the specified bean, based on
   * the specified name/value pairs. This method uses Java reflection APIs
   * to identify corresponding "property setter" method names, and deals
   * with setter arguments of type <code>String</code>, <code>boolean</code>,
   * <code>int</code>, <code>long</code>, <code>float</code>, and
   * <code>double</code>.  In addition, array setters for these types (or the
   * corresponding primitive types) can also be identified.</p>
   *
   * <p>The particular setter method to be called for each property is
   * determined using the usual JavaBeans introspection mechanisms.  Thus,
   * you may identify custom setter methods using a BeanInfo class that is
   * associated with the class of the bean itself.  If no such BeanInfo
   * class is available, the standard method name conversion ("set" plus
   * the capitalized name of the property in question) is used.</p>
   *
   * @param bean
   *         JavaBean whose properties are being populated
   * @param properties
   *         Map keyed by property name, with the
   *         corresponding (String or String[]) value(s) to be set
   * @param ignoreUnknownProperty
   *         {@link BeanPropertyAccessor#ignoreUnknownProperty}
   *
   * @throws NoSuchPropertyException
   *         If no such property
   * @throws InvalidPropertyValueException
   *         Invalid property value
   * @see BeanPropertyAccessor
   */
  public static void populate(
          final Object bean, final Map<String, Object> properties, final boolean ignoreUnknownProperty) {
    Assert.notNull(bean, "target bean must not be null");
    Assert.notNull(properties, "properties must not be null");
    final BeanMetadata metadata = BeanMetadata.ofObject(bean);
    final BeanPropertyAccessor accessor = BeanPropertyAccessor.from(metadata, bean);
    accessor.setIgnoreUnknownProperty(ignoreUnknownProperty);
    accessor.setThrowsWhenReadOnly(false);
    for (final Map.Entry<String, Object> entry : properties.entrySet()) {
      final String key = entry.getKey();
      final Object value = entry.getValue();
      accessor.setProperty(bean, metadata, key, value);
    }
  }

}
