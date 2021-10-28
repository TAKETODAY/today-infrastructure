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

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

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
  public Object postProcessAfterInitialization(Object bean, BeanDefinition def) {
    Class<?> beanClass = def.getBeanClass();
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();

    ReflectionUtils.doWithMethods(beanClass, method -> {
      MergedAnnotations.from(method).stream(EventListener.class).forEach(eventListener -> {
        Class<?>[] eventTypes = getEventTypes(eventListener, method);
        // use ContextUtils#resolveParameter to resolve method arguments
        addListener(def, beanFactory, method, eventTypes);
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
          BeanDefinition def, ConfigurableBeanFactory beanFactory, Method declaredMethod, Class<?>... eventTypes) {
    ObjectSupplier<Object> beanSupplier = beanFactory.getObjectSupplier(def);
    MethodApplicationListener listener = new MethodApplicationListener(
            beanSupplier, declaredMethod, eventTypes, beanFactory);
    context.addApplicationListener(listener);
  }

  static final class MethodApplicationListener
          implements ApplicationListener<Object>, EventProvider {
    final Method targetMethod;
    final Class<?>[] eventTypes;
    final BeanFactory beanFactory;
    final MethodInvoker methodInvoker;
    final ObjectSupplier<Object> beanSupplier;
    final ArgumentsResolver argumentsResolver;

    MethodApplicationListener(
            ObjectSupplier<Object> beanSupplier,
            Method targetMethod, Class<?>[] eventTypes, BeanFactory beanFactory) {
      this.beanSupplier = beanSupplier;
      this.eventTypes = eventTypes;
      this.beanFactory = beanFactory;
      this.targetMethod = targetMethod;
      this.methodInvoker = MethodInvoker.fromMethod(targetMethod);
      this.argumentsResolver = targetMethod.getParameterCount() == 0
                               ? null
                               : beanFactory.getArgumentsResolver();
    }

    @Override
    public void onApplicationEvent(Object event) { // any event type
      Object[] parameter = resolveArguments(argumentsResolver, event);
      // native invoke public,protected,default method
      methodInvoker.invoke(beanSupplier.get(), parameter);
    }

    @Nullable
    private Object[] resolveArguments(ArgumentsResolver resolver, Object event) {
      if (resolver != null) {
        return resolver.resolve(targetMethod, beanFactory, new Object[] { event });
      }
      return null;
    }

    @Override
    public Class<?>[] getSupportedEvent() {
      return eventTypes;
    }
  }

}
