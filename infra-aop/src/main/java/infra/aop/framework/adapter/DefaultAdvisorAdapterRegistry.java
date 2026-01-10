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

package infra.aop.framework.adapter;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import infra.aop.Advisor;
import infra.aop.AfterReturningAdvice;
import infra.aop.MethodBeforeAdvice;
import infra.aop.ThrowsAdvice;
import infra.aop.support.DefaultPointcutAdvisor;

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
