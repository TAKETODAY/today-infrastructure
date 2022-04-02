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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;

/**
 * Subclass of {@link RepeatableSqlAnnotationSqlScriptsParentTests} which verifies
 * that {@link Repeatable} {@link Sql @Sql} annotations are not
 * {@linkplain Inherited @Inherited} from a superclass if the subclass has local
 * {@code @Sql} declarations.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Sql("schema.sql")
@Sql("data-add-catbert.sql")
class RepeatableSqlAnnotationSqlScriptsChildTests extends RepeatableSqlAnnotationSqlScriptsParentTests {

  @Test
  @Order(1)
  @Override
  void classLevelScripts() {
    // Should not find Dilbert, since local @Sql declarations shadow @Sql
    // declarations on a superclass. This is due to the fact that we use
    // "get" semantics instead of "find" semantics when searching for @Sql.
    assertUsers("Catbert");
  }

}
