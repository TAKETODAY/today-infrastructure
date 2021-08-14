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

package cn.taketoday.context.event;

import java.lang.reflect.Method;
import java.util.EventObject;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.core.utils.AnnotationUtils;
import cn.taketoday.core.utils.ContextUtils;
import cn.taketoday.core.utils.ObjectUtils;
import cn.taketoday.core.utils.ReflectionUtils;

/**
 * Process @EventListener annotated on a method
 *
 * @author TODAY 2021/3/17 12:35
 */
public class MethodEventDrivenPostProcessor implements BeanPostProcessor {
  private final ConfigurableApplicationContext context;

  public MethodEventDrivenPostProcessor(ConfigurableApplicationContext context) {
    Assert.notNull(context, "ApplicationContext must not be null");
    this.context = context;
  }

  @Override
  public Object postProcessAfterInitialization(final Object bean, final BeanDefinition def) {
    final Class<?> beanClass = def.getBeanClass();
    final ConfigurableBeanFactory beanFactory = context.getBeanFactory();
    final Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(beanClass);
    for (final Method declaredMethod : declaredMethods) {
      if (AnnotationUtils.isPresent(declaredMethod, EventListener.class)) {
        final AnnotationAttributes[] attributes
                = AnnotationUtils.getAttributesArray(declaredMethod, EventListener.class);
        for (final AnnotationAttributes attribute : attributes) {
          final Class<?>[] eventTypes = getEventTypes(attribute, declaredMethod);
          // use ContextUtils#resolveParameter to resolve method arguments
          addListener(bean, beanFactory, declaredMethod, eventTypes);
        }
      }
    }

    return bean;
  }

  protected Class<?>[] getEventTypes(AnnotationAttributes attribute, Method declaredMethod) {
    final Class<?>[] eventTypes = attribute.getClassArray(Constant.VALUE);

    if (ObjectUtils.isNotEmpty(eventTypes)) {
      return eventTypes;
    }
    final Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
    if (parameterTypes.length == 0) {
      throw new ConfigurationException("cannot determine event type on method: " + declaredMethod);
    }
    else if (parameterTypes.length == 1) {
      return new Class[] { parameterTypes[0] };
    }
    else {
      Class<?> eventType = null;
      for (final Class<?> parameterType : parameterTypes) {
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
          Object bean, ConfigurableBeanFactory beanFactory, Method declaredMethod, Class<?>... eventTypes) {
    final MethodApplicationListener listener
            = new MethodApplicationListener(bean, declaredMethod, eventTypes, beanFactory);

    context.addApplicationListener(listener);
  }

  static final class MethodApplicationListener
          implements ApplicationListener<Object>, ApplicationEventCapable {
    final Object bean;
    final Method targetMethod;
    final Class<?>[] eventTypes;
    final BeanFactory beanFactory;
    final MethodInvoker methodInvoker;

    MethodApplicationListener(
            Object bean, Method targetMethod, Class<?>[] eventTypes, BeanFactory beanFactory) {
      this.bean = bean;
      this.eventTypes = eventTypes;
      this.beanFactory = beanFactory;
      this.targetMethod = targetMethod;
      this.methodInvoker = MethodInvoker.create(targetMethod);
    }

    @Override
    public void onApplicationEvent(final Object event) { // any event type
      final Object[] parameter
              = ContextUtils.resolveParameter(targetMethod, beanFactory, new Object[] { event });
      // native invoke public,protected,default method
      methodInvoker.invoke(bean, parameter);
    }

    @Override
    public Class<?>[] getApplicationEvent() {
      return eventTypes;
    }
  }

}
