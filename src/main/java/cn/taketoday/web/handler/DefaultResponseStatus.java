/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
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
