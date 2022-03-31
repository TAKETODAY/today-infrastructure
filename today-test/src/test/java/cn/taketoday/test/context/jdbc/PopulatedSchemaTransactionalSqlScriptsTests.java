/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.jdbc;

import org.junit.jupiter.api.Test;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;

/**
 * Transactional integration tests that verify rollback semantics for
 * {@link Sql @Sql} support.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@JUnitConfig(PopulatedSchemaDatabaseConfig.class)
@DirtiesContext
class PopulatedSchemaTransactionalSqlScriptsTests extends AbstractTransactionalTests {

	@BeforeTransaction
	@AfterTransaction
	void verifyPreAndPostTransactionDatabaseState() {
		assertNumUsers(0);
	}

	@Test
	@SqlGroup(@Sql("data-add-dogbert.sql"))
	void methodLevelScripts() {
		assertNumUsers(1);
	}

}
