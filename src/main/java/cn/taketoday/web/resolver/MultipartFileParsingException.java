package cn.taketoday.web.resolver;

import cn.taketoday.web.exception.WebRuntimeException;

/**
 * @author TODAY
 * @date 2021/1/17 10:41
 */
public class MultipartFileParsingException extends WebRuntimeException {
  public MultipartFileParsingException() {
    super();
  }

  public MultipartFileParsingException(String message) {
    super(message);
  }

  public MultipartFileParsingException(Throwable cause) {
    super(cause);
  }

  public MultipartFileParsingException(String message, Throwable cause) {
    super(message, cause);
  }
}
