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

import java.util.List;
import java.util.Map;

import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.factory.support.PropertyValuesBinder;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author TODAY 2019-07-09 22:49
 * @see <a href='https://taketoday.cn/articles/1616819014712'>TODAY Context 之 BeanPropertyAccessor</a>
 */
public class DataBinderMapParameterResolver
        extends AbstractDataBinderParameterResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsInternal(final MethodParameter parameter) {
    if (isMap(parameter)) {
      ResolvableType generic = ResolvableType.forMethodParameter(parameter).asMap().getGeneric(1);
      Class<?> valueType = generic.resolve();
      if (valueType != null) {
        return supportsSetProperties(valueType);
      }
    }
    return false;
  }

  public static boolean isMap(MethodParameter parameter) {
    return parameter.getParameterType() == Map.class;
  }

  /**
   * Resolve {@link Map} parameter.
   */
  @Override
  protected Object doBind(
          MultiValueMap<String, PropertyValue> propertyValues, ResolvableMethodParameter resolvable) {
    MethodParameter parameter = resolvable.getParameter();

    Map<String, Object> map = CollectionUtils.createMap(
            parameter.getParameterType(), propertyValues.size());

    PropertyValuesBinder dataBinder = new PropertyValuesBinder();
    ResolvableType generic = resolvable.getResolvableType().asMap().getGeneric(1);
    Class<?> parameterClass = generic.resolve();

    BeanMetadata parameterMetadata = BeanMetadata.from(parameterClass);
    for (Map.Entry<String, List<PropertyValue>> entry : propertyValues.entrySet()) {
      Object rootObject = parameterMetadata.newInstance();
      List<PropertyValue> propertyValueList = entry.getValue();
      dataBinder.bind(rootObject, parameterMetadata, propertyValueList);

      map.put(entry.getKey(), rootObject);
    }
    return map;
  }

}
