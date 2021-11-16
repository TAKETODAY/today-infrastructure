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
package cn.taketoday.context.autowire;

import cn.taketoday.beans.ArgumentsResolvingContext;
import cn.taketoday.beans.ArgumentsResolvingStrategy;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.NullValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Required;
import cn.taketoday.util.StringUtils;

import java.lang.reflect.Parameter;

/**
 * Resolve {@link Autowired} on {@link Parameter}
 *
 * @author TODAY 2019-10-28 20:27
 * @see Required
 */
public class AutowiredArgumentsResolver implements ArgumentsResolvingStrategy {

  @Nullable
  @Override
  public Object resolveArgument(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
    BeanFactory beanFactory = resolvingContext.getBeanFactory();
    if (beanFactory != null) {
      MergedAnnotations annotations = MergedAnnotations.from(parameter);
      MergedAnnotation<Autowired> autowired = annotations.get(Autowired.class); // @Autowired on parameter
      Object bean = resolveBean(autowired, parameter.getType(), beanFactory);
      // @Props on a bean (pojo) which has already annotated @Autowired or not
      if (parameter.isAnnotationPresent(Props.class)) {
        bean = resolvePropsInternal(parameter, parameter.getAnnotation(Props.class), bean);
      }
      if (bean == null) {
        if (AutowiredPropertyResolver.isRequired(parameter, autowired)) { // if it is required
          throw new NoSuchBeanDefinitionException(
                  "[" + parameter + "] on executable: [" + parameter.getDeclaringExecutable()
                          + "] is required and there isn't a [" + parameter.getType() + "] bean", (Throwable) null);
        }
        return NullValue.INSTANCE; // not required
      }
      return bean;
    }
    return null; // next resolver
  }

  protected Object resolveBean(
          MergedAnnotation<Autowired> autowired, Class<?> type, BeanFactory beanFactory) {
    if (autowired.isPresent()) {
      String name = autowired.getString(MergedAnnotation.VALUE);
      if (StringUtils.isNotEmpty(name)) {
        // use name and bean type to get bean
        return beanFactory.getBean(name, type);
      }
    }
    return beanFactory.getBean(type);
  }

  protected Object resolvePropsInternal(Parameter parameter, Props props, @Nullable Object bean) {
    PropsReader propsReader = new PropsReader();
    if (bean != null) {
      return propsReader.read(props, bean);
    }
    return propsReader.read(props, parameter.getType());
  }

}
