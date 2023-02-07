/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.socket;

import org.junit.jupiter.api.BeforeEach;

import cn.taketoday.http.server.ServerHttpRequest;
import cn.taketoday.http.server.ServerHttpResponse;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletRequestContext;

/**
 * Base class for tests using {@link ServerHttpRequest} and {@link ServerHttpResponse}.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractHttpRequestTests {

  protected RequestContext request;

  protected MockHttpServletRequest servletRequest;

  protected MockHttpServletResponse servletResponse;

  @BeforeEach
  protected void setup() {
    resetRequestAndResponse();
  }

  protected void setRequest(String method, String requestUri) {
    this.servletRequest.setMethod(method);
    this.servletRequest.setRequestURI(requestUri);
    this.request = new ServletRequestContext(null, this.servletRequest, servletResponse);
  }

  protected void resetRequestAndResponse() {
    resetResponse();
    resetRequest();
  }

  protected void resetRequest() {
    this.servletRequest = new MockHttpServletRequest();
    this.servletRequest.setAsyncSupported(true);
    this.request = new ServletRequestContext(null, this.servletRequest, servletResponse);
  }

  protected void resetResponse() {
    this.servletResponse = new MockHttpServletResponse();
  }

}
