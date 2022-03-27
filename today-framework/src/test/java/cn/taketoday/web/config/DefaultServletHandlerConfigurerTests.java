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

package cn.taketoday.web.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.mock.MockHttpServletResponse;
import cn.taketoday.web.mock.MockRequestDispatcher;
import cn.taketoday.web.mock.MockServletContext;
import cn.taketoday.web.registry.SimpleUrlHandlerRegistry;
import cn.taketoday.web.resource.DefaultServletHttpRequestHandler;
import cn.taketoday.web.servlet.ServletRequestContext;
import jakarta.servlet.RequestDispatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/27 09:37
 */
class DefaultServletHandlerConfigurerTests {

  private DefaultServletHandlerConfigurer configurer;

  private DispatchingMockServletContext servletContext;

  private MockHttpServletResponse response;

  @BeforeEach
  public void setup() {
    response = new MockHttpServletResponse();
    servletContext = new DispatchingMockServletContext();
    configurer = new DefaultServletHandlerConfigurer(servletContext);
  }

  @Test
  public void notEnabled() {
    assertThat(configurer.buildHandlerRegistry()).isNull();
  }

  @Test
  public void enable() throws Throwable {
    configurer.enable();
    SimpleUrlHandlerRegistry handlerMapping = configurer.buildHandlerRegistry();
    DefaultServletHttpRequestHandler handler = (DefaultServletHttpRequestHandler) handlerMapping.getUrlMap().get("/**");

    assertThat(handler).isNotNull();
    assertThat(handlerMapping.getOrder()).isEqualTo(Integer.MAX_VALUE);

    handler.handleRequest(new ServletRequestContext(null, new MockHttpServletRequest(), response));

    String expected = "default";
    assertThat(servletContext.url).as("The ServletContext was not called with the default servlet name").isEqualTo(expected);
    assertThat(response.getForwardedUrl()).as("The request was not forwarded").isEqualTo(expected);
  }

  @Test
  public void enableWithServletName() throws Throwable {
    configurer.enable("defaultServlet");
    SimpleUrlHandlerRegistry handlerMapping = configurer.buildHandlerRegistry();
    DefaultServletHttpRequestHandler handler = (DefaultServletHttpRequestHandler) handlerMapping.getUrlMap().get("/**");

    assertThat(handler).isNotNull();
    assertThat(handlerMapping.getOrder()).isEqualTo(Integer.MAX_VALUE);

    handler.handleRequest(new ServletRequestContext(null, new MockHttpServletRequest(), response));

    String expected = "defaultServlet";
    assertThat(servletContext.url).as("The ServletContext was not called with the default servlet name").isEqualTo(expected);
    assertThat(response.getForwardedUrl()).as("The request was not forwarded").isEqualTo(expected);
  }

  private static class DispatchingMockServletContext extends MockServletContext {

    private String url;

    @Override
    public RequestDispatcher getNamedDispatcher(String url) {
      this.url = url;
      return new MockRequestDispatcher(url);
    }
  }

}
