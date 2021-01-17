package cn.taketoday.web.resolver;

import cn.taketoday.web.handler.MethodParameter;

/**
 * Parameter can't convert to target class
 *
 * @author TODAY
 * @date 2021/1/17 9:43
 * @since 3.0
 */
public class ParameterConversionException extends MethodParameterException {
  private static final long serialVersionUID = 1L;

  private final String value;

  public ParameterConversionException(MethodParameter parameter, String value, Throwable cause) {
    super(parameter, "Cant convert '" + value + "' to " + parameter.getParameterClass(), cause);
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
