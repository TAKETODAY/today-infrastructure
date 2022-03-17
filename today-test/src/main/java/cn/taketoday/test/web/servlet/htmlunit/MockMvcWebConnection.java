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

package cn.taketoday.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;

import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockHttpSession;
import cn.taketoday.test.web.servlet.htmlunit.webdriver.WebConnectionHtmlUnitDriver;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.RequestBuilder;
import cn.taketoday.test.web.servlet.ResultActions;

/**
 * {@code MockMvcWebConnection} enables {@link MockMvc} to transform a
 * {@link WebRequest} into a {@link WebResponse}.
 * <p>This is the core integration with <a href="http://htmlunit.sourceforge.net/">HtmlUnit</a>.
 * <p>Example usage can be seen below.
 *
 * <pre class="code">
 * WebClient webClient = new WebClient();
 * MockMvc mockMvc = ...
 * MockMvcWebConnection webConnection = new MockMvcWebConnection(mockMvc, webClient);
 * webClient.setWebConnection(webConnection);
 *
 * // Use webClient as normal ...
 * </pre>
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @see WebConnectionHtmlUnitDriver
 * @since 4.0
 */
public final class MockMvcWebConnection implements WebConnection {

  private final Map<String, MockHttpSession> sessions = new HashMap<>();

  private final MockMvc mockMvc;

  @Nullable
  private final String contextPath;

  private WebClient webClient;

  /**
   * Create a new instance that assumes the context path of the application
   * is {@code ""} (i.e., the root context).
   * <p>For example, the URL {@code http://localhost/test/this} would use
   * {@code ""} as the context path.
   *
   * @param mockMvc the {@code MockMvc} instance to use; never {@code null}
   * @param webClient the {@link WebClient} to use. never {@code null}
   */
  public MockMvcWebConnection(MockMvc mockMvc, WebClient webClient) {
    this(mockMvc, webClient, "");
  }

  /**
   * Create a new instance with the specified context path.
   * <p>The path may be {@code null} in which case the first path segment
   * of the URL is turned into the contextPath. Otherwise it must conform
   * to {@link jakarta.servlet.http.HttpServletRequest#getContextPath()}
   * which states that it can be an empty string and otherwise must start
   * with a "/" character and not end with a "/" character.
   *
   * @param mockMvc the {@code MockMvc} instance to use (never {@code null})
   * @param webClient the {@link WebClient} to use (never {@code null})
   * @param contextPath the contextPath to use
   */
  public MockMvcWebConnection(MockMvc mockMvc, WebClient webClient, @Nullable String contextPath) {
    Assert.notNull(mockMvc, "MockMvc must not be null");
    Assert.notNull(webClient, "WebClient must not be null");
    validateContextPath(contextPath);

    this.webClient = webClient;
    this.mockMvc = mockMvc;
    this.contextPath = contextPath;
  }

  /**
   * Validate the supplied {@code contextPath}.
   * <p>If the value is not {@code null}, it must conform to
   * {@link jakarta.servlet.http.HttpServletRequest#getContextPath()} which
   * states that it can be an empty string and otherwise must start with
   * a "/" character and not end with a "/" character.
   *
   * @param contextPath the path to validate
   */
  static void validateContextPath(@Nullable String contextPath) {
    if (contextPath == null || contextPath.isEmpty()) {
      return;
    }
    Assert.isTrue(contextPath.startsWith("/"), () -> "contextPath '" + contextPath + "' must start with '/'.");
    Assert.isTrue(!contextPath.endsWith("/"), () -> "contextPath '" + contextPath + "' must not end with '/'.");
  }

  public void setWebClient(WebClient webClient) {
    Assert.notNull(webClient, "WebClient must not be null");
    this.webClient = webClient;
  }

  @Override
  public WebResponse getResponse(WebRequest webRequest) throws IOException {
    long startTime = System.currentTimeMillis();
    HtmlUnitRequestBuilder requestBuilder = new HtmlUnitRequestBuilder(this.sessions, this.webClient, webRequest);
    requestBuilder.setContextPath(this.contextPath);

    MockHttpServletResponse httpServletResponse = getResponse(requestBuilder);
    String forwardedUrl = httpServletResponse.getForwardedUrl();
    while (forwardedUrl != null) {
      requestBuilder.setForwardPostProcessor(new ForwardRequestPostProcessor(forwardedUrl));
      httpServletResponse = getResponse(requestBuilder);
      forwardedUrl = httpServletResponse.getForwardedUrl();
    }
    storeCookies(webRequest, httpServletResponse.getCookies());

    return new MockWebResponseBuilder(startTime, webRequest, httpServletResponse).build();
  }

  private MockHttpServletResponse getResponse(RequestBuilder requestBuilder) throws IOException {
    ResultActions resultActions;
    try {
      resultActions = this.mockMvc.perform(requestBuilder);
    }
    catch (Exception ex) {
      throw new IOException(ex);
    }

    return resultActions.andReturn().getResponse();
  }

  private void storeCookies(WebRequest webRequest, jakarta.servlet.http.Cookie[] cookies) {
    Date now = new Date();
    CookieManager cookieManager = this.webClient.getCookieManager();
    for (jakarta.servlet.http.Cookie cookie : cookies) {
      if (cookie.getDomain() == null) {
        cookie.setDomain(webRequest.getUrl().getHost());
      }
      Cookie toManage = createCookie(cookie);
      Date expires = toManage.getExpires();
      if (expires == null || expires.after(now)) {
        cookieManager.addCookie(toManage);
      }
      else {
        cookieManager.removeCookie(toManage);
      }
    }
  }

  private static com.gargoylesoftware.htmlunit.util.Cookie createCookie(jakarta.servlet.http.Cookie cookie) {
    Date expires = null;
    if (cookie.getMaxAge() > -1) {
      expires = new Date(System.currentTimeMillis() + cookie.getMaxAge() * 1000);
    }
    BasicClientCookie result = new BasicClientCookie(cookie.getName(), cookie.getValue());
    result.setDomain(cookie.getDomain());
    result.setComment(cookie.getComment());
    result.setExpiryDate(expires);
    result.setPath(cookie.getPath());
    result.setSecure(cookie.getSecure());
    if (cookie.isHttpOnly()) {
      result.setAttribute("httponly", "true");
    }
    return new com.gargoylesoftware.htmlunit.util.Cookie(result);
  }

  @Override
  public void close() {
  }

}
