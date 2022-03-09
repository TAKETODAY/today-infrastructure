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

package cn.taketoday.aop.framework;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;
import java.util.ArrayList;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.AfterReturningAdvice;
import cn.taketoday.aop.MethodBeforeAdvice;
import cn.taketoday.aop.ThrowsAdvice;
import cn.taketoday.aop.proxy.AdvisorAdapterRegistry;
import cn.taketoday.aop.proxy.UnknownAdviceTypeException;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.annotation.AfterReturning;
import cn.taketoday.aop.support.annotation.AfterThrowing;
import cn.taketoday.aop.support.annotation.Before;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Default implementation of the {@link AdvisorAdapterRegistry} interface.
 * Supports {@link org.aopalliance.intercept.MethodInterceptor},
 * {@link MethodBeforeAdvice},
 * {@link AfterReturningAdvice},
 * {@link ThrowsAdvice}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {

  /**
   * Keep track of a single instance so we can return it to classes that request it.
   */
  private static AdvisorAdapterRegistry instance = new DefaultAdvisorAdapterRegistry();

  private final ArrayList<AdvisorAdapter> adapters = new ArrayList<>(3);

  /**
   * Create a new DefaultAdvisorAdapterRegistry, registering well-known adapters.
   */
  public DefaultAdvisorAdapterRegistry() {
    addAdvisorAdapters(
            new BeforeAdvisorAdapter(),
            new ThrowsAdviceAdvisorAdapter(),
            new AfterReturningAdvisorAdapter()
    );
  }

  @Override
  public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
    if (adviceObject instanceof Advisor) {
      return (Advisor) adviceObject;
    }
    if (!(adviceObject instanceof Advice advice)) {
      throw new UnknownAdviceTypeException(adviceObject);
    }
    if (advice instanceof MethodInterceptor) {
      // So well-known it doesn't even need an adapter.
      return new DefaultPointcutAdvisor(advice);
    }
    for (AdvisorAdapter adapter : this.adapters) {
      // Check that it is supported.
      if (adapter.supportsAdvice(advice)) {
        return new DefaultPointcutAdvisor(advice);
      }
    }
    throw new UnknownAdviceTypeException(advice);
  }

  @Override
  public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
    ArrayList<MethodInterceptor> interceptors = new ArrayList<>(3);
    Advice advice = advisor.getAdvice();
    if (advice instanceof MethodInterceptor) {
      interceptors.add((MethodInterceptor) advice);
    }
    for (AdvisorAdapter adapter : this.adapters) {
      if (adapter.supportsAdvice(advice)) {
        interceptors.add(adapter.getInterceptor(advisor));
      }
    }
    if (interceptors.isEmpty()) {
      throw new UnknownAdviceTypeException(advisor.getAdvice());
    }
    return interceptors.toArray(EMPTY_INTERCEPTOR);
  }

  @Override
  public void registerAdvisorAdapter(AdvisorAdapter adapter) {
    this.adapters.add(adapter);
  }

  /**
   * Add {@link AdvisorAdapter} to {@link #adapters} and sort them
   *
   * @param adapters new AdvisorAdapters
   */
  public void addAdvisorAdapters(@Nullable AdvisorAdapter... adapters) {
    if (ObjectUtils.isNotEmpty(adapters)) {
      CollectionUtils.addAll(this.adapters, adapters);
      CollectionUtils.trimToSize(this.adapters);
      AnnotationAwareOrderComparator.sort(this.adapters);
    }
  }

  //---------------------------------------------------------------------
  // static
  //---------------------------------------------------------------------

  /**
   * Return the singleton {@link DefaultAdvisorAdapterRegistry} instance.
   */
  public static AdvisorAdapterRegistry getInstance() {
    return instance;
  }

  /**
   * Reset the singleton {@link DefaultAdvisorAdapterRegistry}, removing any
   * {@link AdvisorAdapterRegistry#registerAdvisorAdapter(AdvisorAdapter) registered}
   * adapters.
   */
  static void reset() {
    instance = new DefaultAdvisorAdapterRegistry();
  }

  //---------------------------------------------------------------------
  // Implementations of AdvisorAdapter
  //---------------------------------------------------------------------

  static final class BeforeAdvisorAdapter implements AdvisorAdapter, Serializable {

    @Override
    public boolean supportsAdvice(Advice advice) {
      return advice instanceof MethodBeforeAdvice;
    }

    @Override
    public MethodInterceptor getInterceptor(Advisor advisor) {
      final MethodBeforeAdvice advice = (MethodBeforeAdvice) advisor.getAdvice();
      final class Interceptor implements MethodInterceptor, Ordered {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
          advice.before(invocation);
          return invocation.proceed();
        }

        @Override
        public int getOrder() {
          return Before.DEFAULT_ORDER;
        }
      }
      return new Interceptor();
    }
  }

  static final class AfterReturningAdvisorAdapter implements AdvisorAdapter, Serializable {

    @Override
    public boolean supportsAdvice(Advice advice) {
      return advice instanceof AfterReturningAdvice;
    }

    @Override
    public MethodInterceptor getInterceptor(Advisor advisor) {
      final AfterReturningAdvice advice = (AfterReturningAdvice) advisor.getAdvice();
      final class Interceptor implements MethodInterceptor, Ordered {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
          final Object returnValue = invocation.proceed();
          advice.afterReturning(returnValue, invocation);
          return returnValue;
        }

        @Override
        public int getOrder() {
          return AfterReturning.DEFAULT_ORDER;
        }
      }
      return new Interceptor();
    }
  }

  /**
   * {@link ThrowsAdvice} Adapter
   */
  static final class ThrowsAdviceAdvisorAdapter implements AdvisorAdapter, Serializable {

    @Override
    public boolean supportsAdvice(Advice advice) {
      return advice instanceof ThrowsAdvice;
    }

    @Override
    public MethodInterceptor getInterceptor(Advisor advisor) {
      final ThrowsAdvice advice = (ThrowsAdvice) advisor.getAdvice();
      final class Interceptor implements MethodInterceptor, Ordered {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
          try {
            return invocation.proceed();
          }
          catch (Throwable ex) {
            return advice.afterThrowing(ex, invocation);
          }
        }

        @Override
        public int getOrder() {
          return AfterThrowing.DEFAULT_ORDER;
        }
      }

      return new Interceptor();
    }
  }

}
