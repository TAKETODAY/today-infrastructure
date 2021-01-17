package cn.taketoday.web.resolver;

import cn.taketoday.web.exception.WebRuntimeException;
import cn.taketoday.web.handler.MethodParameter;

/**
 * Parameter can't convert to target class
 *
 * @author TODAY
 * @date 2021/1/17 9:43
 * @since 3.0
 */
public class ParameterConversionException extends WebRuntimeException {
  private static final long serialVersionUID = 1L;

  private final String value;
  private final MethodParameter parameter;

  public ParameterConversionException(MethodParameter parameter, String value, Throwable cause) {
    super(cause);
    this.value = value;
    this.parameter = parameter;
  }

  public String getValue() {
    return value;
  }

  public MethodParameter getParameter() {
    return parameter;
  }
}
