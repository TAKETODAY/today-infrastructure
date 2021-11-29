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

package cn.taketoday.context.event;

import java.lang.reflect.Method;
import java.util.EventObject;

import cn.taketoday.aop.support.annotation.BeanSupplier;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Configuration;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Process @EventListener annotated on a method
 *
 * @author TODAY 2021/3/17 12:35
 */
@Configuration
public class MethodEventDrivenPostProcessor implements InitializationBeanPostProcessor {

  private final ConfigurableApplicationContext context;

  public MethodEventDrivenPostProcessor(ConfigurableApplicationContext context) {
    Assert.notNull(context, "ApplicationContext must not be null");
    this.context = context;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    Class<?> beanClass = bean.getClass();
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();

    ReflectionUtils.doWithMethods(beanClass, method -> {
      MergedAnnotations.from(method).stream(EventListener.class).forEach(eventListener -> {
        Class<?>[] eventTypes = getEventTypes(eventListener, method);
        String condition = eventListener.getString("condition");
        // use ContextUtils#resolveParameter to resolve method arguments
        addListener(beanName, beanFactory, method, condition, eventTypes); // FIXME bean has already exist?
      });
    });

    return bean;
  }

  protected Class<?>[] getEventTypes(MergedAnnotation<EventListener> eventListener, Method declaredMethod) {
    return getEventTypes(eventListener.getClassArray(MergedAnnotation.VALUE), declaredMethod);
  }

  protected Class<?>[] getEventTypes(Class<?>[] eventTypes, Method declaredMethod) {
    if (ObjectUtils.isNotEmpty(eventTypes)) {
      return eventTypes;
    }
    Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
    if (parameterTypes.length == 0) {
      throw new ConfigurationException("cannot determine event type on method: " + declaredMethod);
    }
    else if (parameterTypes.length == 1) {
      return new Class[] { parameterTypes[0] };
    }
    else {
      Class<?> eventType = null;
      for (Class<?> parameterType : parameterTypes) {
        // lookup EventObject
        if (EventObject.class.isAssignableFrom(parameterType)) {
          eventType = parameterType;
          break;
        }
      }
      if (eventType == null) {
        // fallback to first argument
        eventType = parameterTypes[0];
      }
      return new Class[] { eventType };
    }
  }

  protected void addListener(
          String beanName, ConfigurableBeanFactory beanFactory,
          Method declaredMethod, String condition, Class<?>... eventTypes) {

    MethodApplicationListener listener = new MethodApplicationListener(
            BeanSupplier.from(beanFactory, beanName),
            declaredMethod, eventTypes, beanFactory, context, condition);
    context.addApplicationListener(listener);
  }

}
