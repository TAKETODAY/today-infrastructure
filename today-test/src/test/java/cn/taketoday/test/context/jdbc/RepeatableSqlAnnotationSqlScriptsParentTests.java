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

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.ContextConfiguration;

import java.lang.annotation.Repeatable;

/**
 * This is a copy of {@link TransactionalSqlScriptsTests} that verifies proper
 * handling of {@link Sql @Sql} as a {@link Repeatable} annotation.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@Sql("schema.sql")
@Sql("data.sql")
@DirtiesContext
class RepeatableSqlAnnotationSqlScriptsParentTests extends AbstractTransactionalTests {

	@Test
	@Order(1)
	void classLevelScripts() {
		assertUsers("Dilbert");
	}

	@Test
	@Sql("drop-schema.sql")
	@Sql("schema.sql")
	@Sql("data.sql")
	@Sql("data-add-dogbert.sql")
	@Order(2)
	void methodLevelScripts() {
		assertUsers("Dilbert", "Dogbert");
	}

}
