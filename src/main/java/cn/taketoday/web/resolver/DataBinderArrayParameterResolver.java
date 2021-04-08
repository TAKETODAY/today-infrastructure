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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY 2021/4/8 17:32
 * @since 3.0
 */
public class DataBinderArrayParameterResolver extends DataBinderCollectionParameterResolver {

  @Override
  public boolean supports(MethodParameter parameter) {
    return parameter.isArray() && !ClassUtils.isSimpleType(parameter.getParameterClass());
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Object doBind(MultiValueMap<String, PropertyValue> propertyValues, MethodParameter parameter) {
    final ArrayList<Object> list = (ArrayList<Object>) super.doBind(propertyValues, parameter);
    final Class<?> componentType = parameter.getComponentType();
    final Object[] o = (Object[]) Array.newInstance(componentType, list.size());
    return list.toArray(o);
  }

  @Override
  protected Class<?> getComponentType(MethodParameter parameter) {
    return parameter.getComponentType();
  }

  @Override
  protected Collection<Object> createCollection(MultiValueMap<String, PropertyValue> propertyValues, MethodParameter parameter) {
    return new ArrayList<>(propertyValues.size());
  }
}
