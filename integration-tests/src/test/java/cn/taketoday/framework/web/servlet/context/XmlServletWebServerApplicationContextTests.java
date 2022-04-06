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

package cn.taketoday.framework.web.servlet.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.framework.web.servlet.server.MockServletWebServerFactory;
import jakarta.servlet.Servlet;

import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link XmlServletWebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class XmlServletWebServerApplicationContextTests {

  private static final String PATH = XmlServletWebServerApplicationContextTests.class.getPackage().getName()
          .replace('.', '/') + "/";

  private static final String FILE = "exampleEmbeddedWebApplicationConfiguration.xml";

  private XmlServletWebServerApplicationContext context;

  @Test
  void createFromResource() {
    this.context = new XmlServletWebServerApplicationContext(new ClassPathResource(FILE, getClass()));
    verifyContext();
  }

  @Test
  void createFromResourceLocation() {
    this.context = new XmlServletWebServerApplicationContext(PATH + FILE);
    verifyContext();
  }

  @Test
  void createFromRelativeResourceLocation() {
    this.context = new XmlServletWebServerApplicationContext(getClass(), FILE);
    verifyContext();
  }

  @Test
  void loadAndRefreshFromResource() {
    this.context = new XmlServletWebServerApplicationContext();
    this.context.load(new ClassPathResource(FILE, getClass()));
    this.context.refresh();
    verifyContext();
  }

  @Test
  void loadAndRefreshFromResourceLocation() {
    this.context = new XmlServletWebServerApplicationContext();
    this.context.load(PATH + FILE);
    this.context.refresh();
    verifyContext();
  }

  @Test
  void loadAndRefreshFromRelativeResourceLocation() {
    this.context = new XmlServletWebServerApplicationContext();
    this.context.load(getClass(), FILE);
    this.context.refresh();
    verifyContext();
  }

  private void verifyContext() {
    MockServletWebServerFactory factory = this.context.getBean(MockServletWebServerFactory.class);
    Servlet servlet = this.context.getBean(Servlet.class);
    then(factory.getServletContext()).should().addServlet("servlet", servlet);
  }

}
