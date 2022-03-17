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

package cn.taketoday.web.servlet;

import java.util.Enumeration;

import cn.taketoday.web.view.Model;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author TODAY 2021/4/15 21:03
 * @since 3.0
 */
public final class ServletRequestModelAdapter
        extends AbstractEnumerableModel implements Model {
  private final HttpServletRequest request;

  public ServletRequestModelAdapter(HttpServletRequest request) {
    this.request = request;
  }

  @Override
  public Object getAttribute(String name) {
    return request.getAttribute(name);
  }

  @Override
  public void setAttribute(String name, Object value) {
    request.setAttribute(name, value);
  }

  @Override
  public Object removeAttribute(String name) {
    request.removeAttribute(name);
    return null;
  }

  @Override
  protected Enumeration<String> getAttributes() {
    return request.getAttributeNames();
  }

}
