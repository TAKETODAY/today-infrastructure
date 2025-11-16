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

import com.google.common.primitives.Longs;

import java.util.Comparator;

import infra.persistence.AbstractRepositoryManagerTests;

import static org.assertj.core.api.Assertions.assertThat;

class NamedQueryFilterStaticFieldsTests extends AbstractRepositoryManagerTests {

  static class Entity {
    public long ver;
    public static final Comparator<Entity> VER = new Comparator<Entity>() {
      @Override
      public int compare(final Entity o1, final Entity o2) {
        return Longs.compare(o1.ver, o2.ver);
      }
    };
  }

  @ParameterizedRepositoryManagerTest
  void dontTouchTheStaticFieldTest(DbType dbType, RepositoryManager database) {
    database.createQuery("Drop table if exists TEST").executeUpdate();
    database.createQuery("CREATE TABLE TEST(ver int primary key)").executeUpdate();
    database.createQuery("INSERT INTO TEST VALUES(1)").executeUpdate();

    try (final JdbcConnection connection = database.open();
            final NamedQuery query = connection.createNamedQuery("SELECT * FROM TEST WHERE ver=1")) {
      final Entity entity = query.fetchFirst(Entity.class);
      assertThat(entity.ver).isEqualTo(1L);
    }
  }
}
