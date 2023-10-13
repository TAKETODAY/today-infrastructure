/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.datasource.lookup;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jdbc.datasource.AbstractDataSource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Abstract {@link DataSource} implementation that routes {@link #getConnection()}
 * calls to one of various target DataSources based on a lookup key. The latter is usually
 * (but not necessarily) determined through some thread-bound transaction context.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setTargetDataSources
 * @see #setDefaultTargetDataSource
 * @see #determineCurrentLookupKey()
 * @since 4.0
 */
public abstract class AbstractRoutingDataSource extends AbstractDataSource implements InitializingBean {

  @Nullable
  private Map<Object, Object> targetDataSources;

  @Nullable
  private Object defaultTargetDataSource;

  private boolean lenientFallback = true;

  private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

  @Nullable
  private Map<Object, DataSource> resolvedDataSources;

  @Nullable
  private DataSource resolvedDefaultDataSource;

  /**
   * Specify the map of target DataSources, with the lookup key as key.
   * The mapped value can either be a corresponding {@link DataSource}
   * instance or a data source name String (to be resolved via a
   * {@link #setDataSourceLookup DataSourceLookup}).
   * <p>The key can be of arbitrary type; this class implements the
   * generic lookup process only. The concrete key representation will
   * be handled by {@link #resolveSpecifiedLookupKey(Object)} and
   * {@link #determineCurrentLookupKey()}.
   */
  public void setTargetDataSources(Map<Object, Object> targetDataSources) {
    this.targetDataSources = targetDataSources;
  }

  /**
   * Specify the default target DataSource, if any.
   * <p>The mapped value can either be a corresponding {@link DataSource}
   * instance or a data source name String (to be resolved via a
   * {@link #setDataSourceLookup DataSourceLookup}).
   * <p>This DataSource will be used as target if none of the keyed
   * {@link #setTargetDataSources targetDataSources} match the
   * {@link #determineCurrentLookupKey()} current lookup key.
   */
  public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
    this.defaultTargetDataSource = defaultTargetDataSource;
  }

  /**
   * Specify whether to apply a lenient fallback to the default DataSource
   * if no specific DataSource could be found for the current lookup key.
   * <p>Default is "true", accepting lookup keys without a corresponding entry
   * in the target DataSource map - simply falling back to the default DataSource
   * in that case.
   * <p>Switch this flag to "false" if you would prefer the fallback to only apply
   * if the lookup key was {@code null}. Lookup keys without a DataSource
   * entry will then lead to an IllegalStateException.
   *
   * @see #setTargetDataSources
   * @see #setDefaultTargetDataSource
   * @see #determineCurrentLookupKey()
   */
  public void setLenientFallback(boolean lenientFallback) {
    this.lenientFallback = lenientFallback;
  }

  /**
   * Set the DataSourceLookup implementation to use for resolving data source
   * name Strings in the {@link #setTargetDataSources targetDataSources} map.
   * <p>Default is a {@link JndiDataSourceLookup}, allowing the JNDI names
   * of application server DataSources to be specified directly.
   */
  public void setDataSourceLookup(@Nullable DataSourceLookup dataSourceLookup) {
    this.dataSourceLookup = (dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup());
  }

  @Override
  public void afterPropertiesSet() {
    initialize();
  }

  /**
   * Initialize the internal state of this {@code AbstractRoutingDataSource}
   * by resolving the configured target DataSources.
   *
   * @throws IllegalArgumentException if the target DataSources have not been configured
   * @see #setTargetDataSources(Map)
   * @see #setDefaultTargetDataSource(Object)
   * @see #getResolvedDataSources()
   * @see #getResolvedDefaultDataSource()
   */
  public void initialize() {
    if (this.targetDataSources == null) {
      throw new IllegalArgumentException("Property 'targetDataSources' is required");
    }
    this.resolvedDataSources = CollectionUtils.newHashMap(this.targetDataSources.size());
    for (Map.Entry<Object, Object> entry : targetDataSources.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      Object lookupKey = resolveSpecifiedLookupKey(key);
      DataSource dataSource = resolveSpecifiedDataSource(value);
      resolvedDataSources.put(lookupKey, dataSource);
    }

    if (defaultTargetDataSource != null) {
      this.resolvedDefaultDataSource = resolveSpecifiedDataSource(defaultTargetDataSource);
    }
  }

  /**
   * Resolve the given lookup key object, as specified in the
   * {@link #setTargetDataSources targetDataSources} map, into
   * the actual lookup key to be used for matching with the
   * {@link #determineCurrentLookupKey() current lookup key}.
   * <p>The default implementation simply returns the given key as-is.
   *
   * @param lookupKey the lookup key object as specified by the user
   * @return the lookup key as needed for matching
   */
  protected Object resolveSpecifiedLookupKey(Object lookupKey) {
    return lookupKey;
  }

  /**
   * Resolve the specified data source object into a DataSource instance.
   * <p>The default implementation handles DataSource instances and data source
   * names (to be resolved via a {@link #setDataSourceLookup DataSourceLookup}).
   *
   * @param dataSource the data source value object as specified in the
   * {@link #setTargetDataSources targetDataSources} map
   * @return the resolved DataSource (never {@code null})
   * @throws IllegalArgumentException in case of an unsupported value type
   */
  protected DataSource resolveSpecifiedDataSource(Object dataSource) throws IllegalArgumentException {
    if (dataSource instanceof DataSource) {
      return (DataSource) dataSource;
    }
    else if (dataSource instanceof String) {
      return this.dataSourceLookup.getDataSource((String) dataSource);
    }
    else {
      throw new IllegalArgumentException(
              "Illegal data source value - only [javax.sql.DataSource] and String supported: " + dataSource);
    }
  }

  /**
   * Return the resolved target DataSources that this router manages.
   *
   * @return an unmodifiable map of resolved lookup keys and DataSources
   * @throws IllegalStateException if the target DataSources are not resolved yet
   * @see #setTargetDataSources
   */
  public Map<Object, DataSource> getResolvedDataSources() {
    Assert.state(this.resolvedDataSources != null, "DataSources not resolved yet - call afterPropertiesSet");
    return Collections.unmodifiableMap(this.resolvedDataSources);
  }

  /**
   * Return the resolved default target DataSource, if any.
   *
   * @return the default DataSource, or {@code null} if none or not resolved yet
   * @see #setDefaultTargetDataSource
   */
  @Nullable
  public DataSource getResolvedDefaultDataSource() {
    return this.resolvedDefaultDataSource;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return determineTargetDataSource().getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return determineTargetDataSource().getConnection(username, password);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return (T) this;
    }
    return determineTargetDataSource().unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return (iface.isInstance(this) || determineTargetDataSource().isWrapperFor(iface));
  }

  /**
   * Retrieve the current target DataSource. Determines the
   * {@link #determineCurrentLookupKey() current lookup key}, performs
   * a lookup in the {@link #setTargetDataSources targetDataSources} map,
   * falls back to the specified
   * {@link #setDefaultTargetDataSource default target DataSource} if necessary.
   *
   * @see #determineCurrentLookupKey()
   */
  protected DataSource determineTargetDataSource() {
    Assert.notNull(this.resolvedDataSources, "DataSource router not initialized");
    Object lookupKey = determineCurrentLookupKey();
    DataSource dataSource = this.resolvedDataSources.get(lookupKey);
    if (dataSource == null && (this.lenientFallback || lookupKey == null)) {
      dataSource = this.resolvedDefaultDataSource;
    }
    if (dataSource == null) {
      throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
    }
    return dataSource;
  }

  /**
   * Determine the current lookup key. This will typically be
   * implemented to check a thread-bound transaction context.
   * <p>Allows for arbitrary keys. The returned key needs
   * to match the stored lookup key type, as resolved by the
   * {@link #resolveSpecifiedLookupKey} method.
   */
  @Nullable
  protected abstract Object determineCurrentLookupKey();

}
