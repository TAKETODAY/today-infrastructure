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

import org.junit.runner.RunWith;

import infra.jdbc.core.JdbcTemplate;
import infra.test.context.ContextConfiguration;
import infra.transaction.annotation.Transactional;

/**
 * Abstract base class for verifying support of Infra {@link Transactional
 * &#64;Transactional} annotation.
 *
 * @author Sam Brannen
 * @see ClassLevelTransactionalInfraRunnerTests
 * @see MethodLevelTransactionalInfraRunnerTests
 * @see Transactional
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@ContextConfiguration("transactionalTests-context.xml")
public abstract class AbstractTransactionalInfraRunnerTests {

  protected static final String BOB = "bob";
  protected static final String JANE = "jane";
  protected static final String SUE = "sue";
  protected static final String LUKE = "luke";
  protected static final String LEIA = "leia";
  protected static final String YODA = "yoda";

  protected static int clearPersonTable(JdbcTemplate jdbcTemplate) {
    return jdbcTemplate.update("DELETE FROM person");
  }

  protected static int countRowsInPersonTable(JdbcTemplate jdbcTemplate) {
    return jdbcTemplate.queryForObject("SELECT COUNT(0) FROM person", Integer.class);
  }

  protected static int addPerson(JdbcTemplate jdbcTemplate, String name) {
    return jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
  }

  protected static int deletePerson(JdbcTemplate jdbcTemplate, String name) {
    return jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
  }

}
