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

package cn.taketoday.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import cn.taketoday.lang.Nullable;

/**
 * Abstract base class for JDBC {@link javax.sql.DataSource} implementations
 * that operate on a JDBC {@link java.sql.Driver}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SimpleDriverDataSource
 * @see DriverManagerDataSource
 * @since 4.0
 */
public abstract class AbstractDriverBasedDataSource extends AbstractDataSource {

  @Nullable
  private String url;

  @Nullable
  private String username;

  @Nullable
  private String password;

  @Nullable
  private String catalog;

  @Nullable
  private String schema;

  @Nullable
  private Properties connectionProperties;

  /**
   * Set the JDBC URL to use for connecting through the Driver.
   *
   * @see java.sql.Driver#connect(String, Properties)
   */
  public void setUrl(@Nullable String url) {
    this.url = (url != null ? url.trim() : null);
  }

  /**
   * Return the JDBC URL to use for connecting through the Driver.
   */
  @Nullable
  public String getUrl() {
    return this.url;
  }

  /**
   * Set the JDBC username to use for connecting through the Driver.
   *
   * @see java.sql.Driver#connect(String, Properties)
   */
  public void setUsername(@Nullable String username) {
    this.username = username;
  }

  /**
   * Return the JDBC username to use for connecting through the Driver.
   */
  @Nullable
  public String getUsername() {
    return this.username;
  }

  /**
   * Set the JDBC password to use for connecting through the Driver.
   *
   * @see java.sql.Driver#connect(String, Properties)
   */
  public void setPassword(@Nullable String password) {
    this.password = password;
  }

  /**
   * Return the JDBC password to use for connecting through the Driver.
   */
  @Nullable
  public String getPassword() {
    return this.password;
  }

  /**
   * Specify a database catalog to be applied to each Connection.
   *
   * @see Connection#setCatalog
   */
  public void setCatalog(@Nullable String catalog) {
    this.catalog = catalog;
  }

  /**
   * Return the database catalog to be applied to each Connection, if any.
   */
  @Nullable
  public String getCatalog() {
    return this.catalog;
  }

  /**
   * Specify a database schema to be applied to each Connection.
   *
   * @see Connection#setSchema
   */
  public void setSchema(@Nullable String schema) {
    this.schema = schema;
  }

  /**
   * Return the database schema to be applied to each Connection, if any.
   */
  @Nullable
  public String getSchema() {
    return this.schema;
  }

  /**
   * Specify arbitrary connection properties as key/value pairs,
   * to be passed to the Driver.
   * <p>Can also contain "user" and "password" properties. However,
   * any "username" and "password" bean properties specified on this
   * DataSource will override the corresponding connection properties.
   *
   * @see java.sql.Driver#connect(String, Properties)
   */
  public void setConnectionProperties(@Nullable Properties connectionProperties) {
    this.connectionProperties = connectionProperties;
  }

  /**
   * Return the connection properties to be passed to the Driver, if any.
   */
  @Nullable
  public Properties getConnectionProperties() {
    return this.connectionProperties;
  }

  /**
   * This implementation delegates to {@code getConnectionFromDriver},
   * using the default username and password of this DataSource.
   *
   * @see #getConnectionFromDriver(String, String)
   * @see #setUsername
   * @see #setPassword
   */
  @Override
  public Connection getConnection() throws SQLException {
    return getConnectionFromDriver(getUsername(), getPassword());
  }

  /**
   * This implementation delegates to {@code getConnectionFromDriver},
   * using the given username and password.
   *
   * @see #getConnectionFromDriver(String, String)
   */
  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return getConnectionFromDriver(username, password);
  }

  /**
   * Build properties for the Driver, including the given username and password (if any),
   * and obtain a corresponding Connection.
   *
   * @param username the name of the user
   * @param password the password to use
   * @return the obtained Connection
   * @throws SQLException in case of failure
   * @see java.sql.Driver#connect(String, Properties)
   */
  protected Connection getConnectionFromDriver(@Nullable String username, @Nullable String password) throws SQLException {
    Properties mergedProps = new Properties();
    Properties connProps = getConnectionProperties();
    if (connProps != null) {
      mergedProps.putAll(connProps);
    }
    if (username != null) {
      mergedProps.setProperty("user", username);
    }
    if (password != null) {
      mergedProps.setProperty("password", password);
    }

    Connection con = getConnectionFromDriver(mergedProps);
    if (this.catalog != null) {
      con.setCatalog(this.catalog);
    }
    if (this.schema != null) {
      con.setSchema(this.schema);
    }
    return con;
  }

  /**
   * Obtain a Connection using the given properties.
   * <p>Template method to be implemented by subclasses.
   *
   * @param props the merged connection properties
   * @return the obtained Connection
   * @throws SQLException in case of failure
   */
  protected abstract Connection getConnectionFromDriver(Properties props) throws SQLException;

}
