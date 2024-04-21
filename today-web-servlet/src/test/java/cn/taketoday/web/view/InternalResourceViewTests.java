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

package cn.taketoday.web.view;

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.view.InternalResourceView;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockRequestDispatcher;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link InternalResourceView}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class InternalResourceViewTests {

  private static final Map<String, Object> model = Map.of("foo", "bar", "I", 1L);

  private static final String url = "forward-to";

  private final HttpServletRequest request = mock(HttpServletRequest.class);

  private final MockHttpServletResponse response = new MockHttpServletResponse();

  private final InternalResourceView view = new InternalResourceView();

  /**
   * If the url property isn't supplied, view initialization should fail.
   */
  @Test
  public void rejectsNullUrl() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(
            view::afterPropertiesSet);
  }

  @Test
  public void forward() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myservlet/handler.do");
    request.setContextPath("/mycontext");
    request.setServletPath("/myservlet");
    request.setPathInfo(";mypathinfo");
    request.setQueryString("?param1=value1");
    RequestContext context = new ServletRequestContext(null, request, response);

    view.setUrl(url);
    view.setServletContext(new MockServletContext() {
      @Override
      public int getMinorVersion() {
        return 4;
      }
    });

    view.render(model, context);
    assertThat(response.getForwardedUrl()).isEqualTo(url);

    model.forEach((key, value) -> assertThat(request.getAttribute(key)).as("Values for model key '" + key
            + "' must match").isEqualTo(value));
  }

  @Test
  public void alwaysInclude() throws Exception {
    given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

    view.setUrl(url);
    view.setAlwaysInclude(true);
    RequestContext context = new ServletRequestContext(null, request, response);

    // Can now try multiple tests
    view.render(model, context);
    assertThat(response.getIncludedUrl()).isEqualTo(url);

    model.forEach((key, value) -> verify(request).setAttribute(key, value));
  }

  @Test
  public void includeOnAttribute() throws Exception {
    given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));
    given(request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI)).willReturn("somepath");
    RequestContext context = new ServletRequestContext(null, request, response);

    view.setUrl(url);

    // Can now try multiple tests
    view.render(model, context);
    assertThat(response.getIncludedUrl()).isEqualTo(url);

    model.forEach((key, value) -> verify(request).setAttribute(key, value));
  }

  @Test
  public void includeOnCommitted() throws Exception {
    given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

    response.setCommitted(true);
    view.setUrl(url);
    RequestContext context = new ServletRequestContext(null, request, response);

    // Can now try multiple tests
    view.render(model, context);
    assertThat(response.getIncludedUrl()).isEqualTo(url);

    model.forEach((k, v) -> verify(request).setAttribute(k, v));
  }

}
