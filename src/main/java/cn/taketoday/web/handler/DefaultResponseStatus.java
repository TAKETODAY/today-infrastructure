package cn.taketoday.web.handler;

import java.lang.annotation.Annotation;
import java.util.Objects;

import cn.taketoday.context.utils.StringUtils;
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
    this(value, value.getReasonPhrase());
  }

  public DefaultResponseStatus(HttpStatus value, String reason) {
    this.value = value;
    this.reason = reason;
  }

  public DefaultResponseStatus(ResponseStatus status) {
    this.value = status.value();
    this.reason = status.reason();
    if (StringUtils.isEmpty(this.reason)) {
      this.reason = status.value().getReasonPhrase();
    }
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DefaultResponseStatus)) return false;
    final DefaultResponseStatus that = (DefaultResponseStatus) o;
    return Objects.equals(reason, that.reason) && value == that.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(reason, value);
  }
}
