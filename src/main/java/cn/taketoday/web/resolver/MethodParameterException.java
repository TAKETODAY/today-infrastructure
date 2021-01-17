package cn.taketoday.web.resolver;

import cn.taketoday.web.exception.WebRuntimeException;
import cn.taketoday.web.handler.MethodParameter;

/**
 * MethodParameter can't be resolved
 *
 * @author TODAY
 * @date 2021/1/17 10:05
 */
public class MethodParameterException extends WebRuntimeException {
  private static final long serialVersionUID = 1L;

  private final MethodParameter parameter;

  public MethodParameterException(MethodParameter parameter) {
    this(parameter, null, null);
  }

  public MethodParameterException(MethodParameter parameter, String message) {
    this(parameter, message, null);
  }

  public MethodParameterException(MethodParameter parameter, Throwable cause) {
    this(parameter, null, cause);
  }

  public MethodParameterException(MethodParameter parameter, String message, Throwable cause) {
    super(message, cause);
    this.parameter = parameter;
  }

  public MethodParameter getParameter() {
    return parameter;
  }

}
