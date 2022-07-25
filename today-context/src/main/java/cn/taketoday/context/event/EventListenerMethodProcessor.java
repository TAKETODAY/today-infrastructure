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

package cn.taketoday.context.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.framework.autoproxy.AutoProxyUtils;
import cn.taketoday.aop.scope.ScopedObject;
import cn.taketoday.aop.scope.ScopedProxyUtils;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * Process @EventListener annotated on a method
 *
 * <p>
 * Registers {@link EventListener} methods as individual {@link ApplicationListener} instances.
 * Implements {@link BeanFactoryPostProcessor} primarily for early retrieval,
 * avoiding AOP checks for this processor bean and its {@link EventListenerFactory} delegates.
 *
 * </p>
 *
 * @author TODAY 2021/3/17 12:35
 * @see EventListenerFactory
 * @see DefaultEventListenerFactory
 */
public class EventListenerMethodProcessor
        implements BeanFactoryPostProcessor, SmartInitializingSingleton, ApplicationContextAware {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private List<EventListenerFactory> eventListenerFactories;

  private ConfigurableBeanFactory beanFactory;

  private final EventExpressionEvaluator evaluator = new EventExpressionEvaluator();

  private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

  @Nullable
  private ConfigurableApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext,
            "ApplicationContext does not implement ConfigurableApplicationContext");
    this.applicationContext = (ConfigurableApplicationContext) applicationContext;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;

    Map<String, EventListenerFactory> beans = beanFactory.getBeansOfType(EventListenerFactory.class, false, false);
    List<EventListenerFactory> factories = new ArrayList<>(beans.values());
    AnnotationAwareOrderComparator.sort(factories);
    this.eventListenerFactories = factories;
  }

  @Override
  public void afterSingletonsInstantiated() {
    ConfigurableBeanFactory beanFactory = this.beanFactory;
    Assert.state(beanFactory != null, "No ConfigurableBeanFactory set");
    Set<String> beanNames = beanFactory.getBeanNamesForType(Object.class);
    for (String beanName : beanNames) {
      if (ScopedProxyUtils.isScopedTarget(beanName)) {
        continue;
      }
      Class<?> type = null;
      try {
        type = AutoProxyUtils.determineTargetClass(beanFactory, beanName);
      }
      catch (Throwable ex) {
        // An unresolvable bean type, probably from a lazy bean - let's ignore it.
        if (logger.isDebugEnabled()) {
          logger.debug("Could not resolve target class for bean with name '{}'", beanName, ex);
        }
      }
      if (type != null) {
        if (ScopedObject.class.isAssignableFrom(type)) {
          try {
            Class<?> targetClass = AutoProxyUtils.determineTargetClass(
                    beanFactory, ScopedProxyUtils.getTargetBeanName(beanName));
            if (targetClass != null) {
              type = targetClass;
            }
          }
          catch (Throwable ex) {
            // An invalid scoped proxy arrangement - let's ignore it.
            if (logger.isDebugEnabled()) {
              logger.debug("Could not resolve target bean for scoped proxy '{}'", beanName, ex);
            }
          }
        }
        try {
          process(beanName, type);
        }
        catch (Throwable ex) {
          throw new BeanInitializationException("Failed to process @EventListener " +
                  "annotation on bean with name '" + beanName + "': " + ex.getMessage(), ex);
        }
      }
    }
  }

  private void process(final String beanName, final Class<?> targetType) {
    if (!this.nonAnnotatedClasses.contains(targetType)
            && AnnotationUtils.isCandidateClass(targetType, EventListener.class)) {
      Set<Method> annotatedMethods = null;
      try {
        annotatedMethods = MethodIntrospector.filterMethods(
                targetType, method -> AnnotatedElementUtils.hasAnnotation(method, EventListener.class));
      }
      catch (Throwable ex) {
        // An unresolvable type in a method signature, probably from a lazy bean - let's ignore it.
        if (logger.isDebugEnabled()) {
          logger.debug("Could not resolve methods for bean with name '{}'", beanName, ex);
        }
      }

      if (CollectionUtils.isEmpty(annotatedMethods)) {
        nonAnnotatedClasses.add(targetType);
        if (logger.isTraceEnabled()) {
          logger.trace("No @EventListener annotations found on bean class: {}", targetType.getName());
        }
      }
      else {
        // Non-empty set of methods
        ConfigurableApplicationContext context = this.applicationContext;
        Assert.state(context != null, "No ApplicationContext set");
        List<EventListenerFactory> factories = this.eventListenerFactories;
        Assert.state(factories != null, "EventListenerFactory List not initialized");
        for (Method method : annotatedMethods) {
          for (EventListenerFactory factory : factories) {
            if (factory.supportsMethod(method)) {
              Method methodToUse = AopUtils.selectInvocableMethod(method, context.getType(beanName));
              var listener = factory.createApplicationListener(beanName, targetType, methodToUse);
              if (listener instanceof ApplicationListenerMethodAdapter adapter) {
                adapter.init(context, evaluator);
              }
              context.addApplicationListener(listener);
              break;
            }
          }
        }
        if (logger.isDebugEnabled()) {
          logger.debug("{} @EventListener methods processed on bean '{}': {}",
                  annotatedMethods.size(), beanName, annotatedMethods);
        }
      }
    }
  }

}
