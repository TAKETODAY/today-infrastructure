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
 * This package builds on the beans package to add support for
 * message sources and for the Observer design pattern, and the
 * ability for application objects to obtain resources using a
 * consistent API.
 *
 * <p>There is no necessity for Infra applications to depend
 * on ApplicationContext or even BeanFactory functionality
 * explicitly. One of the strengths of the Infra architecture
 * is that application objects can often be configured without
 * any dependency on Infra-specific APIs.
 */
@NullMarked
package infra.context;

import org.jspecify.annotations.NullMarked;