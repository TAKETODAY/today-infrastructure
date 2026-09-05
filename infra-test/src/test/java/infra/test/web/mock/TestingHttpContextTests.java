/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.test.web.mock;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import infra.context.ApplicationContext;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpRange;
import infra.http.MediaType;
import infra.session.Session;
import infra.web.DispatcherHandler;
import infra.web.mock.MockMemoryPart;
import infra.web.mock.MockSession;
import infra.web.mock.api.DispatcherType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link TestingHttpContext}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public class TestingHttpContextTests {

  @Test
  public void builderBuildsRequest() {
    TestingHttpContext context = TestingHttpContext.post("/api/users?page=1")
            .header("X-Token", "abc")
            .queryParam("name", "today")
            .content("{\"a\":1}")
            .contentType("application/json")
            .build();

    assertThat(context.getMethodAsString()).isEqualTo("POST");
    assertThat(context.getRequestURI()).isEqualTo("/api/users");
    assertThat(context.getQueryString()).isEqualTo("page=1&name=today");
    assertThat(context.getHeader("X-Token")).isEqualTo("abc");
    assertThat(context.getParameter("name")).isEqualTo("today");
    assertThat(context.getContentAsString()).isEqualTo("{\"a\":1}");
    assertThat(context.getContentTypeAsString()).isEqualTo("application/json");
    assertThat(context.getContentLength()).isEqualTo(7);
  }

  @Test
  public void builderWithoutQueryString() {
    TestingHttpContext context = TestingHttpContext.get("/hello").build();
    assertThat(context.getRequestURI()).isEqualTo("/hello");
    assertThat(context.getQueryString()).isNull();
  }

  @Test
  public void defaults() {
    TestingHttpContext context = new TestingHttpContext();
    assertThat(context.getMethodAsString()).isEqualTo("GET");
    assertThat(context.getScheme()).isEqualTo("http");
    assertThat(context.getServerName()).isEqualTo("localhost");
    assertThat(context.getServerPort()).isEqualTo(80);
    assertThat(context.getStatus()).isEqualTo(200);
    assertThat(context.isSecure()).isFalse();
    assertThat(context.getLocale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void secureFromScheme() {
    TestingHttpContext context = TestingHttpContext.get("/").scheme("https").build();
    assertThat(context.isSecure()).isTrue();
  }

  @Test
  public void requestUrlIncludesSchemeServerAndUri() {
    TestingHttpContext context = TestingHttpContext.get("/path")
            .scheme("https").serverName("example.com").serverPort(8443)
            .build();
    assertThat(context.getRequestURL()).isEqualTo("https://example.com:8443/path");
  }

  @Test
  public void parameters() {
    TestingHttpContext context = new TestingHttpContext();
    context.addParameter("a", "1");
    context.addParameter("a", "2");
    context.addParameter("b", "x");

    assertThat(context.getParameter("a")).isEqualTo("1");
    assertThat(context.getParameterValues("a")).containsExactly("1", "2");
    assertThat(context.getRequestParameterNames()).containsExactlyInAnyOrder("a", "b");
    assertThat(context.getParameterMap()).containsKeys("a", "b");

    context.setParameter("a", "9");
    assertThat(context.getParameterValues("a")).containsExactly("9");

    context.removeParameter("a");
    assertThat(context.getParameter("a")).isNull();
  }

  @Test
  public void headers() {
    TestingHttpContext context = new TestingHttpContext();
    context.addRequestHeader("X-Foo", "bar");
    context.addRequestHeader("X-Foo", "baz");

    assertThat(context.getHeader("X-Foo")).isEqualTo("bar");
    assertThat(context.getHeaders("X-Foo")).containsExactly("bar", "baz");
    assertThat(context.getRequestHeaderNames()).contains("X-Foo");

    context.removeRequestHeader("X-Foo");
    assertThat(context.getHeader("X-Foo")).isNull();
  }

  @Test
  public void contentTypeWithCharset() {
    TestingHttpContext context = new TestingHttpContext();
    context.setRequestContentType("text/plain;charset=UTF-8");
    assertThat(context.getContentTypeAsString()).isEqualTo("text/plain;charset=UTF-8");
    assertThat(context.getCharacterEncoding()).isEqualTo("UTF-8");
    assertThat(context.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/plain;charset=UTF-8");
  }

  @Test
  public void characterEncodingAppendedToContentTypeHeader() {
    TestingHttpContext context = new TestingHttpContext();
    context.setRequestContentType("text/plain");
    context.setCharacterEncoding("UTF-8");
    assertThat(context.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/plain;charset=UTF-8");
  }

  @Test
  public void clearingContentTypeRemovesHeader() {
    TestingHttpContext context = new TestingHttpContext();
    context.setRequestContentType("text/plain");
    assertThat(context.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/plain");

    context.setRequestContentType(null);
    assertThat(context.getContentTypeAsString()).isNull();
    assertThat(context.getHeader(HttpHeaders.CONTENT_TYPE)).isNull();
  }

  @Test
  public void requestBody() throws IOException {
    TestingHttpContext context = TestingHttpContext.post("/")
            .content("hello".getBytes(StandardCharsets.UTF_8))
            .build();

    assertThat(context.getContent()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    assertThat(context.getContentAsString()).isEqualTo("hello");
    assertThat(context.createReader()).isNotNull();
  }

  @Test
  public void cookies() {
    TestingHttpContext context = new TestingHttpContext();
    context.setCookies(new HttpCookie("sid", "123"));

    assertThat(context.getCookies()).hasSize(1);
    assertThat(context.getCookies()[0].getName()).isEqualTo("sid");
    assertThat(context.getHeader(HttpHeaders.COOKIE)).isEqualTo("sid=123");
  }

  @Test
  public void sessionCreatedOnDemand() {
    TestingHttpContext context = new TestingHttpContext();
    assertThat(context.getSession(false)).isNull();

    Session session = context.getSession();
    assertThat(session).isNotNull();
    assertThat(context.getSession(false)).isSameAs(session);
  }

  @Test
  public void sessionCanBeSetExplicitly() {
    TestingHttpContext context = new TestingHttpContext();
    MockSession session = new MockSession("abc");
    context.setSession(session);

    assertThat(context.getSession(false)).isSameAs(session);
  }

  @Test
  public void asyncFlags() {
    TestingHttpContext context = new TestingHttpContext();
    context.setAsyncStarted(true);
    context.setAsyncSupported(true);
    context.setDispatcherType(DispatcherType.ASYNC);

    assertThat(context.isAsyncStarted()).isTrue();
    assertThat(context.isAsyncSupported()).isTrue();
    assertThat(context.getDispatcherType()).isEqualTo(DispatcherType.ASYNC);
  }

  @Test
  public void responseWritingAndReading() throws IOException {
    TestingHttpContext context = new TestingHttpContext();
    context.setStatus(201);
    context.setContentType("text/plain");
    context.responseHeaders().add("X-Resp", "yes");

    try (OutputStream out = context.getOutputStream()) {
      out.write("body".getBytes(StandardCharsets.UTF_8));
    }
    context.flush();

    assertThat(context.getStatus()).isEqualTo(201);
    assertThat(context.getResponseContentAsString()).isEqualTo("body");
    assertThat(context.getResponseHeader("X-Resp")).isEqualTo("yes");
    assertThat(context.isCommitted()).isTrue();
  }

  @Test
  public void sendRedirectSetsStatusAndLocation() throws IOException {
    TestingHttpContext context = new TestingHttpContext();
    context.sendRedirect("/login");

    assertThat(context.getStatus()).isEqualTo(302);
    assertThat(context.getResponseHeader(HttpHeaders.LOCATION)).isEqualTo("/login");
  }

  @Test
  public void sendErrorCommits() throws IOException {
    TestingHttpContext context = new TestingHttpContext();
    context.sendError(404);

    assertThat(context.getStatus()).isEqualTo(404);
    assertThat(context.isCommitted()).isTrue();
  }

  @Test
  public void resetClearsResponse() throws IOException {
    TestingHttpContext context = new TestingHttpContext();
    context.setStatus(500);
    context.setContentType("text/plain");
    context.getOutputStream().write("x".getBytes(StandardCharsets.UTF_8));

    context.reset();

    assertThat(context.getStatus()).isEqualTo(200);
    assertThat(context.getResponseContentAsString()).isEmpty();
    assertThat(context.isCommitted()).isFalse();
  }

  @Test
  public void builderConfiguresResponse() {
    TestingHttpContext context = TestingHttpContext.get("/")
            .status(201)
            .responseHeader("X-Resp", "yes")
            .build();

    assertThat(context.getStatus()).isEqualTo(201);
    assertThat(context.getResponseHeader("X-Resp")).isEqualTo("yes");
  }

  @Test
  public void builderSetsCookies() {
    TestingHttpContext context = TestingHttpContext.get("/").cookie("sid", "123").build();

    assertThat(context.getCookies()).hasSize(1);
    assertThat(context.getCookies()[0].getName()).isEqualTo("sid");
    assertThat(context.getHeader(HttpHeaders.COOKIE)).isEqualTo("sid=123");
  }

  @Test
  public void builderExpandsUriTemplate() {
    TestingHttpContext context = TestingHttpContext.get("/users/{id}", 42).build();
    assertThat(context.getRequestURI()).isEqualTo("/users/42");
  }

  @Test
  public void builderSetsApplicationContext() {
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    TestingHttpContext context = TestingHttpContext.get("/").applicationContext(applicationContext).build();
    assertThat(context.getApplicationContext()).isSameAs(applicationContext);
  }

  @Test
  public void builderSetsDispatcherHandler() {
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    TestingHttpContext context = TestingHttpContext.get("/").dispatcherHandler(dispatcherHandler).build();
    assertThat(context.getDispatcherHandler()).isSameAs(dispatcherHandler);
  }

  @Test
  public void bodyBuilderSetsAcceptHeaders() {
    TestingHttpContext context = TestingHttpContext.post("/")
            .accept(MediaType.APPLICATION_JSON)
            .acceptCharset(StandardCharsets.UTF_8)
            .acceptLanguage(Locale.ENGLISH)
            .build();

    assertThat(context.getHeader(HttpHeaders.ACCEPT)).contains("application/json");
    assertThat(context.getHeader(HttpHeaders.ACCEPT_CHARSET)).containsIgnoringCase("utf-8");
    assertThat(context.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).contains("en");
  }

  @Test
  public void bodyBuilderSetsConditionalHeaders() {
    TestingHttpContext context = TestingHttpContext.post("/")
            .ifNoneMatch("\"etag\"")
            .range(HttpRange.createByteRange(0, 9))
            .build();

    assertThat(context.getHeader(HttpHeaders.IF_NONE_MATCH)).contains("\"etag\"");
    assertThat(context.getHeader(HttpHeaders.RANGE)).contains("bytes=0-9");
  }

  @Test
  public void bodyBuilderSetsContentTypeAndCharset() {
    TestingHttpContext context = TestingHttpContext.post("/")
            .content("héllo", StandardCharsets.ISO_8859_1)
            .contentType("text/plain;charset=ISO-8859-1")
            .build();

    assertThat(context.getContentAsString()).isEqualTo("héllo");
    assertThat(context.getContentTypeAsString()).isEqualTo("text/plain;charset=ISO-8859-1");
  }

  @Test
  public void withAppliesPostProcessor() {
    TestingHttpContext context = TestingHttpContext.get("/")
            .with(ctx -> {
              ctx.requestHeaders().add("Authorization", "Bearer token");
              return ctx;
            })
            .build();

    assertThat(context.getHeader("Authorization")).isEqualTo("Bearer token");
  }
  @Test
  public void forwardedUrl() {
    TestingHttpContext context = new TestingHttpContext();
    context.setForwardedUrl("/WEB-INF/view.jsp");
    assertThat(context.getForwardedUrl()).isEqualTo("/WEB-INF/view.jsp");
  }

  @Test
  public void forwardWithoutDispatcherSetsForwardedUrl() throws Exception {
    TestingHttpContext context = new TestingHttpContext();
    context.forward("/WEB-INF/view.jsp");
    assertThat(context.getForwardedUrl()).isEqualTo("/WEB-INF/view.jsp");
  }

  @Test
  public void multipartParts() {
    TestingHttpContext context = new TestingHttpContext();
    context.addPart(new MockMemoryPart("file", "content".getBytes(StandardCharsets.UTF_8)));

    assertThat(context.getPart("file")).isNotNull();
    assertThat(context.getParts()).hasSize(1);
  }

}
