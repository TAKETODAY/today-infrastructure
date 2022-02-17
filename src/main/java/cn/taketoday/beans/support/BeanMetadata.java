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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.Property;
import cn.taketoday.beans.PropertyReadOnlyException;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.reflect.PropertyAccessor;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MapCache;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2021/1/27 22:26
 * @since 3.0
 */
public class BeanMetadata implements Iterable<BeanProperty> {
  private static final boolean defaultCollectPropertiesFromMethod = // @since 4.0
          TodayStrategies.getFlag("collect.properties.methods", true);

  private static final MapCache<BeanMetadataKey, BeanMetadata, ?> metadataMappings = new MapCache<>(
          new ConcurrentReferenceHashMap<>(), BeanMetadata::new);

  private final Class<?> beanClass;

  private BeanInstantiator constructor;
  /**
   * @since 4.0
   */
  private BeanPropertiesHolder propertyHolder;

  // @since 4.0
  private final boolean collectPropertiesFromMethods;

  private BeanMetadata(BeanMetadataKey key) {
    this.beanClass = key.beanClass;
    this.collectPropertiesFromMethods = key.collectPropertiesFromMethods;
  }

  public BeanMetadata(Class<?> beanClass) {
    this(beanClass, defaultCollectPropertiesFromMethod);
  }

  public BeanMetadata(Class<?> beanClass, boolean collectPropertiesFromMethods) {
    this.beanClass = beanClass;
    this.collectPropertiesFromMethods = collectPropertiesFromMethods;
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
   * @param propertyName Property name
   * @return {@link PropertyAccessor}
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
   * @throws PropertyReadOnlyException If this property is read only
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
  public Class<?> getPropertyClass(String propertyName) {
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
    return createBeanProperties(collectPropertiesFromMethods);
  }

  // @since 4.0
  public boolean isCollectPropertiesFromMethods() {
    return collectPropertiesFromMethods;
  }

  /**
   * @param collectPropertiesFromMethods can collect properties from methods
   * @since 4.0
   */
  public HashMap<String, BeanProperty> createBeanProperties(boolean collectPropertiesFromMethods) {
    HashMap<String, BeanProperty> beanPropertyMap = new HashMap<>();
    ReflectionUtils.doWithFields(beanClass, declaredField -> {
      if (!shouldSkip(declaredField)) {
        String propertyName = getPropertyName(declaredField);
        beanPropertyMap.put(propertyName, new BeanProperty(propertyName, declaredField));
      }
    });

    if (collectPropertiesFromMethods) {
      ReflectionUtils.doWithMethods(beanClass, method -> {
//        if (Modifier.isStatic(method.getModifiers())) {
//          return;
//        }

        String methodName = method.getName();

        BeanProperty property = null;
        if (methodName.startsWith("get") || methodName.startsWith("is")) {
          // find property on a getter
          String propertyName = ReflectionUtils.getPropertyName(method, null);
          if (!beanPropertyMap.containsKey(propertyName)) {
            Method writeMethod = ReflectionUtils.getWriteMethod(beanClass, method.getReturnType(), propertyName);
            property = new BeanProperty(propertyName, method, writeMethod, beanClass);
          }
        }
        else if (methodName.startsWith("set")) {
          // find property on a setter
          String propertyName = ReflectionUtils.getPropertyName(null, method);
          if (!beanPropertyMap.containsKey(propertyName)) {
            Class<?>[] parameterTypes = method.getParameterTypes(); // none null
            if (parameterTypes.length == 1) {
              Method readMethod = ReflectionUtils.getReadMethod(beanClass, parameterTypes[0], propertyName);
              property = new BeanProperty(propertyName, readMethod, method, beanClass);
            }
          }
        }

        if (property != null) {
          String alias = getAnnotatedPropertyName(method);
          if (alias != null) {
            property.setPropertyName(alias);
          }
          beanPropertyMap.put(property.getPropertyName(), property);
        }
      });
    }
    return beanPropertyMap;
  }

  protected boolean shouldSkip(Field declaredField) {
    return false;
  }

  /**
   * get alias property-name
   *
   * @param declaredField {@link Field}
   */
  protected String getPropertyName(Field declaredField) {
    String propertyName = getAnnotatedPropertyName(declaredField);
    if (propertyName == null) {
      propertyName = declaredField.getName();
    }
    return propertyName;
  }

  @Nullable
  protected String getAnnotatedPropertyName(AnnotatedElement propertyElement) {
    // just alias name, cannot override its getter,setter
    MergedAnnotation<Property> annotation = MergedAnnotations.from(propertyElement).get(Property.class);
    if (annotation.isPresent()) {
      String name = annotation.getStringValue();
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
    if (!(o instanceof BeanMetadata that))
      return false;
    return collectPropertiesFromMethods == that.collectPropertiesFromMethods
            && beanClass.equals(that.beanClass);
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
   * @param beanClass target bean class cannot be simple class
   * @return {@link BeanMetadata}
   * @see ClassUtils#isSimpleType(Class)
   */
  public static BeanMetadata from(Class<?> beanClass) {
    return from(beanClass, defaultCollectPropertiesFromMethod);
  }

  /**
   * Create a {@link BeanMetadata} with given bean class
   *
   * @param beanClass target bean class cannot be simple class
   * @param collectPropertiesFromMethods collect properties from methods
   * @return {@link BeanMetadata}
   */
  public static BeanMetadata from(Class<?> beanClass, boolean collectPropertiesFromMethods) {
    return metadataMappings.get(new BeanMetadataKey(beanClass, collectPropertiesFromMethods));
  }

  /**
   * Create a {@link BeanMetadata} with given bean class
   *
   * @param object target bean cannot be simple object
   * @return {@link BeanMetadata}
   * @see ClassUtils#isSimpleType(Class)
   */
  public static BeanMetadata from(Object object) {
    return from(object.getClass(), false);
  }

  /**
   * Create a {@link BeanMetadata} with given bean class
   *
   * @param object target bean cannot be simple object
   * @param collectPropertiesFromMethods collect properties from methods
   * @return {@link BeanMetadata}
   * @since 4.0
   */
  public static BeanMetadata from(Object object, boolean collectPropertiesFromMethods) {
    return from(object.getClass(), collectPropertiesFromMethods);
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

  record BeanMetadataKey(Class<?> beanClass, boolean collectPropertiesFromMethods) {

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof BeanMetadataKey that) {
        return collectPropertiesFromMethods == that.collectPropertiesFromMethods
                && Objects.equals(beanClass, that.beanClass);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(beanClass, collectPropertiesFromMethods);
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
