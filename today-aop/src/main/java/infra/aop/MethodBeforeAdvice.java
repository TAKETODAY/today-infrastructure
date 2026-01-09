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

import org.aopalliance.intercept.MethodInvocation;

/**
 * Advice invoked before a method is invoked. Such advices cannot
 * prevent the method call proceeding, unless they throw a Throwable.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/20 22:16
 * @see AfterReturningAdvice
 * @see ThrowsAdvice
 * @since 3.0
 */
public interface MethodBeforeAdvice extends BeforeAdvice {

  /**
   * Callback before a given method is invoked.
   *
   * @param invocation the method invocation join-point
   * @throws Throwable if this object wishes to abort the call.
   * Any exception thrown will be returned to the caller if it's
   * allowed by the method signature. Otherwise the exception
   * will be wrapped as a runtime exception.
   */
  void before(MethodInvocation invocation) throws Throwable;

}
