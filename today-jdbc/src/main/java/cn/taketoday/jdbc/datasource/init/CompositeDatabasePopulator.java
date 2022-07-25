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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cn.taketoday.lang.Assert;

/**
 * Composite {@link DatabasePopulator} that delegates to a list of given
 * {@code DatabasePopulator} implementations, executing all scripts.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Kazuki Shimizu
 * @since 4.0
 */
public class CompositeDatabasePopulator implements DatabasePopulator {

  private final List<DatabasePopulator> populators = new ArrayList<>(4);

  /**
   * Create an empty {@code CompositeDatabasePopulator}.
   *
   * @see #setPopulators
   * @see #addPopulators
   */
  public CompositeDatabasePopulator() {
  }

  /**
   * Create a {@code CompositeDatabasePopulator} with the given populators.
   *
   * @param populators one or more populators to delegate to
   * @since 4.0
   */
  public CompositeDatabasePopulator(Collection<DatabasePopulator> populators) {
    Assert.notNull(populators, "DatabasePopulators must not be null");
    this.populators.addAll(populators);
  }

  /**
   * Create a {@code CompositeDatabasePopulator} with the given populators.
   *
   * @param populators one or more populators to delegate to
   * @since 4.0
   */
  public CompositeDatabasePopulator(DatabasePopulator... populators) {
    Assert.notNull(populators, "DatabasePopulators must not be null");
    this.populators.addAll(Arrays.asList(populators));
  }

  /**
   * Specify one or more populators to delegate to.
   */
  public void setPopulators(DatabasePopulator... populators) {
    Assert.notNull(populators, "DatabasePopulators must not be null");
    this.populators.clear();
    this.populators.addAll(Arrays.asList(populators));
  }

  /**
   * Add one or more populators to the list of delegates.
   */
  public void addPopulators(DatabasePopulator... populators) {
    Assert.notNull(populators, "DatabasePopulators must not be null");
    this.populators.addAll(Arrays.asList(populators));
  }

  @Override
  public void populate(Connection connection) throws SQLException, ScriptException {
    Assert.notNull(connection, "Connection must not be null");
    for (DatabasePopulator populator : this.populators) {
      populator.populate(connection);
    }
  }

}
