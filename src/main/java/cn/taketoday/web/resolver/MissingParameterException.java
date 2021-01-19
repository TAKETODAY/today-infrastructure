package cn.taketoday.web.resolver;

import cn.taketoday.web.handler.MethodParameter;

/**
 * Missing Parameter Exception
 *
 * @author TODAY
 * @date 2021/1/17 10:03
 */
public class MissingParameterException extends MethodParameterException {

  private final String type;

  public MissingParameterException(MethodParameter parameter) {
    this("Parameter", parameter);
  }

  public MissingParameterException(String type, MethodParameter parameter) {
    super(parameter, "Required " + type + " '" + parameter.getName() + "' is not present");
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
