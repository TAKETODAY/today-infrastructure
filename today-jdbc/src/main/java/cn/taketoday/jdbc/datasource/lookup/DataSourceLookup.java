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

package cn.taketoday.jdbc.datasource.lookup;

import javax.sql.DataSource;

/**
 * Strategy interface for looking up DataSources by name.
 *
 * <p>Used, for example, to resolve data source names in JPA
 * {@code persistence.xml} files.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface DataSourceLookup {

  /**
   * Retrieve the DataSource identified by the given name.
   *
   * @param dataSourceName the name of the DataSource
   * @return the DataSource (never {@code null})
   * @throws DataSourceLookupFailureException if the lookup failed
   */
  DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException;

}
