package cn.taketoday.jdbc.type;

/**
 * @author TODAY
 * @date 2021/1/6 15:20
 */
public class DataAccessException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DataAccessException() {}

  public DataAccessException(String message) {
    super(message);
  }

  public DataAccessException(Throwable cause) {
    super(cause);
  }

  public DataAccessException(String message, Throwable cause) {
    super(message, cause);
  }

}
