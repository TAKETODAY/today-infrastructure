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

import org.junit.runner.RunWith;

import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.transaction.annotation.Transactional;

/**
 * Abstract base class for verifying support of Spring's {@link Transactional
 * &#64;Transactional} annotation.
 *
 * @author Sam Brannen
 * @see ClassLevelTransactionalSpringRunnerTests
 * @see MethodLevelTransactionalSpringRunnerTests
 * @see Transactional
 * @since 4.0
 */
@RunWith(Runner.class)
@ContextConfiguration("transactionalTests-context.xml")
public abstract class AbstractTransactionalSpringRunnerTests {

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
