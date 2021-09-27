package cn.taketoday.jdbc;

/**
 * Represents an exception thrown by today-jdbc.
 *
 * @author TODAY
 */
public class PersistenceException extends DataAccessException {
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
