/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.datasource.init;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static cn.taketoday.jdbc.datasource.init.ScriptUtils.executeSqlScript;

/**
 * Integration tests for {@link ScriptUtils}.
 *
 * @author Sam Brannen
 * @see ScriptUtilsUnitTests
 * @since 4.0
 */
public class ScriptUtilsIntegrationTests extends AbstractDatabaseInitializationTests {

  @Override
  protected EmbeddedDatabaseType getEmbeddedDatabaseType() {
    return EmbeddedDatabaseType.HSQL;
  }

  @BeforeEach
  public void setUpSchema() throws SQLException {
    executeSqlScript(db.getConnection(), usersSchema());
  }

  @Test
  public void executeSqlScriptContainingMultiLineComments() throws SQLException {
    executeSqlScript(db.getConnection(), resource("test-data-with-multi-line-comments.sql"));
    assertUsersDatabaseCreated("Hoeller", "Brannen");
  }

  /**
   * @since 4.0
   */
  @Test
  public void executeSqlScriptContainingSingleQuotesNestedInsideDoubleQuotes() throws SQLException {
    executeSqlScript(db.getConnection(), resource("users-data-with-single-quotes-nested-in-double-quotes.sql"));
    assertUsersDatabaseCreated("Hoeller", "Brannen");
  }

}
