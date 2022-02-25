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

package cn.taketoday.jdbc.datasource;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Simple implementation of the standard JDBC {@link javax.sql.DataSource} interface,
 * configuring a plain old JDBC {@link Driver} via bean properties, and
 * returning a new {@link Connection} from every {@code getConnection} call.
 *
 * <p><b>NOTE: This class is not an actual connection pool; it does not actually
 * pool Connections.</b> It just serves as simple replacement for a full-blown
 * connection pool, implementing the same standard interface, but creating new
 * Connections on every call.
 *
 * <p>In a Jakarta EE container, it is recommended to use a JNDI DataSource provided by
 * the container. Such a DataSource can be exposed as a DataSource bean in a Framework
 * ApplicationContext via {@link cn.taketoday.jndi.JndiObjectFactoryBean},
 * for seamless switching to and from a local DataSource bean like this class.
 *
 * <p>This {@code SimpleDriverDataSource} class was originally designed alongside
 * <a href="https://commons.apache.org/proper/commons-dbcp">Apache Commons DBCP</a>
 * and <a href="https://sourceforge.net/projects/c3p0">C3P0</a>, featuring bean-style
 * {@code BasicDataSource}/{@code ComboPooledDataSource} classes with configuration
 * properties for local resource setups. For a modern JDBC connection pool, consider
 * <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a> instead,
 * exposing a corresponding {@code HikariDataSource} instance to the application.
 *
 * @author Juergen Hoeller
 * @see DriverManagerDataSource
 * @since 4.0
 */
public class SimpleDriverDataSource extends AbstractDriverBasedDataSource {

  @Nullable
  private Driver driver;

  /**
   * Constructor for bean-style configuration.
   */
  public SimpleDriverDataSource() {
  }

  /**
   * Create a new DriverManagerDataSource with the given standard Driver parameters.
   *
   * @param driver the JDBC Driver object
   * @param url the JDBC URL to use for accessing the DriverManager
   * @see Driver#connect(String, Properties)
   */
  public SimpleDriverDataSource(Driver driver, String url) {
    setDriver(driver);
    setUrl(url);
  }

  /**
   * Create a new DriverManagerDataSource with the given standard Driver parameters.
   *
   * @param driver the JDBC Driver object
   * @param url the JDBC URL to use for accessing the DriverManager
   * @param username the JDBC username to use for accessing the DriverManager
   * @param password the JDBC password to use for accessing the DriverManager
   * @see Driver#connect(String, Properties)
   */
  public SimpleDriverDataSource(Driver driver, String url, String username, String password) {
    setDriver(driver);
    setUrl(url);
    setUsername(username);
    setPassword(password);
  }

  /**
   * Create a new DriverManagerDataSource with the given standard Driver parameters.
   *
   * @param driver the JDBC Driver object
   * @param url the JDBC URL to use for accessing the DriverManager
   * @param conProps the JDBC connection properties
   * @see Driver#connect(String, Properties)
   */
  public SimpleDriverDataSource(Driver driver, String url, Properties conProps) {
    setDriver(driver);
    setUrl(url);
    setConnectionProperties(conProps);
  }

  /**
   * Specify the JDBC Driver implementation class to use.
   * <p>An instance of this Driver class will be created and held
   * within the SimpleDriverDataSource.
   *
   * @see #setDriver
   */
  public void setDriverClass(Class<? extends Driver> driverClass) {
    this.driver = BeanUtils.newInstance(driverClass);
  }

  /**
   * Specify the JDBC Driver instance to use.
   * <p>This allows for passing in a shared, possibly pre-configured
   * Driver instance.
   *
   * @see #setDriverClass
   */
  public void setDriver(@Nullable Driver driver) {
    this.driver = driver;
  }

  /**
   * Return the JDBC Driver instance to use.
   */
  @Nullable
  public Driver getDriver() {
    return this.driver;
  }

  @Override
  protected Connection getConnectionFromDriver(Properties props) throws SQLException {
    Driver driver = getDriver();
    String url = getUrl();
    Assert.notNull(driver, "Driver must not be null");
    if (logger.isDebugEnabled()) {
      logger.debug("Creating new JDBC Driver Connection to [{}]", url);
    }
    return driver.connect(url, props);
  }

}
