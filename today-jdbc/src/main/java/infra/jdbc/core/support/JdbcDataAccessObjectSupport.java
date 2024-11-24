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

package infra.jdbc.core.support;

import java.sql.Connection;

import javax.sql.DataSource;

import infra.dao.support.DataAccessObjectSupport;
import infra.jdbc.CannotGetJdbcConnectionException;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DataSourceUtils;
import infra.jdbc.support.SQLExceptionTranslator;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Convenient super class for JDBC-based data access objects.
 *
 * <p>Requires a {@link DataSource} to be set, providing a
 * {@link JdbcTemplate} based on it to
 * subclasses through the {@link #getJdbcTemplate()} method.
 *
 * <p>This base class is mainly intended for JdbcTemplate usage but can
 * also be used when working with a Connection directly or when using
 * {@code infra.jdbc.object} operation objects.
 *
 * @author Juergen Hoeller
 * @see #setDataSource
 * @see #getJdbcTemplate
 * @see JdbcTemplate
 * @since 4.0
 */
public abstract class JdbcDataAccessObjectSupport extends DataAccessObjectSupport {

  @Nullable
  private JdbcTemplate jdbcTemplate;

  /**
   * Set the JDBC DataSource to be used by this DAO.
   */
  public final void setDataSource(DataSource dataSource) {
    if (this.jdbcTemplate == null || dataSource != this.jdbcTemplate.getDataSource()) {
      this.jdbcTemplate = createJdbcTemplate(dataSource);
      initTemplateConfig();
    }
  }

  /**
   * Create a JdbcTemplate for the given DataSource.
   * Only invoked if populating the DAO with a DataSource reference!
   * <p>Can be overridden in subclasses to provide a JdbcTemplate instance
   * with different configuration, or a custom JdbcTemplate subclass.
   *
   * @param dataSource the JDBC DataSource to create a JdbcTemplate for
   * @return the new JdbcTemplate instance
   * @see #setDataSource
   */
  protected JdbcTemplate createJdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  /**
   * Return the JDBC DataSource used by this DAO.
   */
  @Nullable
  public final DataSource getDataSource() {
    return (this.jdbcTemplate != null ? this.jdbcTemplate.getDataSource() : null);
  }

  /**
   * Set the JdbcTemplate for this DAO explicitly,
   * as an alternative to specifying a DataSource.
   */
  public final void setJdbcTemplate(@Nullable JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    initTemplateConfig();
  }

  /**
   * Return the JdbcTemplate for this DAO,
   * pre-initialized with the DataSource or set explicitly.
   */
  @Nullable
  public final JdbcTemplate getJdbcTemplate() {
    return this.jdbcTemplate;
  }

  /**
   * Initialize the template-based configuration of this DAO.
   * Called after a new JdbcTemplate has been set, either directly
   * or through a DataSource.
   * <p>This implementation is empty. Subclasses may override this
   * to configure further objects based on the JdbcTemplate.
   *
   * @see #getJdbcTemplate()
   */
  protected void initTemplateConfig() { }

  @Override
  protected void checkDaoConfig() {
    if (this.jdbcTemplate == null) {
      throw new IllegalArgumentException("'dataSource' or 'jdbcTemplate' is required");
    }
  }

  /**
   * Return the SQLExceptionTranslator of this DAO's JdbcTemplate,
   * for translating SQLExceptions in custom JDBC access code.
   *
   * @see JdbcTemplate#getExceptionTranslator()
   */
  protected final SQLExceptionTranslator getExceptionTranslator() {
    JdbcTemplate jdbcTemplate = getJdbcTemplate();
    Assert.state(jdbcTemplate != null, "No JdbcTemplate set");
    return jdbcTemplate.getExceptionTranslator();
  }

  /**
   * Get a JDBC Connection, either from the current transaction or a new one.
   *
   * @return the JDBC Connection
   * @throws CannotGetJdbcConnectionException if the attempt to get a Connection failed
   * @see DataSourceUtils#getConnection(DataSource)
   */
  protected final Connection getConnection() throws CannotGetJdbcConnectionException {
    DataSource dataSource = getDataSource();
    Assert.state(dataSource != null, "No DataSource set");
    return DataSourceUtils.getConnection(dataSource);
  }

  /**
   * Close the given JDBC Connection, created via this DAO's DataSource,
   * if it isn't bound to the thread.
   *
   * @param con the Connection to close
   * @see DataSourceUtils#releaseConnection
   */
  protected final void releaseConnection(Connection con) {
    DataSourceUtils.releaseConnection(con, getDataSource());
  }

}
