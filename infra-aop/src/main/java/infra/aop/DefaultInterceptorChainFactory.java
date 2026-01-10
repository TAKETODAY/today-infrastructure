/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import infra.aop.framework.Advised;
import infra.aop.framework.adapter.AdvisorAdapterRegistry;
import infra.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import infra.aop.support.RuntimeMethodInterceptor;
import infra.lang.Assert;
import infra.util.CollectionUtils;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultInterceptorChainFactory implements InterceptorChainFactory, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final DefaultInterceptorChainFactory INSTANCE = new DefaultInterceptorChainFactory();

  private AdvisorAdapterRegistry registry = DefaultAdvisorAdapterRegistry.getInstance();

  @Override
  public MethodInterceptor[] getInterceptors(Advised config, Method method, @Nullable Class<?> targetClass) {
    // This is somewhat tricky... We have to process introductions first,
    // but we need to preserve order in the ultimate list.
    Advisor[] advisors = config.getAdvisors();
    ArrayList<MethodInterceptor> interceptorList = new ArrayList<>(advisors.length);
    Class<?> actualClass = targetClass != null ? targetClass : method.getDeclaringClass();
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
            List<MethodInterceptor> interceptors = registry.getInterceptors(advisor);
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
          List<MethodInterceptor> interceptors = registry.getInterceptors(advisor);
          interceptorList.addAll(interceptors);
        }
      }
      else {
        List<MethodInterceptor> interceptors = registry.getInterceptors(advisor);
        interceptorList.addAll(interceptors);
      }
    }
    if (interceptorList.isEmpty()) {
      return EMPTY_INTERCEPTOR;
    }
    return interceptorList.toArray(EMPTY_INTERCEPTOR);
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
