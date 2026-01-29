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

package infra.transaction.aspectj;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import infra.beans.factory.DisposableBean;
import infra.transaction.interceptor.TransactionAspectSupport;
import infra.transaction.interceptor.TransactionAttributeSource;
import infra.util.ExceptionUtils;

/**
 * Abstract superaspect for AspectJ transaction aspects. Concrete
 * subaspects will implement the {@code transactionalMethodExecution()}
 * pointcut using a strategy such as Java 5 annotations.
 *
 * <p>Suitable for use inside or outside the Infra IoC container.
 * Set the "transactionManager" property appropriately, allowing
 * use of any transaction implementation supported by Infra.
 *
 * <p><b>NB:</b> If a method implements an interface that is itself
 * transactionally annotated, the relevant Infra transaction attribute
 * will <i>not</i> be resolved. This behavior will vary from that of Infra AOP
 * if proxying an interface (but not when proxying a class). We recommend that
 * transaction annotations should be added to classes, rather than business
 * interfaces, as they are an implementation detail rather than a contract
 * specification validation.
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract aspect AbstractTransactionAspect extends TransactionAspectSupport implements DisposableBean {

  /**
   * Construct the aspect using the given transaction metadata retrieval strategy.
   * @param tas TransactionAttributeSource implementation, retrieving Infra
   * transaction metadata for each joinpoint. Implement the subclass to pass in
   * {@code null} if it is intended to be configured through Setter Injection.
   */
  protected AbstractTransactionAspect(TransactionAttributeSource tas) {
    setTransactionAttributeSource(tas);
  }

  @Override
  public void destroy() {
    // An aspect is basically a singleton -> cleanup on destruction
    clearTransactionManagerCache();
  }

  @SuppressAjWarnings("adviceDidNotMatch")
  Object around(final Object txObject): transactionalMethodExecution(txObject) {
    MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
    // Adapt to TransactionAspectSupport's invokeWithinTransaction...
    try {
      return invokeWithinTransaction(methodSignature.getMethod(), txObject.getClass(), new InvocationCallback() {
        public Object proceedWithInvocation() throws Throwable {
          return proceed(txObject);
        }
      });
    }
    catch (RuntimeException | Error ex) {
      throw ex;
    }
    catch (Throwable thr) {
      throw ExceptionUtils.sneakyThrow(thr);
    }
  }

  /**
   * Concrete subaspects must implement this pointcut, to identify
   * transactional methods. For each selected joinpoint, TransactionMetadata
   * will be retrieved using Framework's TransactionAttributeSource interface.
   */
  protected abstract pointcut transactionalMethodExecution(Object txObject);

}
