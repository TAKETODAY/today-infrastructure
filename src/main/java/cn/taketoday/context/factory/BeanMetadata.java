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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.context.annotation.Property;
import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.reflect.ConstructorAccessor;
import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.Mappings;
import cn.taketoday.context.utils.ReflectionUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY
 * 2021/1/27 22:26
 * @since 3.0
 */
public class BeanMetadata {

  private static final Mappings<BeanMetadata, ?> metadataMappings = new Mappings<>(BeanMetadata::new);
  private static final BeanPropertiesMappings beanPropertiesMappings = new BeanPropertiesMappings();

  private final Class<?> beanClass;

  private ConstructorAccessor constructor;
  private final Map<String, BeanProperty> beanProperties;

  private BeanMetadata(Object key) {
    this((Class<?>) key);
  }

  public BeanMetadata(Class<?> beanClass) {
    this.beanClass = beanClass;
    this.beanProperties = beanPropertiesMappings.get(beanClass, this);
  }

  public Class<?> getType() {
    return this.beanClass;
  }

  public ConstructorAccessor getConstructor() {
    if (constructor == null) {
      constructor = ReflectionUtils.newConstructorAccessor(beanClass);
    }
    return constructor;
  }

  /**
   * Create this bean a new instance with no arguments
   *
   * @return a new instance object
   */
  public Object newInstance() {
    return newInstance(null);
  }

  /**
   * Create this bean a new instance with given arguments
   *
   * @return a new instance object
   */
  public Object newInstance(Object[] args) {
    return getConstructor().newInstance(args);
  }

  /**
   * Get {@link PropertyAccessor}
   *
   * @param propertyName
   *         Property name
   *
   * @return {@link PropertyAccessor}
   */
  public PropertyAccessor getPropertyAccessor(String propertyName) {
    return getBeanProperty(propertyName).getPropertyAccessor();
  }

  public BeanProperty getBeanProperty(final String propertyName) {
    return beanProperties.get(propertyName);
  }

  /**
   * Get {@link BeanProperty} with given name
   *
   * @param propertyName
   *         property name
   *
   * @return target {@link BeanProperty}
   *
   * @throws NoSuchPropertyException
   *         If no such property
   */
  public BeanProperty obtainBeanProperty(final String propertyName) {
    final BeanProperty beanProperty = getBeanProperty(propertyName);
    if (beanProperty == null) {
      throw NoSuchPropertyException.noSuchProperty(beanClass, propertyName);
    }
    return beanProperty;
  }

  /**
   * Set a value to root object
   *
   * @param root
   *         Root object
   * @param propertyName
   *         Property name
   * @param value
   *         new value to set
   *
   * @throws NoSuchPropertyException
   *         If no such property
   * @see #obtainBeanProperty(String)
   */
  public void setProperty(final Object root, final String propertyName, final Object value) {
    obtainBeanProperty(propertyName).setValue(root, value);
  }

  /**
   * Get property value
   *
   * @param root
   *         Root object
   * @param propertyName
   *         Property name
   *
   * @throws NoSuchPropertyException
   *         If no such property
   * @see #obtainBeanProperty(String)
   */
  public Object getProperty(final Object root, final String propertyName) {
    return obtainBeanProperty(propertyName).getValue(root);
  }

  /**
   * Get property type
   *
   * @param propertyName
   *         Property name
   *
   * @throws NoSuchPropertyException
   *         If no such property
   * @see #obtainBeanProperty(String)
   */
  public Class<?> getPropertyClass(final String propertyName) {
    return obtainBeanProperty(propertyName).getType();
  }

  /**
   * Get properties mapping
   *
   * @return map of properties
   */
  public Map<String, BeanProperty> getBeanProperties() {
    return beanProperties;
  }

  public Map<String, BeanProperty> createBeanProperties() {
    Map<String, BeanProperty> beanPropertyMap = new HashMap<>();
    final Collection<Field> declaredFields = ReflectionUtils.getFields(beanClass);
    for (final Field declaredField : declaredFields) {
      if (!shouldSkip(declaredField)) {
        String propertyName = getPropertyName(declaredField);
        beanPropertyMap.put(propertyName, new BeanProperty(declaredField));
      }
    }
    return beanPropertyMap;
  }

  protected boolean shouldSkip(Field declaredField) {
    return Modifier.isStatic(declaredField.getModifiers());
  }

  protected String getPropertyName(Field declaredField) {
    String propertyName = getAnnotatedPropertyName(declaredField);
    if (propertyName == null) {
      propertyName = declaredField.getName();
    }
    return propertyName;
  }

  protected String getAnnotatedPropertyName(AnnotatedElement propertyElement) {
    final Property property = ClassUtils.getAnnotation(Property.class, propertyElement);
    if (property != null) {
      final String name = property.value();
      if (StringUtils.isNotEmpty(name)) {
        return name;
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BeanMetadata)) return false;
    final BeanMetadata that = (BeanMetadata) o;
    return Objects.equals(beanClass, that.beanClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(beanClass);
  }

  /**
   * Create a {@link BeanMetadata} with given bean class
   *
   * @param beanClass
   *         target bean class cannot be simple class
   *
   * @return {@link BeanMetadata}
   *
   * @see ClassUtils#isSimpleType(Class)
   */
  public static BeanMetadata ofClass(Class<?> beanClass) {
    return metadataMappings.get(beanClass);
  }

  /**
   * Create a {@link BeanMetadata} with given bean class
   *
   * @param object
   *         target bean cannot be simple object
   *
   * @return {@link BeanMetadata}
   *
   * @see ClassUtils#isSimpleType(Class)
   */
  public static BeanMetadata ofObject(Object object) {
    return ofClass(object.getClass());
  }

  /**
   * Mapping cache
   */
  static class BeanPropertiesMappings extends Mappings<Map<String, BeanProperty>, BeanMetadata> {

    @Override
    protected Map<String, BeanProperty> createValue(Object key, BeanMetadata param) {
      return param.createBeanProperties();
    }

  }

}
