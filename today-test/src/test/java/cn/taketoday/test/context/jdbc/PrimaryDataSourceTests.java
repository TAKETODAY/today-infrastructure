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
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.jdbc.JdbcTestUtils;
import cn.taketoday.test.transaction.TransactionAssert;

import javax.sql.DataSource;

/**
 * Integration tests that ensure that <em>primary</em> data sources are
 * supported.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see cn.taketoday.test.context.transaction.PrimaryTransactionManagerTests
 */
@JUnitConfig
@DirtiesContext
class PrimaryDataSourceTests {

	@Configuration
	static class Config {

		@Primary
		@Bean
		DataSource primaryDataSource() {
			// @formatter:off
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")
					.build();
			// @formatter:on
		}

		@Bean
		DataSource additionalDataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}

	}


	private JdbcTemplate jdbcTemplate;


	@Autowired
	void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	@Sql("data.sql")
	void dataSourceTest() {
		TransactionAssert.assertThatTransaction().isNotActive();
		assertThat(JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "user")).as("Number of rows in the 'user' table.").isEqualTo(1);
	}

}
