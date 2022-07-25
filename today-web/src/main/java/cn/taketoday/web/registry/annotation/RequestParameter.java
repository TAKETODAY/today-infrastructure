/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.registry.annotation;

import java.util.Objects;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/4/22 0:33
 * @see AnnotationMappingInfo
 * @since 3.0
 */
final class RequestParameter {
  private final String name;
  private final String value;

  private RequestParameter(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final RequestParameter that))
      return false;
    return Objects.equals(name, that.name)
            && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  @Override
  public String toString() {
    if (value == null) {
      return name;
    }
    return name + "=" + value;
  }

  // static

  /**
   * Parsing a string like 'paramName=xxxxx' into a {@link RequestParameter}
   *
   * @param param parameter string
   * @throws IllegalArgumentException param string is not valid (format error),or param string cannot empty
   */
  static RequestParameter parse(String param) {
    Assert.hasLength(param, "param string cannot empty");
    final int indexOfEquals = param.indexOf('=');
    if (indexOfEquals > -1) {
      String value = param.substring(indexOfEquals + 1);
      if (StringUtils.isEmpty(value)) {
        value = null;
      }
      Assert.isTrue(indexOfEquals != 0, "param string is not valid");
      final String name = param.substring(0, indexOfEquals);
      return new RequestParameter(name, value);
    }
    else {
      return new RequestParameter(param, null);
    }
  }

  /**
   * test parameter value
   *
   * @since 4.0
   */
  public boolean matches(RequestContext context) {
    final String parameter = context.getParameter(name);
    if (parameter != null) {
      // test parameter value
      final String value = getValue();
      return value == null || Objects.equals(value, parameter);
    }
    return false;
  }

}
