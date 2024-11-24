/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.annotation.config.task;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import infra.aop.AopInfrastructureBean;
import infra.app.LazyInitializationExcludeFilter;
import infra.beans.factory.config.BeanDefinition;
import infra.core.MethodIntrospector;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.annotation.AnnotationUtils;
import infra.scheduling.TaskScheduler;
import infra.scheduling.annotation.Scheduled;
import infra.scheduling.annotation.Schedules;
import infra.util.ClassUtils;

/**
 * A {@link LazyInitializationExcludeFilter} that detects bean methods annotated with
 * {@link Scheduled} or {@link Schedules}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 17:53
 */
class ScheduledBeanLazyInitializationExcludeFilter implements LazyInitializationExcludeFilter {

  private final Set<Class<?>> nonAnnotatedClasses = ConcurrentHashMap.newKeySet(64);

  ScheduledBeanLazyInitializationExcludeFilter() {
    // Ignore AOP infrastructure such as scoped proxies.
    this.nonAnnotatedClasses.add(AopInfrastructureBean.class);
    this.nonAnnotatedClasses.add(TaskScheduler.class);
    this.nonAnnotatedClasses.add(ScheduledExecutorService.class);
  }

  @Override
  public boolean isExcluded(String beanName, BeanDefinition beanDefinition, Class<?> beanType) {
    return hasScheduledTask(beanType);
  }

  private boolean hasScheduledTask(Class<?> type) {
    Class<?> targetType = ClassUtils.getUserClass(type);
    if (!this.nonAnnotatedClasses.contains(targetType)
            && AnnotationUtils.isCandidateClass(targetType, Scheduled.class, Schedules.class)) {
      Map<Method, Set<Scheduled>> annotatedMethods = MethodIntrospector.selectMethods(targetType,
              method -> {
                Set<Scheduled> scheduledAnnotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(
                        method, Scheduled.class, Schedules.class);
                return (!scheduledAnnotations.isEmpty() ? scheduledAnnotations : null);
              });
      if (annotatedMethods.isEmpty()) {
        this.nonAnnotatedClasses.add(targetType);
      }
      return !annotatedMethods.isEmpty();
    }
    return false;
  }

}
