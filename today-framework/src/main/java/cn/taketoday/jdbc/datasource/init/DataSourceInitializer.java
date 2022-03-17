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

import javax.sql.DataSource;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Used to {@linkplain #setDatabasePopulator set up} a database during
 * initialization and {@link #setDatabaseCleaner clean up} a database during
 * destruction.
 *
 * @author Dave Syer
 * @author Sam Brannen
 * @see DatabasePopulator
 * @since 4.0
 */
public class DataSourceInitializer implements InitializingBean, DisposableBean {

  @Nullable
  private DataSource dataSource;

  @Nullable
  private DatabasePopulator databasePopulator;

  @Nullable
  private DatabasePopulator databaseCleaner;

  private boolean enabled = true;

  /**
   * The {@link DataSource} for the database to populate when this component
   * is initialized and to clean up when this component is shut down.
   * <p>This property is mandatory with no default provided.
   *
   * @param dataSource the DataSource
   */
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Set the {@link DatabasePopulator} to execute during the bean initialization phase.
   *
   * @param databasePopulator the {@code DatabasePopulator} to use during initialization
   * @see #setDatabaseCleaner
   */
  public void setDatabasePopulator(DatabasePopulator databasePopulator) {
    this.databasePopulator = databasePopulator;
  }

  /**
   * Set the {@link DatabasePopulator} to execute during the bean destruction
   * phase, cleaning up the database and leaving it in a known state for others.
   *
   * @param databaseCleaner the {@code DatabasePopulator} to use during destruction
   * @see #setDatabasePopulator
   */
  public void setDatabaseCleaner(DatabasePopulator databaseCleaner) {
    this.databaseCleaner = databaseCleaner;
  }

  /**
   * Flag to explicitly enable or disable the {@linkplain #setDatabasePopulator
   * database populator} and {@linkplain #setDatabaseCleaner database cleaner}.
   *
   * @param enabled {@code true} if the database populator and database cleaner
   * should be called on startup and shutdown, respectively
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Use the {@linkplain #setDatabasePopulator database populator} to set up
   * the database.
   */
  @Override
  public void afterPropertiesSet() {
    execute(this.databasePopulator);
  }

  /**
   * Use the {@linkplain #setDatabaseCleaner database cleaner} to clean up the
   * database.
   */
  @Override
  public void destroy() {
    execute(this.databaseCleaner);
  }

  private void execute(@Nullable DatabasePopulator populator) {
    Assert.state(this.dataSource != null, "DataSource must be set");
    if (this.enabled && populator != null) {
      DatabasePopulator.execute(populator, this.dataSource);
    }
  }

}
