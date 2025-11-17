/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc;

import java.util.List;

import infra.persistence.AbstractRepositoryManagerTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zapodot
 */
class NamedQueryArrayTests extends AbstractRepositoryManagerTests {

  private static class Foo {
    private int bar;
  }

  @ParameterizedRepositoryManagerTest
  void arrayTest(DbType dbType, RepositoryManager database) {
    database.createQuery("Drop table if exists FOO").executeUpdate();
    database.createQuery("CREATE TABLE FOO(BAR int PRIMARY KEY)").executeUpdate();
    database.createQuery("INSERT INTO FOO VALUES(1)").executeUpdate();
    database.createQuery("INSERT INTO FOO VALUES(2)").executeUpdate();

    try (final JdbcConnection connection = database.open();
            final NamedQuery query = connection.createNamedQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

      final List<Foo> foos = query.addParameters("bars", 1, 2)
              .fetch(Foo.class);

      assertThat(foos.size()).isEqualTo(2);
    }
  }

  @ParameterizedRepositoryManagerTest
  void emptyArrayTest(DbType dbType, RepositoryManager database) throws Exception {
    database.createQuery("Drop table if exists FOO").executeUpdate();
    database.createQuery("CREATE TABLE FOO(BAR int PRIMARY KEY)").executeUpdate();
    database.createQuery("INSERT INTO FOO VALUES(1)").executeUpdate();
    database.createQuery("INSERT INTO FOO VALUES(2)").executeUpdate();
    try (final JdbcConnection connection = database.open();
            final NamedQuery query = connection.createNamedQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

      final List<Foo> noFoos = query.addParameters("bars")
              .fetch(Foo.class);
      assertThat(noFoos.size()).isZero();
    }
  }
}
