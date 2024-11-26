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

package infra.test.context.jdbc.merging;

import infra.test.annotation.DirtiesContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.jdbc.AbstractTransactionalTests;
import infra.test.context.jdbc.EmptyDatabaseConfig;
import infra.test.context.jdbc.SqlMergeMode;

/**
 * Abstract base class for tests involving {@link SqlMergeMode @SqlMergeMode}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@DirtiesContext
public abstract class AbstractSqlMergeModeTests extends AbstractTransactionalTests {

}
