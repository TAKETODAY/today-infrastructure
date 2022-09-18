package cn.taketoday.jdbc.result;

import java.io.Closeable;

/**
 * Iterable {@link java.sql.ResultSet}. Needs to be closeable, because allowing
 * manual iteration means it's impossible to know when to close the ResultSet
 * and Connection.
 *
 * @author aldenquimby@gmail.com
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public abstract class ResultSetIterable<T> implements Iterable<T>, Closeable, AutoCloseable {
  private boolean autoCloseConnection = false;

  public boolean isAutoCloseConnection() {
    return this.autoCloseConnection;
  }

  public void setAutoCloseConnection(boolean autoCloseConnection) {
    this.autoCloseConnection = autoCloseConnection;
  }

  // override close to not throw
  @Override
  public abstract void close();

}
