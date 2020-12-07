package cn.taketoday.web.handler;

import java.lang.annotation.Annotation;

import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.http.HttpStatus;

/**
 * @author TODAY
 * @date 2020/12/7 21:44
 */
@SuppressWarnings("all")
public class DefaultResponseStatus implements ResponseStatus {
  private String reason;
  private HttpStatus value;

  public DefaultResponseStatus() {}

  public DefaultResponseStatus(HttpStatus value) {
    this(value, null);
  }

  public DefaultResponseStatus(HttpStatus value, String reason) {
    this.value = value;
    this.reason = reason;
  }

  @Override
  public HttpStatus value() {
    return value;
  }

  @Override
  public String reason() {
    return reason;
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return ResponseStatus.class;
  }

  public void setValue(final HttpStatus value) {
    this.value = value;
  }

  public void setReason(final String reason) {
    this.reason = reason;
  }
}
