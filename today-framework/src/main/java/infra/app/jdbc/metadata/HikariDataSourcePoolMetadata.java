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

package infra.app.jdbc.metadata;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;

import infra.beans.DirectFieldAccessor;

/**
 * {@link DataSourcePoolMetadata} for a Hikari {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HikariDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<HikariDataSource> {

  public HikariDataSourcePoolMetadata(HikariDataSource dataSource) {
    super(dataSource);
  }

  @Nullable
  @Override
  public Integer getActive() {
    try {
      return getHikariPool().getActiveConnections();
    }
    catch (Exception ex) {
      return null;
    }
  }

  @Nullable
  @Override
  public Integer getIdle() {
    try {
      return getHikariPool().getIdleConnections();
    }
    catch (Exception ex) {
      return null;
    }
  }

  @SuppressWarnings("NullAway")
  private HikariPool getHikariPool() {
    return (HikariPool) new DirectFieldAccessor(getDataSource()).getPropertyValue("pool");
  }

  @Override
  public Integer getMax() {
    return getDataSource().getMaximumPoolSize();
  }

  @Override
  public Integer getMin() {
    return getDataSource().getMinimumIdle();
  }

  @Override
  public String getValidationQuery() {
    return getDataSource().getConnectionTestQuery();
  }

  @Override
  public Boolean getDefaultAutoCommit() {
    return getDataSource().isAutoCommit();
  }

}
