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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;

/**
 * Simple implementation of the standard JDBC {@link javax.sql.DataSource} interface,
 * configuring the plain old JDBC {@link DriverManager} via bean properties, and
 * returning a new {@link Connection} from every {@code getConnection} call.
 *
 * <p><b>NOTE: This class is not an actual connection pool; it does not actually
 * pool Connections.</b> It just serves as simple replacement for a full-blown
 * connection pool, implementing the same standard interface, but creating new
 * Connections on every call.
 *
 * <p>Useful for test or standalone environments outside of a Jakarta EE container, either
 * as a DataSource bean in a corresponding ApplicationContext or in conjunction with
 * a simple JNDI environment. Pool-assuming {@code Connection.close()} calls will
 * simply close the Connection, so any DataSource-aware persistence code should work.
 *
 * <p><b>NOTE: Within special class loading environments such as OSGi, this class
 * is effectively superseded by {@link SimpleDriverDataSource} due to general class
 * loading issues with the JDBC DriverManager that be resolved through direct Driver
 * usage (which is exactly what SimpleDriverDataSource does).</b>
 *
 * <p>In a Jakarta EE container, it is recommended to use a JNDI DataSource provided by
 * the container. Such a DataSource can be exposed as a DataSource bean in a Spring
 * ApplicationContext via {@link cn.taketoday.jndi.JndiObjectFactoryBean},
 * for seamless switching to and from a local DataSource bean like this class.
 * For tests, you can then either set up a mock JNDI environment through
 * {@link cn.taketoday.mock.jndi.SimpleNamingContextBuilder}, or switch the
 * bean definition to a local DataSource (which is simpler and thus recommended).
 *
 * <p>This {@code DriverManagerDataSource} class was originally designed alongside
 * <a href="https://commons.apache.org/proper/commons-dbcp">Apache Commons DBCP</a>
 * and <a href="https://sourceforge.net/projects/c3p0">C3P0</a>, featuring bean-style
 * {@code BasicDataSource}/{@code ComboPooledDataSource} classes with configuration
 * properties for local resource setups. For a modern JDBC connection pool, consider
 * <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a> instead,
 * exposing a corresponding {@code HikariDataSource} instance to the application.
 *
 * @author Juergen Hoeller
 * @see SimpleDriverDataSource
 * @since 4.0
 */
public class DriverManagerDataSource extends AbstractDriverBasedDataSource {

  /**
   * Constructor for bean-style configuration.
   */
  public DriverManagerDataSource() {
  }

  /**
   * Create a new DriverManagerDataSource with the given JDBC URL,
   * not specifying a username or password for JDBC access.
   *
   * @param url the JDBC URL to use for accessing the DriverManager
   * @see DriverManager#getConnection(String)
   */
  public DriverManagerDataSource(String url) {
    setUrl(url);
  }

  /**
   * Create a new DriverManagerDataSource with the given standard
   * DriverManager parameters.
   *
   * @param url the JDBC URL to use for accessing the DriverManager
   * @param username the JDBC username to use for accessing the DriverManager
   * @param password the JDBC password to use for accessing the DriverManager
   * @see DriverManager#getConnection(String, String, String)
   */
  public DriverManagerDataSource(String url, String username, String password) {
    setUrl(url);
    setUsername(username);
    setPassword(password);
  }

  /**
   * Create a new DriverManagerDataSource with the given JDBC URL,
   * not specifying a username or password for JDBC access.
   *
   * @param url the JDBC URL to use for accessing the DriverManager
   * @param conProps the JDBC connection properties
   * @see DriverManager#getConnection(String)
   */
  public DriverManagerDataSource(String url, Properties conProps) {
    setUrl(url);
    setConnectionProperties(conProps);
  }

  /**
   * Set the JDBC driver class name. This driver will get initialized
   * on startup, registering itself with the JDK's DriverManager.
   * <p><b>NOTE: DriverManagerDataSource is primarily intended for accessing
   * <i>pre-registered</i> JDBC drivers.</b> If you need to register a new driver,
   * consider using {@link SimpleDriverDataSource} instead. Alternatively, consider
   * initializing the JDBC driver yourself before instantiating this DataSource.
   * The "driverClassName" property is mainly preserved for backwards compatibility,
   * as well as for migrating between Commons DBCP and this DataSource.
   *
   * @see DriverManager#registerDriver(java.sql.Driver)
   * @see SimpleDriverDataSource
   */
  public void setDriverClassName(String driverClassName) {
    Assert.hasText(driverClassName, "Property 'driverClassName' must not be empty");
    String driverClassNameToUse = driverClassName.trim();
    try {
      Class.forName(driverClassNameToUse, true, ClassUtils.getDefaultClassLoader());
    }
    catch (ClassNotFoundException ex) {
      throw new IllegalStateException("Could not load JDBC driver class [" + driverClassNameToUse + "]", ex);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Loaded JDBC driver: " + driverClassNameToUse);
    }
  }

  @Override
  protected Connection getConnectionFromDriver(Properties props) throws SQLException {
    String url = getUrl();
    Assert.state(url != null, "'url' not set");
    if (logger.isDebugEnabled()) {
      logger.debug("Creating new JDBC DriverManager Connection to [" + url + "]");
    }
    return getConnectionFromDriverManager(url, props);
  }

  /**
   * Getting a Connection using the nasty static from DriverManager is extracted
   * into a protected method to allow for easy unit testing.
   *
   * @see DriverManager#getConnection(String, Properties)
   */
  protected Connection getConnectionFromDriverManager(String url, Properties props) throws SQLException {
    return DriverManager.getConnection(url, props);
  }

}
