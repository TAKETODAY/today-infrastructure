/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.framework.adapter;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.AfterReturningAdvice;
import cn.taketoday.aop.MethodBeforeAdvice;
import cn.taketoday.aop.ThrowsAdvice;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Keep track of a single instance so we can return it to classes that request it.
   */
  private static AdvisorAdapterRegistry instance = new DefaultAdvisorAdapterRegistry();

  private final ArrayList<AdvisorAdapter> adapters = new ArrayList<>(3);

  /**
   * Create a new DefaultAdvisorAdapterRegistry, registering well-known adapters.
   */
  public DefaultAdvisorAdapterRegistry() {
    registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
    registerAdvisorAdapter(new AfterReturningAdviceAdapter());
    registerAdvisorAdapter(new ThrowsAdviceAdapter());
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
  public List<MethodInterceptor> getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
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
    return interceptors;
  }

  @Override
  public void registerAdvisorAdapter(AdvisorAdapter adapter) {
    this.adapters.add(adapter);
  }

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
}
