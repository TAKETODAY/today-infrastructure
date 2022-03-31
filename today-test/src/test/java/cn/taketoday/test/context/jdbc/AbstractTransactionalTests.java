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

import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.jdbc.JdbcTestUtils;
import cn.taketoday.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(ApplicationExtension.class)
@Transactional
public abstract class AbstractTransactionalTests {

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	protected final int countRowsInTable(String tableName) {
		return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
	}

	protected final void assertNumUsers(int expected) {
		assertThat(countRowsInTable("user")).as("Number of rows in the 'user' table.").isEqualTo(expected);
	}

	protected final void assertUsers(String... expectedUsers) {
		List<String> actualUsers = this.jdbcTemplate.queryForList("select name from user", String.class);
		assertThat(actualUsers).containsExactlyInAnyOrder(expectedUsers);
	}

}
