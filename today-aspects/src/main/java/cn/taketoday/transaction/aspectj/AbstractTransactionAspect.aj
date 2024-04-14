/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.transaction.aspectj;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.transaction.interceptor.TransactionAspectSupport;
import cn.taketoday.transaction.interceptor.TransactionAttributeSource;
import cn.taketoday.util.ExceptionUtils;

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
 * transactionally annotated, the relevant Spring transaction attribute
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
   * @param tas TransactionAttributeSource implementation, retrieving Spring
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
