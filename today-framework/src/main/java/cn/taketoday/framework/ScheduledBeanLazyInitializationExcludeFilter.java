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

package cn.taketoday.framework;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import cn.taketoday.aop.AopInfrastructureBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.annotation.Scheduled;
import cn.taketoday.scheduling.annotation.Schedules;
import cn.taketoday.util.ClassUtils;

/**
 * A {@link LazyInitializationExcludeFilter} that detects bean methods annotated with
 * {@link Scheduled} or {@link Schedules}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 17:53
 */
class ScheduledBeanLazyInitializationExcludeFilter implements LazyInitializationExcludeFilter {

  private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

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
            && AnnotationUtils.isCandidateClass(targetType, Arrays.asList(Scheduled.class, Schedules.class))) {
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
