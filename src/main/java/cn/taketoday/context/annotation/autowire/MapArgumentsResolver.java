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
package cn.taketoday.context.annotation.autowire;

import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.ArgumentsResolvingContext;
import cn.taketoday.beans.ArgumentsResolvingStrategy;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.DefaultProps;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * Resolve {@link Map}
 *
 * @author TODAY 2019-10-28 20:27
 */
@SuppressWarnings("rawtypes")
public class MapArgumentsResolver
        extends NonNullBeanFactoryStrategy implements ArgumentsResolvingStrategy {

  @Override
  protected boolean supportsInternal(Parameter parameter, ArgumentsResolvingContext beanFactory) {
    return Map.class.isAssignableFrom(parameter.getType());
  }

  /**
   * 处理所有Map参数
   * <p>
   * 有 Props 就注入Properties
   * </p>
   *
   * @param parameter Target method {@link Parameter}
   */
  @Override
  protected Object resolveInternal(
          Parameter parameter, BeanFactory beanFactory, ArgumentsResolvingContext resolvingContext) {
    Class<?> type = parameter.getType();
    Map beansOfType = getBeansOfType(parameter, beanFactory);
    return convert(beansOfType, type);
  }

  protected Map getBeansOfType(Parameter parameter, BeanFactory beanFactory) {
    DefaultProps props = getProps(parameter);

    if (props != null) { // 处理 Properties
      PropsReader propsReader = new PropsReader();
      propsReader.setBeanFactory(beanFactory);
      return propsReader.readMap(props);
    }

    ResolvableType parameterType = ResolvableType.fromParameter(parameter);
    ResolvableType generic = parameterType.asMap().getGeneric(1);
    Class<?> beanClass = generic.toClass();
    return beanFactory.getBeansOfType(beanClass);
  }

  @SuppressWarnings("unchecked")
  protected Map convert(Map map, Class<?> type) {
    if (type != Map.class) {
      Map newMap = CollectionUtils.createMap(type, map.size());
      newMap.putAll(map);
      map = newMap;
    }
    return map;
  }

  private DefaultProps getProps(Parameter parameter) {
    AnnotationAttributes attributes = AnnotationUtils.getAttributes(Props.class, parameter);
    if (attributes == null) {
      if (Properties.class.isAssignableFrom(parameter.getType())) {
        return new DefaultProps();
      }
      return null;
    }
    else {
      return new DefaultProps(attributes);
    }
  }

}
