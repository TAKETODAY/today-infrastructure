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

/**
 * Integration tests that verify support for default SQL script detection.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(EmptyDatabaseConfig.class)
@Sql
@DirtiesContext
class DefaultScriptDetectionSqlScriptsTests extends AbstractTransactionalTests {

	@Test
	void classLevel() {
		assertNumUsers(2);
	}

	@Test
	@Sql
	void methodLevel() {
		assertNumUsers(3);
	}

}
