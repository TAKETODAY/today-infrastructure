package cn.taketoday.jdbc;

import java.io.Serial;

import cn.taketoday.dao.DataAccessException;

/**
 * Represents an exception thrown by today-jdbc.
 *
 * @author TODAY
 */
public class PersistenceException extends DataAccessException {
  @Serial
  private static final long serialVersionUID = 1L;

  public PersistenceException() { }

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
