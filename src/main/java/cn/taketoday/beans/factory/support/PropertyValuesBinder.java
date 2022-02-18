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

package cn.taketoday.beans.factory.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.support.BeanPropertyAccessor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Assert;

/**
 * Bind PropertyValues to target bean
 *
 * @author TODAY 2021/3/21 15:40
 * @see PropertyValue
 * @since 3.0
 */
public class PropertyValuesBinder extends BeanPropertyAccessor {
  protected ArrayList<PropertyValue> propertyValues;

  public PropertyValuesBinder() { }

  public PropertyValuesBinder(Class<?> beanClass) {
    super(beanClass);
  }

  public PropertyValuesBinder(Class<?> beanClass, ConversionService conversionService) {
    super(beanClass, conversionService);
  }

  public PropertyValuesBinder(Object object) {
    super(BeanMetadata.from(object), object);
  }

  public PropertyValuesBinder(BeanMetadata metadata, Object object) {
    super(metadata, object);
  }

  /**
   * Bind {@link #propertyValues} to {@link #rootObject} object
   */
  public Object bind() {
    Assert.state(propertyValues != null, "No property values");
    return bind(propertyValues);
  }

  /**
   * Bind {@code propertyValues} to {@link #rootObject} object
   */
  public Object bind(Iterable<PropertyValue> propertyValues) {
    return bind(getRootObject(), propertyValues);
  }

  /**
   * Bind {@code propertyValues} to {@code rootObject} object
   */
  public Object bind(Object rootObject, Iterable<PropertyValue> propertyValues) {
    return bind(rootObject, getMetadata(), propertyValues);
  }

  public Object bind(Object rootObject, BeanMetadata metadata, Iterable<PropertyValue> propertyValues) {
    for (PropertyValue propertyValue : propertyValues) {
      setProperty(rootObject, metadata, propertyValue);
    }
    return rootObject;
  }

  // Map

  /**
   * bind map of property-values to root object
   *
   * @param propertyValues map of property values
   * @since 4.0
   */
  public void bind(Map<String, Object> propertyValues) {
    for (Map.Entry<String, Object> entry : propertyValues.entrySet()) {
      setProperty(rootObject, metadata, entry.getKey(), entry.getValue());
    }
  }

  public void setProperty(Object root, BeanMetadata metadata, PropertyValue propertyValue) {
    setProperty(root, metadata, propertyValue.getName(), propertyValue.getValue());
  }

  //

  public void addPropertyValue(String name, Object value) {
    addPropertyValue(new PropertyValue(name, value));
  }

  public void addPropertyValue(PropertyValue propertyValue) {
    getPropertyValues().add(propertyValue);
  }

  public void addPropertyValues(PropertyValue... propertyValues) {
    Collections.addAll(getPropertyValues(), propertyValues);
  }

  public void addPropertyValues(List<PropertyValue> propertyValues) {
    getPropertyValues().addAll(propertyValues);
  }

  public void addPropertyValues(Map<String, Object> propertyValues) {
    propertyValues.forEach(this::addPropertyValue);
  }

  public List<PropertyValue> getPropertyValues() {
    if (propertyValues == null) {
      propertyValues = new ArrayList<>();
    }
    return propertyValues;
  }
}
