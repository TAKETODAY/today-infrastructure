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

package infra.scripting.groovy;

import org.aopalliance.intercept.MethodInvocation;

import infra.aop.MethodBeforeAdvice;
import infra.aop.ThrowsAdvice;

public class LogUserAdvice implements MethodBeforeAdvice, ThrowsAdvice {

  private int countBefore = 0;

  private int countThrows = 0;

  @Override
  public void before(MethodInvocation invocation) throws Throwable {
    countBefore++;
    // System.out.println("Method:" + method.getName());
  }

  public Object afterThrowing(Throwable ex) throws Throwable {
    countThrows++;
    // System.out.println("***********************************************************************************");
    // System.out.println("Exception caught:");
    // System.out.println("***********************************************************************************");
    // e.printStackTrace();
    throw ex;
  }

  public int getCountBefore() {
    return countBefore;
  }

  public int getCountThrows() {
    return countThrows;
  }

  public void reset() {
    countThrows = 0;
    countBefore = 0;
  }

}
