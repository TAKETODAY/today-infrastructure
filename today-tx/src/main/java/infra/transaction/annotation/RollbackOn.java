/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

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
 * @since 5.0
 * @see EnableTransactionManagement#rollbackOn()
 * @see org.springframework.transaction.interceptor.RuleBasedTransactionAttribute
 */
public enum RollbackOn {

	/**
	 * The default rollback-on behavior: rollback on
	 * {@link RuntimeException RuntimeExceptions} as well as {@link Error Errors}.
	 * @see org.springframework.transaction.interceptor.RollbackRuleAttribute#ROLLBACK_ON_RUNTIME_EXCEPTIONS
	 */
	RUNTIME_EXCEPTIONS,

	/**
	 * The alternative mode: rollback on all exceptions, including any checked
	 * {@link Exception}.
	 * @see org.springframework.transaction.interceptor.RollbackRuleAttribute#ROLLBACK_ON_ALL_EXCEPTIONS
	 */
	ALL_EXCEPTIONS

}
