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

/**
 * AspectJ integration package. Includes Framework AOP advice implementations for AspectJ 5
 * annotation-style methods, and an AspectJExpressionPointcut: a Framework AOP Pointcut
 * implementation that allows use of the AspectJ pointcut expression language with the Framework AOP
 * runtime framework.
 *
 * <p>Note that use of this package does <i>not</i> require the use of the {@code ajc} compiler
 * or AspectJ load-time weaver. It is intended to enable the use of a valuable subset of AspectJ
 * functionality, with consistent semantics, with the proxy-based Framework AOP framework.
 */
@NonNullApi
@NonNullFields
package infra.aop.aspectj;

import infra.lang.NonNullApi;
import infra.lang.NonNullFields;