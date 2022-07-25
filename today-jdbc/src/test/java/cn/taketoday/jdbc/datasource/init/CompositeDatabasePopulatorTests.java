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

package cn.taketoday.jdbc.datasource.init;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CompositeDatabasePopulator}.
 *
 * @author Kazuki Shimizu
 * @author Juergen Hoeller
 * @since 4.0
 */
class CompositeDatabasePopulatorTests {

  private final Connection mockedConnection = mock(Connection.class);

  private final DatabasePopulator mockedDatabasePopulator1 = mock(DatabasePopulator.class);

  private final DatabasePopulator mockedDatabasePopulator2 = mock(DatabasePopulator.class);

  @Test
  void addPopulators() throws SQLException {
    CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
    populator.addPopulators(mockedDatabasePopulator1, mockedDatabasePopulator2);

    populator.populate(mockedConnection);

    verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
    verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
  }

  @Test
  void setPopulatorsWithMultiple() throws SQLException {
    CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
    populator.setPopulators(mockedDatabasePopulator1, mockedDatabasePopulator2);  // multiple

    populator.populate(mockedConnection);

    verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
    verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
  }

  @Test
  void setPopulatorsForOverride() throws SQLException {
    CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
    populator.setPopulators(mockedDatabasePopulator1);
    populator.setPopulators(mockedDatabasePopulator2);  // override

    populator.populate(mockedConnection);

    verify(mockedDatabasePopulator1, times(0)).populate(mockedConnection);
    verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
  }

  @Test
  void constructWithVarargs() throws SQLException {
    CompositeDatabasePopulator populator =
            new CompositeDatabasePopulator(mockedDatabasePopulator1, mockedDatabasePopulator2);

    populator.populate(mockedConnection);

    verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
    verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
  }

  @Test
  void constructWithCollection() throws SQLException {
    Set<DatabasePopulator> populators = new LinkedHashSet<>();
    populators.add(mockedDatabasePopulator1);
    populators.add(mockedDatabasePopulator2);

    CompositeDatabasePopulator populator = new CompositeDatabasePopulator(populators);
    populator.populate(mockedConnection);

    verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
    verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
  }

}
