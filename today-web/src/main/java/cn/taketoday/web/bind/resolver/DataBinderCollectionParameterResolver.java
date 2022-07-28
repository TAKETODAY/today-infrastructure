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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.bind.RequestContextDataBinder;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * resolve collection parameter
 *
 * <pre>
 *  class UserForm {
 *     int age;
 *
 *     String name;
 *
 *     String[] arr;
 *
 *     List<String> stringList;
 *
 *     Map<String, Integer> map;
 *  }
 *
 *  void test(List<UserForm> userList) { }
 *
 * </pre>
 *
 * @author TODAY 2021/4/8 14:33
 * @see <a href='https://taketoday.cn/articles/1616819014712'>TODAY Context 之 BeanPropertyAccessor</a>
 * @since 3.0
 */
@Deprecated
public class DataBinderCollectionParameterResolver extends AbstractDataBinderParameterResolver {

  private int maxValueIndex = 500;

  @Override
  protected boolean supportsInternal(ResolvableMethodParameter resolvable) {
    Class<?> parameterType = resolvable.getParameterType();
    if (CollectionUtils.isCollection(parameterType)) {
      ResolvableType generic = resolvable.getResolvableType().asCollection().getGeneric(0);
      Class<?> valueType = generic.resolve();
      if (valueType != null) {
        return supportsSetProperties(valueType);
      }
    }
    return false;
  }

  /**
   * @return Collection object
   * @throws ParameterIndexExceededException {@code valueIndex} exceed {@link #maxValueIndex}
   * @throws ParameterFormatException {@code valueIndex} number format error
   * @see #createCollection(MultiValueMap, ResolvableMethodParameter)
   */
  @Override
  @SuppressWarnings({ "rawtypes" })
  protected Object doBind(MultiValueMap<String, PropertyValue> propertyValues, ResolvableMethodParameter resolvable) {
    Collection<Object> collection = createCollection(propertyValues, resolvable);
    boolean isList = collection instanceof List;

    int maxValueIndex = getMaxValueIndex();
    Class<?> parameterClass = getComponentType(resolvable);
    BeanMetadata parameterMetadata = BeanMetadata.from(parameterClass);

    for (Map.Entry<String, List<PropertyValue>> entry : propertyValues.entrySet()) {
      Object rootObject = parameterMetadata.newInstance();
      RequestContextDataBinder dataBinder = new RequestContextDataBinder(rootObject, resolvable.getName());
      dataBinder.setAutoGrowCollectionLimit(maxValueIndex);
      List<PropertyValue> propertyValueList = entry.getValue();
      dataBinder.bind(new PropertyValues(propertyValueList));

      if (isList) {
        try {
          String key = entry.getKey();
          int valueIndex = Integer.parseInt(key);
          if (valueIndex > maxValueIndex) {
            throw new ParameterIndexExceededException(resolvable.getParameter());
          }
          CollectionUtils.setValue((List) collection, valueIndex, rootObject);
        }
        catch (NumberFormatException e) {
          throw new ParameterFormatException(resolvable.getParameter(), e);
        }
      }
      else {
        collection.add(rootObject);
      }
    }
    return collection;
  }

  /**
   * create {@link Collection} object
   */
  protected Collection<Object> createCollection(MultiValueMap<String, PropertyValue> propertyValues, ResolvableMethodParameter parameter) {
    return CollectionUtils.createCollection(parameter.getParameterType(), propertyValues.size());
  }

  protected Class<?> getComponentType(ResolvableMethodParameter resolvable) {
    return resolvable.getResolvableType().asCollection().getGeneric(0).resolve();
  }

  public void setMaxValueIndex(int maxValueIndex) {
    this.maxValueIndex = maxValueIndex;
  }

  public int getMaxValueIndex() {
    return maxValueIndex;
  }

}
