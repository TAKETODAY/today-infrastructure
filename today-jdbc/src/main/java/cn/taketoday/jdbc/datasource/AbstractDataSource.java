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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.datasource;

import java.io.PrintWriter;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Abstract base class for  {@link DataSource}
 * implementations, taking care of the padding.
 *
 * <p>'Padding' in the context of this class means default implementations
 * for certain methods from the {@code DataSource} interface, such as
 * {@link #getLoginTimeout()}, {@link #setLoginTimeout(int)}, and so forth.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DriverManagerDataSource
 * @since 4.0
 */
public abstract class AbstractDataSource implements DataSource {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Returns 0, indicating the default system timeout is to be used.
   */
  @Override
  public int getLoginTimeout() throws SQLException {
    return 0;
  }

  /**
   * Setting a login timeout is not supported.
   */
  @Override
  public void setLoginTimeout(int timeout) throws SQLException {
    throw new UnsupportedOperationException("setLoginTimeout");
  }

  /**
   * LogWriter methods are not supported.
   */
  @Override
  public PrintWriter getLogWriter() {
    throw new UnsupportedOperationException("getLogWriter");
  }

  /**
   * LogWriter methods are not supported.
   */
  @Override
  public void setLogWriter(PrintWriter pw) throws SQLException {
    throw new UnsupportedOperationException("setLogWriter");
  }

  @Override
  public java.util.logging.Logger getParentLogger() {
    return java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return (T) this;
    }
    throw new SQLException("DataSource of type [" + getClass().getName() +
            "] cannot be unwrapped as [" + iface.getName() + "]");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return iface.isInstance(this);
  }

}
