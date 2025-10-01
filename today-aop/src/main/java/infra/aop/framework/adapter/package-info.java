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
 * SPI package allowing AOP framework to handle arbitrary advice types.
 *
 * <p>Users who want merely to <i>use</i> the AOP framework, rather than extend
 * its capabilities, don't need to concern themselves with this package.
 *
 * <p>You may wish to use these adapters to wrap Framework-specific advices, such as MethodBeforeAdvice,
 * in MethodInterceptor, to allow their use in another AOP framework supporting the AOP Alliance interfaces.
 *
 * <p>These adapters do not depend on any other framework classes to allow such usage.
 */
@NullMarked
package infra.aop.framework.adapter;

import org.jspecify.annotations.NullMarked;
