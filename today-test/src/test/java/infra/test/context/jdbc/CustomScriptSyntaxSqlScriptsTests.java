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

package infra.test.context.jdbc;

import org.junit.jupiter.api.Test;

import infra.test.annotation.DirtiesContext;
import infra.test.context.ContextConfiguration;

/**
 * Integration tests that verify support for custom SQL script syntax
 * configured via {@link SqlConfig @SqlConfig}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@DirtiesContext
class CustomScriptSyntaxSqlScriptsTests extends AbstractTransactionalTests {

  @Test
  @Sql("schema.sql")
  @Sql(scripts = "data-add-users-with-custom-script-syntax.sql",//
       config = @SqlConfig(commentPrefixes = { "`", "%%" }, blockCommentStartDelimiter = "#$", blockCommentEndDelimiter = "$#", separator = "@@"))
  void methodLevelScripts() {
    assertNumUsers(3);
  }

}
