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
package cn.taketoday.context.loader;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.annotation.DefaultProps;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ObjectUtils;

/**
 * Resolve {@link Map}
 *
 * @author TODAY <br>
 * 2019-10-28 20:27
 */
public class MapParameterResolver
        extends OrderedSupport implements ExecutableParameterResolver, Ordered {

  public MapParameterResolver() {
    this(Integer.MAX_VALUE);
  }

  public MapParameterResolver(int order) {
    super(order);
  }

  @Override
  public boolean supports(Parameter parameter) {
    return Map.class.isAssignableFrom(parameter.getType());
  }

  /**
   * 处理所有Map参数
   * <p>
   * 有 Props 就注入Properties
   * </p>
   *
   * @param parameter
   *         Target method {@link Parameter}
   * @param beanFactory
   *         {@link BeanFactory}
   */
  @Override
  @SuppressWarnings("unchecked")
  public Object resolve(Parameter parameter, BeanFactory beanFactory) {
    // 处理 Properties
    final Props props = getProps(parameter);
    if (props != null) {
      final Properties loadProps = ContextUtils.loadProps(props, System.getProperties());
      final Class<?> type = parameter.getType();

      if (type.isInterface()) { // extends or implements Map
        return loadProps;
      }

      final Map<Object, Object> ret = (Map<Object, Object>) ClassUtils.newInstance(type, beanFactory);
      ret.putAll(loadProps);

      return ret;
    }

    Class<?> beanClass = getBeanClass(ClassUtils.getGenericityClass(parameter));
    final Class<?> type = parameter.getType();
    if (type == Map.class) {
      return beanFactory.getBeansOfType(beanClass);
    }

    Map<Object, Object> ret = (Map<Object, Object>) ClassUtils.newInstance(type, beanFactory);
    ret.putAll(beanFactory.getBeansOfType(beanClass));
    return ret;
  }

  protected Class<?> getBeanClass(final Type[] genericityClass) {
    if (ObjectUtils.isNotEmpty(genericityClass)) {
      final Type beanType = genericityClass[1];
      if (beanType instanceof WildcardType) {
        return Object.class;
      }
      else if (beanType instanceof Class) {
        return (Class<?>) beanType;
      }
      throw new ConfigurationException("Not Support " + beanType);
    }
    return Object.class;
  }

  private Props getProps(Parameter parameter) {
    Props props = parameter.getAnnotation(Props.class);
    if (props == null && Properties.class.isAssignableFrom(parameter.getType())) {
      return new DefaultProps();
    }
    return props;
  }

}
