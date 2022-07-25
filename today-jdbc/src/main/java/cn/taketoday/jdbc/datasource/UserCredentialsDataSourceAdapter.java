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
import java.sql.SQLException;

import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * An adapter for a target JDBC {@link javax.sql.DataSource}, applying the specified
 * user credentials to every standard {@code getConnection()} call, implicitly
 * invoking {@code getConnection(username, password)} on the target.
 * All other methods simply delegate to the corresponding methods of the
 * target DataSource.
 *
 * <p>Can be used to proxy a target JNDI DataSource that does not have user
 * credentials configured. Client code can work with this DataSource as usual,
 * using the standard {@code getConnection()} call.
 *
 * <p>In the following example, client code can simply transparently work with
 * the preconfigured "myDataSource", implicitly accessing "myTargetDataSource"
 * with the specified user credentials.
 *
 * <pre class="code">
 * &lt;bean id="myTargetDataSource" class="cn.taketoday.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/jdbc/myds"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myDataSource" class="cn.taketoday.jdbc.datasource.UserCredentialsDataSourceAdapter"&gt;
 *   &lt;property name="targetDataSource" ref="myTargetDataSource"/&gt;
 *   &lt;property name="username" value="myusername"/&gt;
 *   &lt;property name="password" value="mypassword"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>If the "username" is empty, this proxy will simply delegate to the
 * standard {@code getConnection()} method of the target DataSource.
 * This can be used to keep a UserCredentialsDataSourceAdapter bean definition
 * just for the <i>option</i> of implicitly passing in user credentials if
 * the particular target DataSource requires it.
 *
 * @author Juergen Hoeller
 * @see #getConnection
 * @since 4.0
 */
public class UserCredentialsDataSourceAdapter extends DelegatingDataSource {

  @Nullable
  private String username;

  @Nullable
  private String password;

  @Nullable
  private String catalog;

  @Nullable
  private String schema;

  private final ThreadLocal<JdbcUserCredentials> threadBoundCredentials =
          new NamedThreadLocal<>("Current JDBC user credentials");

  /**
   * Set the default username that this adapter should use for retrieving Connections.
   * <p>Default is no specific user. Note that an explicitly specified username
   * will always override any username/password specified at the DataSource level.
   *
   * @see #setPassword
   * @see #setCredentialsForCurrentThread(String, String)
   * @see #getConnection(String, String)
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Set the default user's password that this adapter should use for retrieving Connections.
   * <p>Default is no specific password. Note that an explicitly specified username
   * will always override any username/password specified at the DataSource level.
   *
   * @see #setUsername
   * @see #setCredentialsForCurrentThread(String, String)
   * @see #getConnection(String, String)
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Specify a database catalog to be applied to each retrieved Connection.
   *
   * @see Connection#setCatalog
   */
  public void setCatalog(String catalog) {
    this.catalog = catalog;
  }

  /**
   * Specify a database schema to be applied to each retrieved Connection.
   *
   * @see Connection#setSchema
   */
  public void setSchema(String schema) {
    this.schema = schema;
  }

  /**
   * Set user credententials for this proxy and the current thread.
   * The given username and password will be applied to all subsequent
   * {@code getConnection()} calls on this DataSource proxy.
   * <p>This will override any statically specified user credentials,
   * that is, values of the "username" and "password" bean properties.
   *
   * @param username the username to apply
   * @param password the password to apply
   * @see #removeCredentialsFromCurrentThread
   */
  public void setCredentialsForCurrentThread(String username, String password) {
    this.threadBoundCredentials.set(new JdbcUserCredentials(username, password));
  }

  /**
   * Remove any user credentials for this proxy from the current thread.
   * Statically specified user credentials apply again afterwards.
   *
   * @see #setCredentialsForCurrentThread
   */
  public void removeCredentialsFromCurrentThread() {
    this.threadBoundCredentials.remove();
  }

  /**
   * Determine whether there are currently thread-bound credentials,
   * using them if available, falling back to the statically specified
   * username and password (i.e. values of the bean properties) otherwise.
   * <p>Delegates to {@link #doGetConnection(String, String)} with the
   * determined credentials as parameters.
   *
   * @see #doGetConnection
   */
  @Override
  public Connection getConnection() throws SQLException {
    JdbcUserCredentials threadCredentials = this.threadBoundCredentials.get();
    Connection con = threadCredentials != null
                     ? doGetConnection(threadCredentials.username, threadCredentials.password)
                     : doGetConnection(this.username, this.password);

    if (this.catalog != null) {
      con.setCatalog(this.catalog);
    }
    if (this.schema != null) {
      con.setSchema(this.schema);
    }
    return con;
  }

  /**
   * Simply delegates to {@link #doGetConnection(String, String)},
   * keeping the given user credentials as-is.
   */
  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return doGetConnection(username, password);
  }

  /**
   * This implementation delegates to the {@code getConnection(username, password)}
   * method of the target DataSource, passing in the specified user credentials.
   * If the specified username is empty, it will simply delegate to the standard
   * {@code getConnection()} method of the target DataSource.
   *
   * @param username the username to use
   * @param password the password to use
   * @return the Connection
   * @see javax.sql.DataSource#getConnection(String, String)
   * @see javax.sql.DataSource#getConnection()
   */
  protected Connection doGetConnection(@Nullable String username, @Nullable String password) throws SQLException {
    Assert.state(getTargetDataSource() != null, "'targetDataSource' is required");
    if (StringUtils.isNotEmpty(username)) {
      return getTargetDataSource().getConnection(username, password);
    }
    else {
      return getTargetDataSource().getConnection();
    }
  }

  /**
   * Inner class used as ThreadLocal value.
   */
  private record JdbcUserCredentials(String username, String password) {

    @Override
    public String toString() {
      return "JdbcUserCredentials[username='" + this.username + "',password='" + this.password + "']";
    }
  }

}
