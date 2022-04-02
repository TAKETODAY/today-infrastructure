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
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

/**
 * Transactional integration tests for {@link Sql @Sql} support with
 * inlined SQL {@link Sql#statements statements}.
 *
 * @author Sam Brannen
 * @see TransactionalSqlScriptsTests
 * @since 4.0
 */
@JUnitConfig(EmptyDatabaseConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(
        scripts = "schema.sql",
        statements = "INSERT INTO user VALUES('Dilbert')"
)
@DirtiesContext
class TransactionalInlinedStatementsSqlScriptsTests extends AbstractTransactionalTests {

  @Test
  @Order(1)
  void classLevelScripts() {
    assertNumUsers(1);
  }

  @Test
  @Sql(statements = "DROP TABLE user IF EXISTS")
  @Sql("schema.sql")
  @Sql(statements = "INSERT INTO user VALUES ('Dilbert'), ('Dogbert'), ('Catbert')")
  @Order(2)
  void methodLevelScripts() {
    assertNumUsers(3);
  }

}
