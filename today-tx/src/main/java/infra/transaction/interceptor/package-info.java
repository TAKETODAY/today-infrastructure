/**
 * AOP-based solution for declarative transaction demarcation.
 * Builds on the AOP infrastructure in infra.aop.framework.
 * Any POJO can be transactionally advised with Framework.
 *
 * <p>The TransactionFactoryProxyBean can be used to create transactional
 * AOP proxies transparently to code that uses them.
 *
 * <p>The TransactionInterceptor is the AOP Alliance MethodInterceptor that
 * delivers transactional advice, based on the Framework transaction abstraction.
 * This allows declarative transaction management in any environment,
 * even without JTA if an application uses only a single database.
 */
@NonNullApi
@NonNullFields
package infra.transaction.interceptor;

import infra.lang.NonNullApi;
import infra.lang.NonNullFields;
