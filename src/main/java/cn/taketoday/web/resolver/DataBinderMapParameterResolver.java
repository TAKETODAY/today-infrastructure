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
package cn.taketoday.web.resolver;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.factory.support.PropertyValuesBinder;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author TODAY 2019-07-09 22:49
 * @see <a href='https://taketoday.cn/articles/1616819014712'>TODAY Context 之 BeanPropertyAccessor</a>
 */
public class DataBinderMapParameterResolver
        extends AbstractDataBinderParameterResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsInternal(final ResolvableMethodParameter parameter) {
    if (isMap(parameter)) {
      final Type valueType = parameter.getGeneric(1);
      if (valueType instanceof Class) {
        return supportsSetProperties(valueType);
      }
    }
    return false;
  }

  public static boolean isMap(ResolvableMethodParameter parameter) {
    return parameter.is(Map.class);
  }

  /**
   * Resolve {@link Map} parameter.
   */
  @Override
  protected Object doBind(MultiValueMap<String, PropertyValue> propertyValues, ResolvableMethodParameter parameter) {
    final Map<String, Object> map = CollectionUtils.createMap(parameter.getParameterType(), propertyValues.size());

    final PropertyValuesBinder dataBinder = new PropertyValuesBinder();
    final Class<?> parameterClass = (Class<?>) parameter.getGeneric(1);
    final BeanMetadata parameterMetadata = BeanMetadata.from(parameterClass);
    for (final Map.Entry<String, List<PropertyValue>> entry : propertyValues.entrySet()) {
      final Object rootObject = parameterMetadata.newInstance();
      final List<PropertyValue> propertyValueList = entry.getValue();
      dataBinder.bind(rootObject, parameterMetadata, propertyValueList);

      map.put(entry.getKey(), rootObject);
    }
    return map;
  }

}
