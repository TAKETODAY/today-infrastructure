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

package cn.taketoday.web.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.factory.BeanMetadata;
import cn.taketoday.context.factory.DataBinder;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY 2021/4/8 14:33
 * @since 3.0
 */
public class DataBinderCollectionParameterResolver extends AbstractDataBinderParameterResolver<Integer> {

  @Override
  public boolean supports(MethodParameter parameter) {
    return parameter.isCollection() && !ClassUtils.primitiveTypes.contains(parameter.getParameterClass());
  }

  @Override
  protected Object doBind(MultiValueMap<Integer, PropertyValue> propertyValues, MethodParameter parameter) {
    final ArrayList<Object> list = new ArrayList<>();

    final DataBinder dataBinder = new DataBinder();
    final Class<?> parameterClass = (Class<?>) parameter.getGenerics(0);
    final BeanMetadata parameterMetadata = BeanMetadata.ofClass(parameterClass);
    for (final Map.Entry<Integer, List<PropertyValue>> entry : propertyValues.entrySet()) {
      final Object rootObject = parameterMetadata.newInstance();
      final List<PropertyValue> propertyValueList = entry.getValue();
      dataBinder.bind(rootObject, parameterMetadata, propertyValueList);

      CollectionUtils.setValue(list, entry.getKey(), rootObject);
    }
    return list;
  }

  @Override
  protected void doPutValue(MultiValueMap<Integer, PropertyValue> propertyValues, String key, PropertyValue propertyValue) {
    final int valueIndex = Integer.parseInt(key);
    propertyValues.add(valueIndex, propertyValue);
  }

}
