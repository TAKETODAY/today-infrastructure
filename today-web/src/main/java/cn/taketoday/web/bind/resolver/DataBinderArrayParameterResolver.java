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

package cn.taketoday.web.bind.resolver;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

import cn.taketoday.beans.PropertyValue;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author TODAY 2021/4/8 17:32
 * @see <a href='https://taketoday.cn/articles/1616819014712'>TODAY Context 之 BeanPropertyAccessor</a>
 * @since 3.0
 */
@Deprecated
public class DataBinderArrayParameterResolver extends DataBinderCollectionParameterResolver {

  @Override
  protected boolean supportsInternal(ResolvableMethodParameter parameter) {
    Class<?> parameterType = parameter.getParameterType();
    return parameterType.isArray() && supportsSetProperties(parameterType);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Object doBind(MultiValueMap<String, PropertyValue> propertyValues, ResolvableMethodParameter resolvable) {
    final ArrayList<Object> list = (ArrayList<Object>) super.doBind(propertyValues, resolvable);
    final Class<?> componentType = resolvable.getComponentType();
    final Object[] o = (Object[]) Array.newInstance(componentType, list.size());
    return list.toArray(o);
  }

  @Override
  protected Class<?> getComponentType(ResolvableMethodParameter parameter) {
    return parameter.getComponentType();
  }

  @Override
  protected Collection<Object> createCollection(MultiValueMap<String, PropertyValue> propertyValues, ResolvableMethodParameter parameter) {
    return new ArrayList<>(propertyValues.size());
  }
}
