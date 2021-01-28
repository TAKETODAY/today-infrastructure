/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import cn.taketoday.context.annotation.Property;
import cn.taketoday.context.reflect.ConstructorAccessor;
import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.Mappings;
import cn.taketoday.context.utils.ReflectionUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY
 * @date 2021/1/27 22:26
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

  public Object newInstance() {
    return getConstructor().newInstance();
  }

  public PropertyAccessor getPropertyAccessor(String propertyName) {
    return getBeanProperty(propertyName).getPropertyAccessor();
  }

  public BeanProperty getBeanProperty(final String propertyName) {
    return beanProperties.get(propertyName);
  }

  public Map<String, BeanProperty> getBeanProperties() {
    return beanProperties;
  }

  public Map<String, BeanProperty> createBeanProperties() {
    Map<String, BeanProperty> beanPropertyMap = new HashMap<>();
    final Collection<Field> declaredFields = ReflectionUtils.getFields(beanClass);
    for (final Field declaredField : declaredFields) {
      if (Modifier.isStatic(declaredField.getModifiers())) {
        continue;
      }

      String propertyName = getAnnotatedPropertyName(declaredField);
      if (propertyName == null) {
        propertyName = declaredField.getName();
      }

      beanPropertyMap.put(propertyName, new BeanProperty(declaredField));
    }
    return beanPropertyMap;
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
   * Mapping cache
   */
  static class BeanPropertiesMappings extends Mappings<Map<String, BeanProperty>, BeanMetadata> {

    @Override
    protected Map<String, BeanProperty> createValue(Object key, BeanMetadata param) {
      return param.createBeanProperties();
    }

  }

}
