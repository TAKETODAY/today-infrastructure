/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.event;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import infra.aop.framework.autoproxy.AutoProxyUtils;
import infra.aop.scope.ScopedObject;
import infra.aop.scope.ScopedProxyUtils;
import infra.aop.support.AopUtils;
import infra.beans.factory.BeanInitializationException;
import infra.beans.factory.SmartInitializingSingleton;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.context.expression.BeanFactoryResolver;
import infra.core.MethodIntrospector;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.annotation.AnnotationUtils;
import infra.expression.spel.support.StandardEvaluationContext;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;

/**
 * Process @EventListener annotated on a method
 *
 * <p>
 * Registers {@link EventListener} methods as individual {@link ApplicationListener} instances.
 * Implements {@link BeanFactoryPostProcessor} primarily for early retrieval,
 * avoiding AOP checks for this processor bean and its {@link EventListenerFactory} delegates.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EventListenerFactory
 * @see DefaultEventListenerFactory
 * @since 2021/3/17 12:35
 */
public class EventListenerMethodProcessor implements SmartInitializingSingleton, ApplicationContextAware {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private ConfigurableApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext,
            "ApplicationContext does not implement ConfigurableApplicationContext");
    this.applicationContext = (ConfigurableApplicationContext) applicationContext;
  }

  @Override
  public void afterSingletonsInstantiated(ConfigurableBeanFactory beanFactory) {
    Set<Class<?>> nonAnnotatedClasses = new HashSet<>();

    var factories = beanFactory.getBeans(EventListenerFactory.class);
    AnnotationAwareOrderComparator.sort(factories);
    StandardEvaluationContext shared = new StandardEvaluationContext();
    shared.setBeanResolver(new BeanFactoryResolver(beanFactory));
    EventExpressionEvaluator evaluator = new EventExpressionEvaluator(shared);

    var beanNames = beanFactory.getBeanNamesForType(Object.class);
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
        logger.debug("Could not resolve target class for bean with name '{}'", beanName, ex);
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
            logger.debug("Could not resolve target bean for scoped proxy '{}'", beanName, ex);
          }
        }
        try {
          process(beanName, type, factories, evaluator, nonAnnotatedClasses);
        }
        catch (Throwable ex) {
          throw new BeanInitializationException("Failed to process @EventListener annotation on bean with name '%s': %s"
                  .formatted(beanName, ex.getMessage()), ex);
        }
      }
    }
  }

  private void process(String beanName, Class<?> targetType,
          List<EventListenerFactory> factories, EventExpressionEvaluator evaluator, Set<Class<?>> nonAnnotatedClasses) {

    if (!nonAnnotatedClasses.contains(targetType)
            && AnnotationUtils.isCandidateClass(targetType, EventListener.class)
            && !isInfraContainerClass(targetType)) {

      Set<Method> annotatedMethods = null;
      try {
        annotatedMethods = MethodIntrospector.filterMethods(
                targetType, method -> AnnotatedElementUtils.hasAnnotation(method, EventListener.class));
      }
      catch (Throwable ex) {
        // An unresolvable type in a method signature, probably from a lazy bean - let's ignore it.
        logger.debug("Could not resolve methods for bean with name '{}'", beanName, ex);
      }

      if (CollectionUtils.isEmpty(annotatedMethods)) {
        nonAnnotatedClasses.add(targetType);
        logger.trace("No @EventListener annotations found on bean class: {}", targetType.getName());
      }
      else {
        // Non-empty set of methods
        ConfigurableApplicationContext context = this.applicationContext;
        Assert.state(context != null, "No ApplicationContext set");
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

  /**
   * Determine whether the given class is an {@code infra}
   * bean class that is not annotated as a user or test {@link Component}...
   * which indicates that there is no {@link EventListener} to be found there.
   */
  private static boolean isInfraContainerClass(Class<?> clazz) {
    return clazz.getName().startsWith("infra.")
            && !AnnotatedElementUtils.isAnnotated(ClassUtils.getUserClass(clazz), Component.class);
  }

}
