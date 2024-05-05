/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler;

import org.junit.jupiter.api.Test;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.mock.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/11 17:45
 */
class SimpleNotFoundHandlerTests {
  SimpleNotFoundHandler notFoundHandler = new SimpleNotFoundHandler();

  HttpMockRequestImpl request = new HttpMockRequestImpl();
  MockHttpServletResponse response = new MockHttpServletResponse();

  @Test
  void handleNotFound() throws Throwable {
    request.setRequestURI("/not-found");
    ServletRequestContext requestContext = new ServletRequestContext(null, request, response);
    assertThat(notFoundHandler.handleNotFound(requestContext)).isEqualTo(HttpRequestHandler.NONE_RETURN_VALUE);
    assertThat(requestContext.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value()).isEqualTo(response.getStatus());
    assertThat(response.isCommitted()).isTrue();

  }

}