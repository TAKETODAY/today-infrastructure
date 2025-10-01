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

package infra.jdbc.datasource.lookup;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import infra.lang.Assert;

/**
 * Simple {@link DataSourceLookup} implementation that relies on a map for doing lookups.
 *
 * <p>Useful for testing environments or applications that need to match arbitrary
 * {@link String} names to target {@link DataSource} objects.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 4.0
 */
public class MapDataSourceLookup implements DataSourceLookup {

  private final Map<String, DataSource> dataSources = new HashMap<>(4);

  /**
   * Create a new instance of the {@link MapDataSourceLookup} class.
   */
  public MapDataSourceLookup() { }

  /**
   * Create a new instance of the {@link MapDataSourceLookup} class.
   *
   * @param dataSources the {@link Map} of {@link DataSource DataSources}; the keys
   * are {@link String Strings}, the values are actual {@link DataSource} instances.
   */
  public MapDataSourceLookup(Map<String, DataSource> dataSources) {
    setDataSources(dataSources);
  }

  /**
   * Create a new instance of the {@link MapDataSourceLookup} class.
   *
   * @param dataSourceName the name under which the supplied {@link DataSource} is to be added
   * @param dataSource the {@link DataSource} to be added
   */
  public MapDataSourceLookup(String dataSourceName, DataSource dataSource) {
    addDataSource(dataSourceName, dataSource);
  }

  /**
   * Set the {@link Map} of {@link DataSource DataSources}; the keys
   * are {@link String Strings}, the values are actual {@link DataSource} instances.
   * <p>If the supplied {@link Map} is {@code null}, then this method
   * call effectively has no effect.
   *
   * @param dataSources said {@link Map} of {@link DataSource DataSources}
   */
  public void setDataSources(@Nullable Map<String, DataSource> dataSources) {
    if (dataSources != null) {
      this.dataSources.putAll(dataSources);
    }
  }

  /**
   * Get the {@link Map} of {@link DataSource DataSources} maintained by this object.
   * <p>The returned {@link Map} is {@link Collections#unmodifiableMap(Map) unmodifiable}.
   *
   * @return said {@link Map} of {@link DataSource DataSources} (never {@code null})
   */
  public Map<String, DataSource> getDataSources() {
    return Collections.unmodifiableMap(this.dataSources);
  }

  /**
   * Add the supplied {@link DataSource} to the map of {@link DataSource DataSources}
   * maintained by this object.
   *
   * @param dataSourceName the name under which the supplied {@link DataSource} is to be added
   * @param dataSource the {@link DataSource} to be so added
   */
  public void addDataSource(String dataSourceName, DataSource dataSource) {
    Assert.notNull(dataSourceName, "DataSource name is required");
    Assert.notNull(dataSource, "DataSource is required");
    this.dataSources.put(dataSourceName, dataSource);
  }

  @Override
  public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
    Assert.notNull(dataSourceName, "DataSource name is required");
    DataSource dataSource = this.dataSources.get(dataSourceName);
    if (dataSource == null) {
      throw new DataSourceLookupFailureException(
              "No DataSource with name '" + dataSourceName + "' registered");
    }
    return dataSource;
  }

}
