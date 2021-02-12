package cn.taketoday.jdbc.type;

import cn.taketoday.context.NestedRuntimeException;

/**
 * @author TODAY
 * @date 2021/1/6 15:20
 */
public class DataAccessException extends NestedRuntimeException {
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
