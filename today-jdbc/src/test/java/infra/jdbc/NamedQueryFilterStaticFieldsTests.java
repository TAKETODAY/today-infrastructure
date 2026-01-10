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
