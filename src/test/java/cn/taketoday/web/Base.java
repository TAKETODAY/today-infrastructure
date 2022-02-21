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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 * 2018-12-24 19:17
 */
@Getter
@Setter
public class Base {

  protected Jetty jetty;
  protected ServletContext servletContext;
  protected long start = System.currentTimeMillis();
  protected AnnotationConfigServletWebApplicationContext context;

  @BeforeEach
  public void before() {
    final Jetty jetty = getJetty();
    jetty.start();
    context = jetty.getApplicationContext();
    servletContext = context.getServletContext();
  }

  @AfterEach
  public void after() {
    if (context != null) {
      context.close();
      getJetty().stop();
    }
  }

  public Jetty getJetty() {
    if (jetty == null) {
      jetty = createJetty();
    }
    return jetty;
  }

  protected Jetty createJetty() {
    return new Jetty();
  }

}
