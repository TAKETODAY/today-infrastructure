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
 * The classes in this package represent RDBMS queries, updates,
 * and stored procedures as threadsafe, reusable objects. This approach
 * is modelled by JDO, although of course objects returned by queries
 * are "disconnected" from the database.
 *
 * <p>This higher level of JDBC abstraction depends on the lower-level
 * abstraction in the {@code infra.jdbc.core} package.
 * Exceptions thrown are as in the {@code infra.dao} package,
 * meaning that code using this package does not need to implement JDBC or
 * RDBMS-specific error handling.
 *
 * <p>This package and related packages are discussed in Chapter 9 of
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 */
@NonNullApi
@NonNullFields
package infra.jdbc.object;

import infra.lang.NonNullApi;
import infra.lang.NonNullFields;
