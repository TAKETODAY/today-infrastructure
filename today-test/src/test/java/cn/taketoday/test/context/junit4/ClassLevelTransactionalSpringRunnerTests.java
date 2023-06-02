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

import javax.sql.DataSource;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextTestExecutionListener;
import cn.taketoday.test.context.transaction.TransactionalTestExecutionListener;
import cn.taketoday.test.transaction.TransactionAssert;
import cn.taketoday.transaction.annotation.Propagation;
import cn.taketoday.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 4 based integration test which verifies support of Infra
 * {@link Transactional &#64;Transactional}, {@link TestExecutionListeners
 * &#64;TestExecutionListeners}, and {@link ContextConfiguration
 * &#64;ContextConfiguration} annotations in conjunction with the
 * {@link InfraRunner} and the following
 * {@link TestExecutionListener TestExecutionListeners}:
 *
 * <ul>
 * <li>{@link DependencyInjectionTestExecutionListener}</li>
 * <li>{@link DirtiesContextTestExecutionListener}</li>
 * <li>{@link TransactionalTestExecutionListener}</li>
 * </ul>
 *
 * <p>This class specifically tests usage of {@code @Transactional} defined
 * at the <strong>class level</strong>.
 *
 * @author Sam Brannen
 * @see MethodLevelTransactionalSpringRunnerTests
 * @since 4.0
 */
@Transactional
public class ClassLevelTransactionalSpringRunnerTests extends AbstractTransactionalSpringRunnerTests {

  protected static JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @AfterClass
  public static void verifyFinalTestData() {
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo(4);
  }

  @Before
  public void verifyInitialTestData() {
    clearPersonTable(jdbcTemplate);
    assertThat(addPerson(jdbcTemplate, BOB)).as("Adding bob").isEqualTo(1);
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the initial number of rows in the person table.").isEqualTo(1);
  }

  @Test
  public void modifyTestDataWithinTransaction() {
    TransactionAssert.assertThatTransaction().isActive();
    assertThat(deletePerson(jdbcTemplate, BOB)).as("Deleting bob").isEqualTo(1);
    assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
    assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo(1);
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within a transaction.").isEqualTo(2);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void modifyTestDataWithoutTransaction() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(addPerson(jdbcTemplate, LUKE)).as("Adding luke").isEqualTo(1);
    assertThat(addPerson(jdbcTemplate, LEIA)).as("Adding leia").isEqualTo(1);
    assertThat(addPerson(jdbcTemplate, YODA)).as("Adding yoda").isEqualTo(1);
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table without a transaction.").isEqualTo(4);
  }

}
