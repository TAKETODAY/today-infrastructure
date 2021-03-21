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
import java.util.List;

/**
 * @author TODAY 2021/3/21 15:40
 * @since 3.0
 */
public class DataBinder extends BeanPropertyAccessor {

  List<PropertyValue> propertyValues;

  public Object bind() {
    final Object bean = getMetadata().newInstance(); // native-invoke constructor

    return bean;
  }

  @Override
  protected Object convertIfNecessary(Object value, Class<?> requiredType, BeanProperty beanProperty) {
    final PropertyEditor editor = findEditor(requiredType, beanProperty);
    if (editor != null && value instanceof String) {
      try {
        editor.setAsText((String) value);
        return editor.getValue();
      }
      catch (IllegalArgumentException ignored) { }
    }
    return super.convertIfNecessary(value, requiredType, beanProperty);
  }

  private PropertyEditor findEditor(Class<?> requiredType, BeanProperty beanProperty) {
    return null;
  }

}
