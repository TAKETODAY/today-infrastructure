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
 * JdbcTemplate variant with named parameter support.
 *
 * <p>NamedParameterJdbcTemplate is a wrapper around JdbcTemplate that adds
 * support for named parameter parsing. It does not implement the JdbcOperations
 * interface or extend JdbcTemplate, but implements the dedicated
 * NamedParameterJdbcOperations interface.
 *
 * <P>If you need the full power of Framework JDBC for less common operations, use
 * the {@code getJdbcOperations()} method of NamedParameterJdbcTemplate and
 * work with the returned classic template, or use a JdbcTemplate instance directly.
 */
@NonNullApi
@NonNullFields
package infra.jdbc.core.namedparam;

import infra.lang.NonNullApi;
import infra.lang.NonNullFields;
