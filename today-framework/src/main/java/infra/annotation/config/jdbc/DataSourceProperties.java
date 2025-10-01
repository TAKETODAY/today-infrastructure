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

package infra.annotation.config.jdbc;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.InitializingBean;
import infra.context.properties.ConfigurationProperties;
import infra.jdbc.config.DataSourceBuilder;
import infra.jdbc.config.DatabaseDriver;
import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * Base class for configuration of a data source.
 *
 * @author Dave Syer
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 * @author Benedikt Ritter
 * @author Eddú Meléndez
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/23 17:13
 */
@SuppressWarnings("NullAway")
@ConfigurationProperties(prefix = "datasource")
public class DataSourceProperties implements BeanClassLoaderAware, InitializingBean {

  @Nullable
  private ClassLoader classLoader;

  /**
   * Whether to generate a random datasource name.
   */
  private boolean generateUniqueName = true;

  /**
   * Datasource name to use if "generate-unique-name" is false. Defaults to "testdb"
   * when using an embedded database, otherwise null.
   */
  @Nullable
  private String name;

  /**
   * Fully qualified name of the connection pool implementation to use. By default, it
   * is auto-detected from the classpath.
   */
  @Nullable
  private Class<? extends DataSource> type;

  /**
   * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
   */
  @Nullable
  private String driverClassName;

  /**
   * JDBC URL of the database.
   */
  @Nullable
  private String url;

  /**
   * Login username of the database.
   */
  @Nullable
  private String username;

  /**
   * Login password of the database.
   */
  @Nullable
  private String password;

  /**
   * JNDI location of the datasource. Class, url, username and password are ignored when
   * set.
   */
  @Nullable
  private String jndiName;

  /**
   * Connection details for an embedded database. Defaults to the most suitable embedded
   * database that is available on the classpath.
   */
  @Nullable
  private EmbeddedDatabaseConnection embeddedDatabaseConnection;

  private Xa xa = new Xa();

  @Nullable
  private String uniqueName;

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.embeddedDatabaseConnection == null) {
      this.embeddedDatabaseConnection = EmbeddedDatabaseConnection.get(this.classLoader);
    }
  }

  /**
   * Initialize a {@link DataSourceBuilder} with the state of this instance.
   *
   * @return a {@link DataSourceBuilder} initialized with the customizations defined on
   * this instance
   */
  public DataSourceBuilder<?> initializeDataSourceBuilder() {
    return DataSourceBuilder.create(getClassLoader())
            .type(getType())
            .driverClassName(determineDriverClassName())
            .url(determineUrl())
            .username(determineUsername())
            .password(determinePassword());
  }

  public boolean isGenerateUniqueName() {
    return this.generateUniqueName;
  }

  public void setGenerateUniqueName(boolean generateUniqueName) {
    this.generateUniqueName = generateUniqueName;
  }

  @Nullable
  public String getName() {
    return this.name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  @Nullable
  public Class<? extends DataSource> getType() {
    return this.type;
  }

  public void setType(@Nullable Class<? extends DataSource> type) {
    this.type = type;
  }

  /**
   * Return the configured driver or {@code null} if none was configured.
   *
   * @return the configured driver
   * @see #determineDriverClassName()
   */
  @Nullable
  public String getDriverClassName() {
    return this.driverClassName;
  }

  public void setDriverClassName(@Nullable String driverClassName) {
    this.driverClassName = driverClassName;
  }

  /**
   * Determine the driver to use based on this configuration and the environment.
   *
   * @return the driver to use
   */
  public String determineDriverClassName() {
    if (StringUtils.hasText(this.driverClassName)) {
      if (!driverClassIsLoadable()) {
        throw new IllegalStateException("Cannot load driver class: " + this.driverClassName);
      }
      return this.driverClassName;
    }
    String driverClassName = null;
    if (StringUtils.hasText(this.url)) {
      driverClassName = DatabaseDriver.fromJdbcUrl(this.url).getDriverClassName();
    }
    if (StringUtils.isBlank(driverClassName)) {
      Assert.state(embeddedDatabaseConnection != null, "No EmbeddedDatabaseConnection.");
      driverClassName = embeddedDatabaseConnection.getDriverClassName();
    }
    if (StringUtils.isBlank(driverClassName)) {
      throw new DataSourceBeanCreationException(
              "Failed to determine a suitable driver class", this, embeddedDatabaseConnection);
    }
    return driverClassName;
  }

  private boolean driverClassIsLoadable() {
    try {
      ClassUtils.forName(this.driverClassName, null);
      return true;
    }
    catch (UnsupportedClassVersionError ex) {
      // Driver library has been compiled with a later JDK, propagate error
      throw ex;
    }
    catch (Throwable ex) {
      return false;
    }
  }

  /**
   * Return the configured url or {@code null} if none was configured.
   *
   * @return the configured url
   * @see #determineUrl()
   */
  @Nullable
  public String getUrl() {
    return this.url;
  }

  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  /**
   * Determine the url to use based on this configuration and the environment.
   *
   * @return the url to use
   */
  public String determineUrl() {
    if (StringUtils.hasText(this.url)) {
      return this.url;
    }
    String databaseName = determineDatabaseName();
    Assert.state(embeddedDatabaseConnection != null, "afterPropertiesSet is required");
    String url = (databaseName != null) ? embeddedDatabaseConnection.getUrl(databaseName) : null;
    if (StringUtils.isBlank(url)) {
      throw new DataSourceBeanCreationException("Failed to determine suitable jdbc url", this,
              this.embeddedDatabaseConnection);
    }
    return url;
  }

  /**
   * Determine the name to used based on this configuration.
   *
   * @return the database name to use or {@code null}
   */
  @Nullable
  public String determineDatabaseName() {
    if (this.generateUniqueName) {
      if (this.uniqueName == null) {
        this.uniqueName = UUID.randomUUID().toString();
      }
      return this.uniqueName;
    }
    if (StringUtils.isNotEmpty(this.name)) {
      return this.name;
    }
    if (this.embeddedDatabaseConnection != EmbeddedDatabaseConnection.NONE) {
      return "testdb";
    }
    return null;
  }

  /**
   * Return the configured username or {@code null} if none was configured.
   *
   * @return the configured username
   * @see #determineUsername()
   */
  @Nullable
  public String getUsername() {
    return this.username;
  }

  public void setUsername(@Nullable String username) {
    this.username = username;
  }

  /**
   * Determine the username to use based on this configuration and the environment.
   *
   * @return the username to use
   */
  @Nullable
  public String determineUsername() {
    if (StringUtils.hasText(this.username)) {
      return this.username;
    }
    if (EmbeddedDatabaseConnection.isEmbedded(determineDriverClassName(), determineUrl())) {
      return "sa";
    }
    return null;
  }

  /**
   * Return the configured password or {@code null} if none was configured.
   *
   * @return the configured password
   * @see #determinePassword()
   */
  @Nullable
  public String getPassword() {
    return this.password;
  }

  public void setPassword(@Nullable String password) {
    this.password = password;
  }

  /**
   * Determine the password to use based on this configuration and the environment.
   *
   * @return the password to use
   */
  @Nullable
  public String determinePassword() {
    if (StringUtils.hasText(this.password)) {
      return this.password;
    }
    if (EmbeddedDatabaseConnection.isEmbedded(determineDriverClassName(), determineUrl())) {
      return "";
    }
    return null;
  }

  @Nullable
  public String getJndiName() {
    return this.jndiName;
  }

  /**
   * Allows the DataSource to be managed by the container and obtained via JNDI. The
   * {@code URL}, {@code driverClassName}, {@code username} and {@code password} fields
   * will be ignored when using JNDI lookups.
   *
   * @param jndiName the JNDI name
   */
  public void setJndiName(@Nullable String jndiName) {
    this.jndiName = jndiName;
  }

  @Nullable
  public EmbeddedDatabaseConnection getEmbeddedDatabaseConnection() {
    return this.embeddedDatabaseConnection;
  }

  public void setEmbeddedDatabaseConnection(@Nullable EmbeddedDatabaseConnection embeddedDatabaseConnection) {
    this.embeddedDatabaseConnection = embeddedDatabaseConnection;
  }

  @Nullable
  public ClassLoader getClassLoader() {
    return this.classLoader;
  }

  public Xa getXa() {
    return this.xa;
  }

  public void setXa(Xa xa) {
    this.xa = xa;
  }

  /**
   * XA Specific datasource settings.
   */
  public static class Xa {

    /**
     * XA datasource fully qualified name.
     */
    private String dataSourceClassName;

    /**
     * Properties to pass to the XA data source.
     */
    private Map<String, String> properties = new LinkedHashMap<>();

    public String getDataSourceClassName() {
      return this.dataSourceClassName;
    }

    public void setDataSourceClassName(String dataSourceClassName) {
      this.dataSourceClassName = dataSourceClassName;
    }

    public Map<String, String> getProperties() {
      return this.properties;
    }

    public void setProperties(Map<String, String> properties) {
      this.properties = properties;
    }

  }

  static class DataSourceBeanCreationException extends BeanCreationException {

    public final DataSourceProperties properties;

    public final EmbeddedDatabaseConnection connection;

    DataSourceBeanCreationException(String message,
            DataSourceProperties properties, EmbeddedDatabaseConnection connection) {
      super(message);
      this.properties = properties;
      this.connection = connection;
    }

  }

}
