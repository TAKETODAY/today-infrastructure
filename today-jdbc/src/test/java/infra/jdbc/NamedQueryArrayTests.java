/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
