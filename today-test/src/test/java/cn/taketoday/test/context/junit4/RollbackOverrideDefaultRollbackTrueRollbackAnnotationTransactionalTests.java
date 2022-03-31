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

package cn.taketoday.test.context.junit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.test.annotation.Rollback;
import cn.taketoday.test.transaction.TransactionAssert;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extension of {@link DefaultRollbackTrueRollbackAnnotationTransactionalTests}
 * which tests method-level <em>rollback override</em> behavior via the
 * {@link Rollback @Rollback} annotation.
 *
 * @author Sam Brannen
 * @since 4.0
 * @see Rollback
 */
public class RollbackOverrideDefaultRollbackTrueRollbackAnnotationTransactionalTests extends
		DefaultRollbackTrueRollbackAnnotationTransactionalTests {

	private static JdbcTemplate jdbcTemplate;


	@Autowired
	@Override
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}


	@Before
	@Override
	public void verifyInitialTestData() {
		clearPersonTable(jdbcTemplate);
		assertThat(addPerson(jdbcTemplate, BOB)).as("Adding bob").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the initial number of rows in the person table.").isEqualTo(1);
	}

	@Test
	@Rollback(false)
	@Override
	public void modifyTestDataWithinTransaction() {
		TransactionAssert.assertThatTransaction().isActive();
		assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within a transaction.").isEqualTo(3);
	}

	@AfterClass
	public static void verifyFinalTestData() {
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo(3);
	}

}
