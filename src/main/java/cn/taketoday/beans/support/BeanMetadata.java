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

package cn.taketoday.beans.support;

import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.Property;
import cn.taketoday.beans.factory.PropertyReadOnlyException;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.reflect.PropertyAccessor;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.Mappings;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * @author TODAY 2021/1/27 22:26
 * @since 3.0
 */
public class BeanMetadata implements Iterable<BeanProperty> {

  private static final Mappings<BeanMetadata, ?> metadataMappings = new Mappings<>(BeanMetadata::new);

  private final Class<?> beanClass;

  private BeanInstantiator constructor;
  /**
   * @since 4.0
   */
  private BeanPropertiesHolder propertyHolder;

  private BeanMetadata(Object key) {
    this((Class<?>) key);
  }

  public BeanMetadata(Class<?> beanClass) {
    this.beanClass = beanClass;
  }

  public Class<?> getType() {
    return this.beanClass;
  }

  public BeanInstantiator getConstructor() {
    BeanInstantiator constructor = this.constructor;
    if (constructor == null) {
      constructor = createAccessor();
      this.constructor = constructor;
    }
    return constructor;
  }

  protected BeanInstantiator createAccessor() {
    return BeanInstantiator.fromConstructor(beanClass);
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
  public Object newInstance(@Nullable Object[] args) {
    return getConstructor().instantiate(args);
  }

  /**
   * Get {@link PropertyAccessor}
   *
   * @param propertyName
   *         Property name
   *
   * @return {@link PropertyAccessor}
   *
   * @throws NoSuchPropertyException
   *         If no such property
   */
  public PropertyAccessor getPropertyAccessor(String propertyName) {
    return obtainBeanProperty(propertyName).getPropertyAccessor();
  }

  @Nullable
  public BeanProperty getBeanProperty(final String propertyName) {
    return getBeanProperties().get(propertyName);
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
      throw new NoSuchPropertyException(beanClass, propertyName);
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
   * @throws PropertyReadOnlyException
   *         If this property is read only
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
  @NonNull
  public HashMap<String, BeanProperty> getBeanProperties() {
    return propertyHolder().mapping;
  }

  /**
   * @since 4.0
   */
  public int getPropertySize() {
    return propertyHolder().beanProperties.size();
  }

  /**
   * @since 4.0
   */
  public boolean containsProperty(String name) {
    return propertyHolder().mapping.containsKey(name);
  }

  /**
   * @since 4.0
   */
  private BeanPropertiesHolder propertyHolder() {
    if (propertyHolder == null) {
      propertyHolder = BeanPropertiesMappings.computeProperties(beanClass, this);
    }
    return propertyHolder;
  }

  public HashMap<String, BeanProperty> createBeanProperties() {
    HashMap<String, BeanProperty> beanPropertyMap = new HashMap<>();
    ReflectionUtils.doWithFields(beanClass, declaredField -> {
      if (!shouldSkip(declaredField)) {
        String propertyName = getPropertyName(declaredField);
        beanPropertyMap.put(propertyName, new BeanProperty(propertyName, declaredField));
      }
    });
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

  protected String getAnnotatedPropertyName(Field propertyElement) {
    // just alias name, cannot override its getter,setter
    AnnotationAttributes attributes = AnnotationUtils.getAttributes(Property.class, propertyElement);
    if (attributes != null) {
      final String name = attributes.getString(Constant.VALUE);
      if (StringUtils.isNotEmpty(name)) {
        return name;
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof BeanMetadata))
      return false;
    final BeanMetadata that = (BeanMetadata) o;
    return Objects.equals(beanClass, that.beanClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(beanClass);
  }

  //---------------------------------------------------------------------
  // Implementation of Iterable interface
  //---------------------------------------------------------------------

  @Override
  public Iterator<BeanProperty> iterator() {
    return propertyHolder().beanProperties.iterator();
  }

  @Override
  public void forEach(Consumer<? super BeanProperty> action) {
    propertyHolder().beanProperties.forEach(action);
  }

  @Override
  public Spliterator<BeanProperty> spliterator() {
    return propertyHolder().beanProperties.spliterator();
  }

  //---------------------------------------------------------------------
  // static
  //---------------------------------------------------------------------

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
   * @since 4.0
   */
  static final class BeanPropertiesHolder {
    final HashMap<String, BeanProperty> mapping;
    final ArrayList<BeanProperty> beanProperties;

    BeanPropertiesHolder(HashMap<String, BeanProperty> mapping) {
      this.mapping = mapping;
      this.beanProperties = new ArrayList<>(mapping.values());
    }
  }

  /**
   * Mapping cache
   */
  static class BeanPropertiesMappings extends Mappings<BeanPropertiesHolder, BeanMetadata> {
    private static final BeanPropertiesMappings beanPropertiesMappings = new BeanPropertiesMappings();

    static BeanPropertiesHolder computeProperties(Class<?> beanClass, BeanMetadata metadata) {
      return beanPropertiesMappings.get(beanClass, metadata);
    }

    @Override
    protected BeanPropertiesHolder createValue(Object key, BeanMetadata param) {
      HashMap<String, BeanProperty> propertyMap = param.createBeanProperties();
      return new BeanPropertiesHolder(propertyMap);
    }

  }

}
