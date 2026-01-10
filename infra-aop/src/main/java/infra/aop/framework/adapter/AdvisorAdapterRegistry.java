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

import org.aopalliance.intercept.MethodInterceptor;

import java.util.List;

import infra.aop.Advisor;
import infra.aop.AfterReturningAdvice;
import infra.aop.MethodBeforeAdvice;
import infra.aop.PointcutAdvisor;
import infra.aop.ThrowsAdvice;

/**
 * Interface for registries of Advisor adapters.
 *
 * <p><i>This is an SPI interface, not to be implemented by any user.</i>
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public interface AdvisorAdapterRegistry {

  /**
   * Return an {@link Advisor} wrapping the given advice.
   * <p>Should by default at least support
   * {@link org.aopalliance.intercept.MethodInterceptor},
   * {@link MethodBeforeAdvice},
   * {@link AfterReturningAdvice},
   * {@link ThrowsAdvice}.
   *
   * @param advice an object that should be an advice
   * @return an Advisor wrapping the given advice (never {@code null};
   * if the advice parameter is an Advisor, it is to be returned as-is)
   * @throws UnknownAdviceTypeException if no registered advisor adapter
   * can wrap the supposed advice
   */
  Advisor wrap(Object advice) throws UnknownAdviceTypeException;

  /**
   * Return an array of AOP Alliance MethodInterceptors to allow use of the
   * given Advisor in an interception-based framework.
   * <p>Don't worry about the pointcut associated with the {@link Advisor}, if it is
   * a {@link PointcutAdvisor}: just return an interceptor.
   *
   * @param advisor the Advisor to find an interceptor for
   * @return a list of MethodInterceptors to expose this Advisor's behavior
   * @throws UnknownAdviceTypeException if the Advisor type is
   * not understood by any registered AdvisorAdapter
   */
  List<MethodInterceptor> getInterceptors(Advisor advisor) throws UnknownAdviceTypeException;

  /**
   * Register the given {@link AdvisorAdapter}. Note that it is not necessary to register
   * adapters for an AOP Alliance Interceptors or Advices: these must be
   * automatically recognized by an {@code AdvisorAdapterRegistry} implementation.
   *
   * @param adapter an AdvisorAdapter that understands particular Advisor or Advice types
   */
  void registerAdvisorAdapter(AdvisorAdapter adapter);

}
