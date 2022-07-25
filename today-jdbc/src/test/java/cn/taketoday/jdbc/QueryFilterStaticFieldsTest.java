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

package cn.taketoday.jdbc;

import com.google.common.primitives.Longs;

import org.junit.Rule;
import org.junit.Test;
import org.zapodot.junit.db.EmbeddedDatabaseRule;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryFilterStaticFieldsTest {

  @Rule
  public EmbeddedDatabaseRule databaseRule = EmbeddedDatabaseRule.builder()
          .withInitialSql(
                  "CREATE TABLE TEST(ver int primary key); INSERT INTO TEST VALUES(1);")
          .build();

  static class Entity {
    public long ver;
    public static final Comparator<Entity> VER = new Comparator<Entity>() {
      @Override
      public int compare(final Entity o1, final Entity o2) {
        return Longs.compare(o1.ver, o2.ver);
      }
    };
  }

  @Test
  public void dontTouchTheStaticFieldTest() throws Exception {
    final JdbcOperations dataBase = new JdbcOperations(databaseRule.getDataSource());
    try (final JdbcConnection connection = dataBase.open();
            final Query query = connection.createQuery("SELECT * FROM TEST WHERE ver=1")) {
      final Entity entity = query.fetchFirst(Entity.class);
      assertThat(entity.ver).isEqualTo(1L);
    }
  }
}
