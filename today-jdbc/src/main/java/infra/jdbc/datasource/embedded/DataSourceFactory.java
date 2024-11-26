/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.jdbc.datasource.embedded;

import javax.sql.DataSource;

import infra.jdbc.datasource.SimpleDriverDataSource;

/**
 * {@code DataSourceFactory} encapsulates the creation of a particular
 * {@link DataSource} implementation such as a non-pooling
 * {@link SimpleDriverDataSource}
 * or a HikariCP pool setup in the shape of a {@code HikariDataSource}.
 *
 * <p>Call {@link #getConnectionProperties()} to configure normalized
 * {@code DataSource} properties before calling {@link #getDataSource()}
 * to actually get the configured {@code DataSource} instance.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 4.0
 */
public interface DataSourceFactory {

  /**
   * Get the {@linkplain ConnectionProperties connection properties}
   * of the {@link #getDataSource DataSource} to be configured.
   */
  ConnectionProperties getConnectionProperties();

  /**
   * Get the {@link DataSource} with the
   * {@linkplain #getConnectionProperties connection properties} applied.
   */
  DataSource getDataSource();

}
