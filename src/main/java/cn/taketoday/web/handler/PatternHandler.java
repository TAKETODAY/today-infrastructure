/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.OrderUtils;
import cn.taketoday.lang.Assert;

/**
 * Pattern handler match in runtime
 *
 * @author TODAY 2019-12-25 14:51
 */
public final class PatternHandler implements Serializable, Ordered {
  private static final long serialVersionUID = 1L;

  private final String pattern; // path pattern
  private final Object handler; // real handler

  public PatternHandler(String pattern, Object handler) {
    Assert.notNull(pattern, "pattern must not be null");
    Assert.notNull(handler, "handler must not be null");
    this.pattern = pattern;
    this.handler = handler;
  }

  public String getPattern() {
    return pattern;
  }

  public Object getHandler() {
    return handler;
  }

  @Override
  public int getOrder() {
    return OrderUtils.getOrderOrLowest(handler);
  }

  @Override
  public int hashCode() {
    return Objects.hash(handler, pattern);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof PatternHandler) {
      final PatternHandler other = (PatternHandler) obj;
      return Objects.equals(other.pattern, pattern)
              && Objects.equals(other.handler, handler);
    }
    return false;
  }

  @Override
  public String toString() {
    return new StringBuilder()
            .append("[pattern=")
            .append(pattern)
            .append(", handler=")
            .append(handler)
            .append(']')
            .toString();
  }

}
