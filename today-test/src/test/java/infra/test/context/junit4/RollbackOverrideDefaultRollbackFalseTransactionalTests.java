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

package infra.test.context.junit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.jdbc.core.JdbcTemplate;
import infra.test.annotation.Rollback;
import infra.test.transaction.TransactionAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extension of {@link DefaultRollbackFalseRollbackAnnotationTransactionalTests}
 * which tests method-level <em>rollback override</em> behavior via the
 * {@link Rollback @Rollback} annotation.
 *
 * @author Sam Brannen
 * @see Rollback
 * @since 4.0
 */
public class RollbackOverrideDefaultRollbackFalseTransactionalTests
        extends DefaultRollbackFalseRollbackAnnotationTransactionalTests {

  private static int originalNumRows;

  private static JdbcTemplate jdbcTemplate;

  @Autowired
  @Override
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Before
  @Override
  public void verifyInitialTestData() {
    originalNumRows = clearPersonTable(jdbcTemplate);
    assertThat(addPerson(jdbcTemplate, BOB)).as("Adding bob").isEqualTo(1);
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the initial number of rows in the person table.").isEqualTo(1);
  }

  @Test
  @Rollback
  @Override
  public void modifyTestDataWithinTransaction() {
    TransactionAssert.assertThatTransaction().isActive();
    assertThat(deletePerson(jdbcTemplate, BOB)).as("Deleting bob").isEqualTo(1);
    assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
    assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo(1);
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within a transaction.").isEqualTo(2);
  }

  @AfterClass
  public static void verifyFinalTestData() {
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo(originalNumRows);
  }

}
