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

package infra.transaction.annotation;

/**
 * An enum for global rollback-on behavior.
 *
 * <p>Note that the default behavior matches the traditional behavior in
 * EJB CMT and JTA, with the latter having rollback rules similar to Spring.
 * A global switch to trigger a rollback on any exception affects Spring's
 * {@link Transactional} as well as {@link jakarta.transaction.Transactional}
 * but leaves the non-rule-based {@link jakarta.ejb.TransactionAttribute} as-is.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EnableTransactionManagement#rollbackOn()
 * @see infra.transaction.interceptor.RuleBasedTransactionAttribute
 * @since 5.0
 */
public enum RollbackOn {

  /**
   * The default rollback-on behavior: rollback on
   * {@link RuntimeException RuntimeExceptions} as well as {@link Error Errors}.
   *
   * @see infra.transaction.interceptor.RollbackRuleAttribute#ROLLBACK_ON_RUNTIME_EXCEPTIONS
   */
  RUNTIME_EXCEPTIONS,

  /**
   * The alternative mode: rollback on all exceptions, including any checked
   * {@link Exception}.
   *
   * @see infra.transaction.interceptor.RollbackRuleAttribute#ROLLBACK_ON_ALL_EXCEPTIONS
   */
  ALL_EXCEPTIONS

}
