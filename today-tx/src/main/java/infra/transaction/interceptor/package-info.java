/*
 * Copyright 2017 - 2025 the original author or authors.
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
@NullMarked
package infra.transaction.interceptor;

import org.jspecify.annotations.NullMarked;
