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
import jakarta.servlet.ServletContext;

/**
 * @author TODAY 2021/4/15 21:01
 * @since 3.0
 */
public final class ServletContextModelAdapter
        extends AbstractEnumerableModel implements Model {
  private final ServletContext context;

  public ServletContextModelAdapter(ServletContext context) {
    this.context = context;
  }

  @Override
  public Object getAttribute(String name) {
    return context.getAttribute(name);
  }

  @Override
  public void setAttribute(String name, Object value) {
    context.setAttribute(name, value);
  }

  @Override
  public Object removeAttribute(String name) {
    context.removeAttribute(name);
    return null;
  }

  @Override
  protected Enumeration<String> getAttributes() {
    return context.getAttributeNames();
  }

}
