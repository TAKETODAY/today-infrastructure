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

import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author TODAY 2021/3/21 15:40
 * @since 3.0
 */
public class DataBinder extends BeanPropertyAccessor {
  protected final List<PropertyValue> propertyValues = new ArrayList<>();

  public DataBinder() { }

  public DataBinder(Class<?> beanClass) {
    super(beanClass);
  }

  public DataBinder(Object object) {
    super(BeanMetadata.ofClass(object.getClass()), object);
  }

  public DataBinder(BeanMetadata metadata, Object object) {
    super(metadata, object);
  }

  public Object bind() {
    final Object rootObject = getRootObject();
    final BeanMetadata metadata = getMetadata();
    for (final PropertyValue propertyValue : propertyValues) {
      setProperty(rootObject, metadata, propertyValue);
    }
    return rootObject;
  }

  public void setProperty(final Object root, final BeanMetadata metadata, final PropertyValue propertyValue) {
    setProperty(root, metadata, propertyValue.getName(), propertyValue.getValue());
  }

  @Override
  protected Object doConvertInternal(Object value, Class<?> requiredType, BeanProperty beanProperty) {
    final PropertyEditor editor = findEditor(requiredType, beanProperty);
    if (editor != null && value instanceof String) {
      try {
        editor.setAsText((String) value);
        return editor.getValue();
      }
      catch (IllegalArgumentException ignored) { }
    }
    // fallback to conversion service
    return super.doConvertInternal(value, requiredType, beanProperty);
  }

  private PropertyEditor findEditor(Class<?> requiredType, BeanProperty beanProperty) {
    return null;
  }

  public void addPropertyValue(String name, Object value) {
    addPropertyValues(new PropertyValue(name, value));
  }

  public void addPropertyValues(PropertyValue... propertyValues) {
    Collections.addAll(this.propertyValues, propertyValues);
  }
  public void addPropertyValues(List<PropertyValue> propertyValues) {
    this.propertyValues.addAll(propertyValues);
  }

}
