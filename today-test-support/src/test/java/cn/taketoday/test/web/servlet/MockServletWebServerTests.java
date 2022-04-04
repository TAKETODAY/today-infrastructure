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

package cn.taketoday.test.web.servlet;

import org.junit.jupiter.api.Test;

import cn.taketoday.mock.web.MockSessionCookieConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockServletWebServer}.
 *
 * @author Stephane Nicoll
 */
class MockServletWebServerTests {

  @Test
  void servletContextIsConfigured() {
    MockServletWebServer server = TestMockServletWebServer.create();
    assertThat(server.getServletContext()).isNotNull();
  }

  @Test
  void servletContextHasSessionCookieConfigConfigured() {
    MockServletWebServer server = TestMockServletWebServer.create();
    assertThat(server.getServletContext().getSessionCookieConfig()).isNotNull()
            .isInstanceOf(MockSessionCookieConfig.class);
  }

  private static final class TestMockServletWebServer extends MockServletWebServer {

    private TestMockServletWebServer(Initializer[] initializers, int port) {
      super(initializers, port);
    }

    static MockServletWebServer create(Initializer... initializers) {
      return new TestMockServletWebServer(initializers, 8080);
    }

  }

}
