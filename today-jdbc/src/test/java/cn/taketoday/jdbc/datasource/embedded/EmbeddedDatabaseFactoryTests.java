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

package cn.taketoday.jdbc.datasource.embedded;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import cn.taketoday.jdbc.datasource.init.DatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 */
public class EmbeddedDatabaseFactoryTests {

  private EmbeddedDatabaseFactory factory = new EmbeddedDatabaseFactory();

  @Test
  public void testGetDataSource() {
    StubDatabasePopulator populator = new StubDatabasePopulator();
    factory.setDatabasePopulator(populator);
    EmbeddedDatabase db = factory.getDatabase();
    assertThat(populator.populateCalled).isTrue();
    db.shutdown();
  }

  private static class StubDatabasePopulator implements DatabasePopulator {

    private boolean populateCalled;

    @Override
    public void populate(Connection connection) {
      this.populateCalled = true;
    }
  }

}
