/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import org.junit.Test;

import javax.servlet.ServletContext;

import cn.taketoday.web.Base;

import static org.junit.Assert.assertEquals;

/**
 * @author TODAY <br>
 *         2020-04-28 10:46
 */
public class StandardWebServletApplicationContextTest extends Base {

  @Test
  public void testStandardWebServletApplicationContext() {
    final ServletContext servletContext = context.getServletContext();
    assertEquals(servletContext, getServletContext());
    assertEquals(context.getContextPath(), getServletContext().getContextPath());
  }

}
