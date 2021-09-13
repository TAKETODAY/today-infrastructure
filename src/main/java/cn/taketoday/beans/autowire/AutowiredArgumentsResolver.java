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
package cn.taketoday.beans.autowire;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;

import cn.taketoday.beans.ArgumentsResolvingStrategy;
import cn.taketoday.beans.Autowired;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.ContextUtils;
import cn.taketoday.context.Props;
import cn.taketoday.core.NonNull;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.Required;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * Resolve {@link Autowired} on {@link Parameter}
 *
 * @author TODAY 2019-10-28 20:27
 */
public class AutowiredArgumentsResolver
        extends NonNullBeanFactoryStrategy implements ArgumentsResolvingStrategy, Ordered {

  public AutowiredArgumentsResolver() {
    setOrder(LOWEST_PRECEDENCE);
  }

  @Override
  public boolean supportsInternal(Parameter parameter, @NonNull BeanFactory beanFactory) {
    return true;
  }

  @Override
  public final Object resolveInternal(
          Parameter parameter, @NonNull BeanFactory beanFactory) {
    final Autowired autowired = parameter.getAnnotation(Autowired.class); // @Autowired on parameter
    Object bean = resolveBean(autowired != null ? autowired.value() : null, parameter.getType(), beanFactory);
    // @Props on a bean (pojo) which has already annotated @Autowired or not
    if (parameter.isAnnotationPresent(Props.class)) {
      bean = resolvePropsInternal(parameter, parameter.getAnnotation(Props.class), bean);
    }
    if (bean == null && isRequired(parameter, autowired)) { // if it is required
      final NoSuchBeanDefinitionException noSuchBean = new NoSuchBeanDefinitionException(parameter.getType());
      LoggerFactory.getLogger(AutowiredArgumentsResolver.class)//
              .error("[{}] on executable: [{}] is required and there isn't a [{}] bean",
                     parameter, parameter.getDeclaringExecutable(), parameter.getType(), noSuchBean);
      throw noSuchBean;
    }
    return bean;
  }

  // @since 3.0 Required
  public static boolean isRequired(AnnotatedElement element, Autowired autowired) {
    return (autowired == null || autowired.required())
            || AnnotationUtils.isPresent(element, Required.class);
  }

  protected Object resolveBean(final String name, final Class<?> type, final BeanFactory beanFactory) {
    if (StringUtils.isNotEmpty(name)) {
      // use name and bean type to get bean
      return beanFactory.getBean(name, type);
    }
    return beanFactory.getBean(type);
  }

  protected Object resolvePropsInternal(final Parameter parameter, final Props props, final Object bean) {
    if (bean != null) {
      return ContextUtils.resolveProps(props, bean, ContextUtils.loadProps(props, System.getProperties()));
    }
    return ContextUtils.resolveProps(props, parameter.getType(), ContextUtils.loadProps(props, System.getProperties()));
  }

}
