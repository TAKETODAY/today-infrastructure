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

package cn.taketoday.aop;

import org.aopalliance.intercept.MethodInterceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import cn.taketoday.aop.framework.adapter.AdvisorAdapterRegistry;
import cn.taketoday.aop.support.RuntimeMethodInterceptor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DefaultInterceptorChainFactory implements InterceptorChainFactory, Serializable {
  private AdvisorAdapterRegistry registry = DefaultAdvisorAdapterRegistry.getInstance();

  @Override
  public MethodInterceptor[] getInterceptorsAndDynamicInterceptionAdvice(
          Advised config, Method method, @Nullable Class<?> targetClass) {

    // This is somewhat tricky... We have to process introductions first,
    // but we need to preserve order in the ultimate list.
    Advisor[] advisors = config.getAdvisors();
    ArrayList<MethodInterceptor> interceptorList = new ArrayList<>(advisors.length);
    Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
    Boolean hasIntroductions = null;

    for (Advisor advisor : advisors) {
      if (advisor instanceof PointcutAdvisor pointcutAdvisor) {
        // Add it conditionally.
        if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
          MethodMatcher matcher = pointcutAdvisor.getPointcut().getMethodMatcher();
          boolean match;
          if (matcher instanceof IntroductionAwareMethodMatcher) {
            if (hasIntroductions == null) {
              hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
            }
            match = ((IntroductionAwareMethodMatcher) matcher).matches(method, actualClass, hasIntroductions);
          }
          else {
            match = matcher.matches(method, actualClass);
          }
          if (match) {
            MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
            if (matcher.isRuntime()) {
              // Creating a new object instance in the getInterceptors() method
              // isn't a problem as we normally cache created chains.
              for (MethodInterceptor interceptor : interceptors) {
                interceptorList.add(new RuntimeMethodInterceptor(interceptor, matcher));
              }
            }
            else {
              CollectionUtils.addAll(interceptorList, interceptors);
            }
          }
        }
      }
      else if (advisor instanceof IntroductionAdvisor ia) {
        if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
          MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
          Collections.addAll(interceptorList, interceptors);
        }
      }
      else {
        MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
        Collections.addAll(interceptorList, interceptors);
      }
    }
    if (interceptorList.isEmpty()) {
      return AdvisorAdapterRegistry.EMPTY_INTERCEPTOR;
    }
    return interceptorList.toArray(AdvisorAdapterRegistry.EMPTY_INTERCEPTOR);
  }

  /**
   * Determine whether the Advisors contain matching introductions.
   */
  private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
    for (Advisor advisor : advisors) {
      if (advisor instanceof IntroductionAdvisor ia && ia.getClassFilter().matches(actualClass)) {
        return true;
      }
    }
    return false;
  }

  public void setRegistry(AdvisorAdapterRegistry registry) {
    Assert.notNull(registry, "AdvisorAdapterRegistry is required");
    this.registry = registry;
  }

  public AdvisorAdapterRegistry getRegistry() {
    return registry;
  }
}
