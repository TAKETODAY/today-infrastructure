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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.reflect.PropertyAccessor;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MapCache;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author TODAY 2021/1/27 22:26
 * @since 3.0
 */
public final class BeanMetadata implements Iterable<BeanProperty> {

  private static final MapCache<Class<?>, BeanMetadata, ?> metadataMappings = new MapCache<>(
          new ConcurrentReferenceHashMap<>(), BeanMetadata::new);

  private final Class<?> beanClass;

  private BeanInstantiator instantiator;

  /**
   * @since 4.0
   */
  private BeanPropertiesHolder propertyHolder;

  public BeanMetadata(Class<?> beanClass) {
    this.beanClass = beanClass;
  }

  public Class<?> getType() {
    return this.beanClass;
  }

  public BeanInstantiator getInstantiator() {
    BeanInstantiator instantiator = this.instantiator;
    if (instantiator == null) {
      instantiator = BeanInstantiator.fromClass(beanClass);
      this.instantiator = instantiator;
    }
    return instantiator;
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
    return getInstantiator().instantiate(args);
  }

  /**
   * Get {@link cn.taketoday.reflect.PropertyAccessor}
   *
   * @param propertyName Property name
   * @return {@link cn.taketoday.reflect.PropertyAccessor}
   * @throws NoSuchPropertyException If no such property
   */
  public PropertyAccessor getPropertyAccessor(String propertyName) {
    return obtainBeanProperty(propertyName).getPropertyAccessor();
  }

  @Nullable
  public BeanProperty getBeanProperty(String propertyName) {
    return getBeanProperties().get(propertyName);
  }

  /**
   * Get {@link BeanProperty} with given name
   *
   * @param propertyName property name
   * @return target {@link BeanProperty}
   * @throws NoSuchPropertyException If no such property
   */
  public BeanProperty obtainBeanProperty(String propertyName) {
    BeanProperty beanProperty = getBeanProperty(propertyName);
    if (beanProperty == null) {
      throw new NoSuchPropertyException(beanClass, propertyName);
    }
    return beanProperty;
  }

  /**
   * Set a value to root object
   *
   * @param root Root object
   * @param propertyName Property name
   * @param value new value to set
   * @throws NotWritablePropertyException If this property is read only
   * @throws NoSuchPropertyException If no such property
   * @see #obtainBeanProperty(String)
   */
  public void setProperty(Object root, String propertyName, Object value) {
    obtainBeanProperty(propertyName).setValue(root, value);
  }

  /**
   * Get property value
   *
   * @param root Root object
   * @param propertyName Property name
   * @throws NoSuchPropertyException If no such property
   * @see #obtainBeanProperty(String)
   */
  public Object getProperty(Object root, String propertyName) {
    return obtainBeanProperty(propertyName).getValue(root);
  }

  /**
   * Get property type
   *
   * @param propertyName Property name
   * @throws NoSuchPropertyException If no such property
   * @see #obtainBeanProperty(String)
   */
  public Class<?> getPropertyType(String propertyName) {
    return obtainBeanProperty(propertyName).getType();
  }

  /**
   * Get properties mapping
   *
   * @return map of properties
   */
  public HashMap<String, BeanProperty> getBeanProperties() {
    return propertyHolder().mapping;
  }

  /**
   * Get list of properties
   *
   * <p>
   * Note: not read-only
   *
   * @return list of properties
   */
  public ArrayList<BeanProperty> beanProperties() {
    return propertyHolder().beanProperties;
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
    BeanPropertiesHolder propertyHolder = this.propertyHolder;
    if (propertyHolder == null) {
      propertyHolder = BeanPropertiesMapCache.computeProperties(this);
      this.propertyHolder = propertyHolder;
    }
    return propertyHolder;
  }

  public HashMap<String, BeanProperty> createBeanProperties() {
    HashMap<String, BeanProperty> beanPropertyMap = new HashMap<>();
    CachedIntrospectionResults results = new CachedIntrospectionResults(beanClass);

    PropertyDescriptor[] propertyDescriptors = results.getPropertyDescriptors();
    for (PropertyDescriptor descriptor : propertyDescriptors) {
      if (descriptor.getReadMethod() != null || descriptor.getWriteMethod() != null) {
        BeanProperty property = new BeanProperty(descriptor, beanClass);
        beanPropertyMap.put(descriptor.getName(), property);
      }
    }

    ReflectionUtils.doWithFields(beanClass, field -> {
      if (!Modifier.isStatic(field.getModifiers())) {
        String propertyName = getPropertyName(field);
        if (!beanPropertyMap.containsKey(propertyName)) {
          BeanProperty property = new FieldBeanProperty(field);
          beanPropertyMap.put(propertyName, property);
        }
      }
    });

    return beanPropertyMap;
  }

  private String getPropertyName(Field field) {
    // todo maybe start with 'm,_'
    return field.getName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof BeanMetadata that))
      return false;
    return beanClass.equals(that.beanClass);
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
  // static factory method
  //---------------------------------------------------------------------

  /**
   * Create a {@link BeanMetadata} with given bean class
   *
   * @param beanClass target bean class cannot be simple class
   * @return {@link BeanMetadata}
   * @see ClassUtils#isSimpleType(Class)
   */
  public static BeanMetadata from(Class<?> beanClass) {
    return metadataMappings.get(beanClass);
  }

  /**
   * Create a {@link BeanMetadata} with given bean class
   *
   * @param object target bean cannot be simple object
   * @return {@link BeanMetadata}
   * @see ClassUtils#isSimpleType(Class)
   */
  public static BeanMetadata from(Object object) {
    return from(object.getClass());
  }

  /**
   * @since 4.0
   */
  static final class BeanPropertiesHolder {
    public final HashMap<String, BeanProperty> mapping;
    public final ArrayList<BeanProperty> beanProperties;

    BeanPropertiesHolder(HashMap<String, BeanProperty> mapping) {
      this.mapping = mapping;
      this.beanProperties = new ArrayList<>(mapping.values());
    }
  }

  /**
   * Mapping cache
   */
  static class BeanPropertiesMapCache extends MapCache<BeanMetadata, BeanPropertiesHolder, BeanMetadata> {
    private static final BeanPropertiesMapCache beanPropertiesMappings = new BeanPropertiesMapCache();

    BeanPropertiesMapCache() {
      super(new ConcurrentReferenceHashMap<>());
    }

    static BeanPropertiesHolder computeProperties(BeanMetadata metadata) {
      return beanPropertiesMappings.get(metadata);
    }

    @Override
    protected BeanPropertiesHolder createValue(BeanMetadata key, BeanMetadata param) {
      HashMap<String, BeanProperty> propertyMap = key.createBeanProperties();
      return new BeanPropertiesHolder(propertyMap);
    }

  }

}
