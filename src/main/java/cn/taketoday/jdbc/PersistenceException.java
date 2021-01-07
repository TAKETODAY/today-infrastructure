package cn.taketoday.jdbc;

import cn.taketoday.jdbc.type.DataAccessException;

/**
 * @author TODAY
 * @date 2021/1/6 15:19
 */
public class PersistenceException extends DataAccessException {
  private static final long serialVersionUID = 1L;

  public PersistenceException() {
    super();
  }

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
