/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.zapodot.junit.db.EmbeddedDatabaseRule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zapodot
 */
public class NamedQueryArrayTest {

  private static class Foo {
    private int bar;
  }

  @Rule
  public EmbeddedDatabaseRule databaseRule = EmbeddedDatabaseRule.builder()
          .withMode(EmbeddedDatabaseRule.CompatibilityMode.Oracle)
          .withInitialSql("CREATE TABLE FOO(BAR int PRIMARY KEY); INSERT INTO FOO VALUES(1); INSERT INTO FOO VALUES(2)")
          .build();

  @Test
  public void arrayTest() throws Exception {
    final RepositoryManager database = new RepositoryManager(databaseRule.getDataSource());
    try (final JdbcConnection connection = database.open();
            final NamedQuery query = connection.createNamedQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

      final List<Foo> foos = query.addParameters("bars", 1, 2)
              .fetch(Foo.class);

      assertThat(foos.size()).isEqualTo(2);
    }
  }

  @Test
  public void emptyArrayTest() throws Exception {
    final RepositoryManager database = new RepositoryManager(databaseRule.getDataSource());

    try (final JdbcConnection connection = database.open();
            final NamedQuery query = connection.createNamedQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

      final List<Foo> noFoos = query.addParameters("bars", new Integer[] {})
              .fetch(Foo.class);
      assertThat(noFoos.size()).isZero();
    }
  }
}
