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

package cn.taketoday.jdbc.datasource.embedded;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jdbc.datasource.init.DatabasePopulator;
import cn.taketoday.lang.Nullable;

/**
 * A subclass of {@link EmbeddedDatabaseFactory} that implements {@link FactoryBean}
 * for registration as a Framework bean. Returns the actual {@link DataSource} that
 * provides connectivity to the embedded database to Framework.
 *
 * <p>The target {@link DataSource} is returned instead of an {@link EmbeddedDatabase}
 * proxy since the {@link FactoryBean} will manage the initialization and destruction
 * lifecycle of the embedded database instance.
 *
 * <p>Implements {@link DisposableBean} to shutdown the embedded database when the
 * managing IoC is being closed.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.0
 */
public class EmbeddedDatabaseFactoryBean extends EmbeddedDatabaseFactory
        implements FactoryBean<DataSource>, InitializingBean, DisposableBean {

  @Nullable
  private DatabasePopulator databaseCleaner;

  /**
   * Set a script execution to be run in the bean destruction callback,
   * cleaning up the database and leaving it in a known state for others.
   *
   * @param databaseCleaner the database script executor to run on destroy
   * @see #setDatabasePopulator
   * @see cn.taketoday.jdbc.datasource.init.DataSourceInitializer#setDatabaseCleaner
   */
  public void setDatabaseCleaner(DatabasePopulator databaseCleaner) {
    this.databaseCleaner = databaseCleaner;
  }

  @Override
  public void afterPropertiesSet() {
    initDatabase();
  }

  @Override
  @Nullable
  public DataSource getObject() {
    return getDataSource();
  }

  @Override
  public Class<? extends DataSource> getObjectType() {
    return DataSource.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void destroy() {
    if (this.databaseCleaner != null && getDataSource() != null) {
      DatabasePopulator.execute(this.databaseCleaner, getDataSource());
    }
    shutdownDatabase();
  }

}
