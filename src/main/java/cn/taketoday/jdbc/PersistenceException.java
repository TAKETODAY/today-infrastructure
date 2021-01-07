package cn.taketoday.jdbc;

import cn.taketoday.jdbc.type.DataAccessException;

/**
 * Represents an exception thrown by today-jdbc.
 *
 * @author TODAY
 */
public class PersistenceException extends DataAccessException {

  public PersistenceException() {}

  public PersistenceException(String message) {
    super(message);
  }

  public PersistenceException(String message, Throwable cause) {
    super(message, cause);
  }

  public PersistenceException(Throwable cause) {
    super(cause);
  }
}
